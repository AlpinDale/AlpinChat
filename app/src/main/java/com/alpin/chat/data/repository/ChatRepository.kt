package com.alpin.chat.data.repository

import com.alpin.chat.data.local.datastore.AppSettings
import com.alpin.chat.data.remote.api.LLMApiService
import com.alpin.chat.data.remote.dto.ModelsResponse
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val llmApiService: LLMApiService
) {
    suspend fun testConnection(modelConfig: ModelConfig): Result<ModelsResponse> =
        llmApiService.testConnection(modelConfig)

    fun streamChatCompletion(
        modelConfig: ModelConfig,
        messages: List<Message>,
        settings: AppSettings,
        thinkingEnabled: Boolean = false
    ): Flow<String> = llmApiService.streamChatCompletion(
        modelConfig = modelConfig,
        messages = messages,
        settings = settings,
        thinkingEnabled = thinkingEnabled
    )

    suspend fun sendChatCompletion(
        modelConfig: ModelConfig,
        messages: List<Message>,
        settings: AppSettings
    ): Result<String> = llmApiService.sendChatCompletion(
        modelConfig = modelConfig,
        messages = messages,
        settings = settings
    )
}
