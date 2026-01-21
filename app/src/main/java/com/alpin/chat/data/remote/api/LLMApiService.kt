package com.alpin.chat.data.remote.api

import com.alpin.chat.data.local.datastore.AppSettings
import com.alpin.chat.data.remote.dto.ModelsResponse
import com.alpin.chat.data.remote.dto.SSEChunk
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.MessageRole
import com.alpin.chat.domain.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    suspend fun testConnection(modelConfig: ModelConfig): Result<ModelsResponse> = withContext(Dispatchers.IO) {
        try {
            // Try /models endpoint first (OpenAI format)
            val request = Request.Builder()
                .url("${modelConfig.baseUrl.trimEnd('/')}/models")
                .get()
                .apply {
                    modelConfig.apiKey?.let {
                        addHeader("Authorization", "Bearer $it")
                    }
                }
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty response")
                try {
                    Result.success(json.decodeFromString<ModelsResponse>(body))
                } catch (e: Exception) {
                    // If parsing fails, still consider it a success since server responded
                    Result.success(ModelsResponse())
                }
            } else {
                // Some servers don't have /models, try a simple health check approach
                // by checking if we get any response (even an error means server is reachable)
                if (response.code == 404) {
                    // Server is reachable but doesn't have /models endpoint - that's OK
                    Result.success(ModelsResponse())
                } else {
                    Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAvailableModels(baseUrl: String, apiKey: String?): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/models")
                .get()
                .apply {
                    apiKey?.let {
                        addHeader("Authorization", "Bearer $it")
                    }
                }
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty response")
                val modelsResponse = json.decodeFromString<ModelsResponse>(body)

                // Extract model IDs from OpenAI format or Ollama format
                val modelIds = mutableListOf<String>()
                modelsResponse.data?.forEach { model ->
                    modelIds.add(model.id)
                }
                modelsResponse.models?.forEach { model ->
                    model.name?.let { modelIds.add(it) }
                    model.model?.let { if (it !in modelIds) modelIds.add(it) }
                }

                Result.success(modelIds)
            } else {
                Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChatCompletion(
        modelConfig: ModelConfig,
        messages: List<Message>,
        settings: AppSettings,
        thinkingEnabled: Boolean = false
    ): Flow<String> = callbackFlow {
        val allMessages = buildList {
            settings.systemPrompt.takeIf { it.isNotBlank() }?.let {
                add(buildMessageJson(MessageRole.SYSTEM.value, it, null))
            }
            messages.forEach { message ->
                add(buildMessageJson(message.role.value, message.content, message.imageData))
            }
            // If model supports thinking but thinking is disabled, prefill to skip thinking
            if (modelConfig.supportsThinking && !thinkingEnabled) {
                add(buildMessageJson(MessageRole.ASSISTANT.value, "<think>\n</think>\n", null))
            }
        }

        val requestBody = buildRequestBody(modelConfig.modelName, allMessages, settings, stream = true)

        val request = Request.Builder()
            .url("${modelConfig.baseUrl.trimEnd('/')}/chat/completions")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .apply {
                modelConfig.apiKey?.let {
                    addHeader("Authorization", "Bearer $it")
                }
            }
            .build()

        val call = okHttpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        close(IOException("HTTP ${response.code}: $errorBody"))
                        return
                    }

                    response.body?.byteStream()?.let { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.startsWith("data: ")) {
                                val data = currentLine.removePrefix("data: ").trim()
                                if (data == "[DONE]") {
                                    break
                                }
                                if (data.isNotEmpty()) {
                                    try {
                                        val chunk = json.decodeFromString<SSEChunk>(data)
                                        chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                                            trySend(content)
                                        }
                                    } catch (e: Exception) {
                                        // Skip malformed chunks
                                    }
                                }
                            }
                        }
                        reader.close()
                    }
                    close()
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }
        })

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendChatCompletion(
        modelConfig: ModelConfig,
        messages: List<Message>,
        settings: AppSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allMessages = buildList {
                settings.systemPrompt.takeIf { it.isNotBlank() }?.let {
                    add(buildMessageJson(MessageRole.SYSTEM.value, it, null))
                }
                messages.forEach { message ->
                    add(buildMessageJson(message.role.value, message.content, message.imageData))
                }
            }

            val requestBody = buildRequestBody(modelConfig.modelName, allMessages, settings, stream = false)

            val request = Request.Builder()
                .url("${modelConfig.baseUrl.trimEnd('/')}/chat/completions")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .apply {
                    modelConfig.apiKey?.let {
                        addHeader("Authorization", "Bearer $it")
                    }
                }
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: throw IOException("Empty response")
                val parsed = json.decodeFromString<com.alpin.chat.data.remote.dto.ChatCompletionResponse>(body)
                val content = parsed.choices.firstOrNull()?.message?.content ?: ""
                Result.success(content)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(IOException("HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequestBody(
        model: String,
        messages: List<String>,
        settings: AppSettings,
        stream: Boolean
    ): String {
        return buildString {
            append("{")
            append("\"model\":\"$model\",")
            append("\"messages\":$messages,")
            append("\"stream\":$stream")

            // Only add sampling params if they are enabled (not null)
            settings.temperature?.let { append(",\"temperature\":$it") }
            settings.topP?.let { append(",\"top_p\":$it") }
            settings.topK?.let { append(",\"top_k\":$it") }
            settings.minP?.let { append(",\"min_p\":$it") }
            settings.presencePenalty?.let { append(",\"presence_penalty\":$it") }
            settings.frequencyPenalty?.let { append(",\"frequency_penalty\":$it") }
            settings.repetitionPenalty?.let { append(",\"repetition_penalty\":$it") }

            append("}")
        }
    }

    private fun buildMessageJson(role: String, content: String, imageData: String?): String {
        return if (imageData != null) {
            """{"role":"$role","content":[{"type":"text","text":${json.encodeToString(kotlinx.serialization.serializer<String>(), content)}},{"type":"image_url","image_url":{"url":"$imageData","detail":"auto"}}]}"""
        } else {
            """{"role":"$role","content":${json.encodeToString(kotlinx.serialization.serializer<String>(), content)}}"""
        }
    }
}
