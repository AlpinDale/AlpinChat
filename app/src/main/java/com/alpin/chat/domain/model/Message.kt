package com.alpin.chat.domain.model

import com.alpin.chat.data.local.entity.MessageAlternativeEntity
import com.alpin.chat.data.local.entity.MessageEntity
import java.util.UUID

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}

data class MessageAlternative(
    val content: String,
    val thinkingContent: String? = null
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val imageData: String? = null,
    val thinkingContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isThinking: Boolean = false,
    val alternatives: List<MessageAlternative> = emptyList(),
    val currentAlternativeIndex: Int = 0
) {
    val totalAlternatives: Int get() = alternatives.size + 1 // +1 for the original

    val displayContent: String get() = when {
        isStreaming -> content  // During streaming, always show the streaming content directly
        currentAlternativeIndex == 0 -> content
        else -> alternatives.getOrNull(currentAlternativeIndex - 1)?.content ?: content
    }

    val displayThinkingContent: String? get() = when {
        isStreaming -> thinkingContent  // During streaming, show streaming thinking content
        currentAlternativeIndex == 0 -> thinkingContent
        else -> alternatives.getOrNull(currentAlternativeIndex - 1)?.thinkingContent
    }
}

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    role = MessageRole.entries.find { it.value == role } ?: MessageRole.USER,
    content = content,
    imageData = imageData,
    thinkingContent = thinkingContent,
    timestamp = timestamp,
    alternatives = alternatives?.map { it.toDomain() } ?: emptyList(),
    currentAlternativeIndex = currentAlternativeIndex
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.value,
    content = content,
    imageData = imageData,
    thinkingContent = thinkingContent,
    timestamp = timestamp,
    alternatives = alternatives.map { it.toEntity() }.takeIf { it.isNotEmpty() },
    currentAlternativeIndex = currentAlternativeIndex
)

fun MessageAlternativeEntity.toDomain(): MessageAlternative = MessageAlternative(
    content = content,
    thinkingContent = thinkingContent
)

fun MessageAlternative.toEntity(): MessageAlternativeEntity = MessageAlternativeEntity(
    content = content,
    thinkingContent = thinkingContent
)
