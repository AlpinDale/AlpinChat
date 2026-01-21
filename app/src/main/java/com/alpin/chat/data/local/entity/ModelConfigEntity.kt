package com.alpin.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey
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
