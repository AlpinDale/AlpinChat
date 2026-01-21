package com.alpin.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    // All sampling params are optional - null means omit from request
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("min_p")
    val minP: Float? = null,
    @SerialName("presence_penalty")
    val presencePenalty: Float? = null,
    @SerialName("frequency_penalty")
    val frequencyPenalty: Float? = null,
    @SerialName("repetition_penalty")
    val repetitionPenalty: Float? = null
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: ChatContentDto
)

@Serializable
sealed class ChatContentDto {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : ChatContentDto()

    @Serializable
    @SerialName("array")
    data class MultiModal(val parts: List<ContentPartDto>) : ChatContentDto()
}

@Serializable
sealed class ContentPartDto {
    @Serializable
    @SerialName("text")
    data class TextPart(
        val type: String = "text",
        val text: String
    ) : ContentPartDto()

    @Serializable
    @SerialName("image_url")
    data class ImagePart(
        val type: String = "image_url",
        @SerialName("image_url")
        val imageUrl: ImageUrlDto
    ) : ContentPartDto()
}

@Serializable
data class ImageUrlDto(
    val url: String,
    val detail: String = "auto"
)
