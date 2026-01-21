package com.alpin.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class MessageAlternativeEntity(
    val content: String,
    val thinkingContent: String? = null
)

class MessageAlternativesConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAlternativesList(alternatives: List<MessageAlternativeEntity>?): String? {
        return alternatives?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toAlternativesList(data: String?): List<MessageAlternativeEntity>? {
        return data?.let { json.decodeFromString<List<MessageAlternativeEntity>>(it) }
    }
}

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
@TypeConverters(MessageAlternativesConverter::class)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String,
    val content: String,
    val imageData: String? = null,
    val thinkingContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val alternatives: List<MessageAlternativeEntity>? = null,
    val currentAlternativeIndex: Int = 0
)
