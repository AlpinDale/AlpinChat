package com.alpin.chat.data.repository

import com.alpin.chat.data.local.db.MessageDao
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.toDomain
import com.alpin.chat.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getMessagesForConversationSync(conversationId: String): List<Message> =
        messageDao.getMessagesForConversationSync(conversationId).map { it.toDomain() }

    suspend fun getMessageById(id: String): Message? =
        messageDao.getMessageById(id)?.toDomain()

    suspend fun getLastMessage(conversationId: String): Message? =
        messageDao.getLastMessage(conversationId)?.toDomain()

    suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    suspend fun insertMessages(messages: List<Message>) {
        messageDao.insertMessages(messages.map { it.toEntity() })
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message.toEntity())
    }

    suspend fun updateMessageContent(id: String, content: String) {
        messageDao.updateMessageContent(id, content)
    }

    suspend fun deleteMessage(id: String) {
        messageDao.deleteMessageById(id)
    }

    suspend fun deleteMessagesForConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
    }

    suspend fun getMessageCount(conversationId: String): Int =
        messageDao.getMessageCount(conversationId)
}
