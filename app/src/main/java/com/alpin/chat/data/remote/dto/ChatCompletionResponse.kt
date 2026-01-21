package com.alpin.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val id: String,
    @SerialName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

@Serializable
data class ChoiceDto(
    val index: Int,
    val message: MessageDto? = null,
    val delta: DeltaDto? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String?
)

@Serializable
data class DeltaDto(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

@Serializable
data class SSEChunk(
    val id: String? = null,
    @SerialName("object")
    val objectType: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<ChoiceDto> = emptyList()
)

@Serializable
data class ModelsResponse(
    @SerialName("object")
    val objectType: String? = null,
    val data: List<ModelDto>? = null,
    // Ollama format
    val models: List<OllamaModelDto>? = null
)

@Serializable
data class ModelDto(
    val id: String,
    @SerialName("object")
    val objectType: String? = null,
    val created: Long? = null,
    @SerialName("owned_by")
    val ownedBy: String? = null
)

@Serializable
data class OllamaModelDto(
    val name: String? = null,
    val model: String? = null,
    @SerialName("modified_at")
    val modifiedAt: String? = null,
    val size: Long? = null
)
