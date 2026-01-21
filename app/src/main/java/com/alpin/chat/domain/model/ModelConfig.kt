package com.alpin.chat.domain.model

import com.alpin.chat.data.local.entity.ModelConfigEntity
import java.util.UUID

data class ModelConfig(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val baseUrl: String,
    val modelName: String,
    val apiKey: String? = null,
    val isDefault: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsThinking: Boolean = false,
    val contextLength: Int = 32768
)

fun ModelConfigEntity.toDomain(): ModelConfig = ModelConfig(
    id = id,
    displayName = displayName,
    baseUrl = baseUrl,
    modelName = modelName,
    apiKey = apiKey,
    isDefault = isDefault,
    supportsVision = supportsVision,
    supportsThinking = supportsThinking,
    contextLength = contextLength
)

fun ModelConfig.toEntity(): ModelConfigEntity = ModelConfigEntity(
    id = id,
    displayName = displayName,
    baseUrl = baseUrl,
    modelName = modelName,
    apiKey = apiKey,
    isDefault = isDefault,
    supportsVision = supportsVision,
    supportsThinking = supportsThinking,
    contextLength = contextLength
)
