package com.alpin.chat.domain.model

import com.alpin.chat.data.local.entity.ConversationEntity

data class Conversation(
    val id: String,
    val title: String,
    val modelConfigId: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    modelConfigId = modelConfigId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    title = title,
    modelConfigId = modelConfigId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
