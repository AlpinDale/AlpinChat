package com.alpin.chat.data.repository

import com.alpin.chat.data.local.db.ConversationDao
import com.alpin.chat.data.local.entity.ConversationEntity
import com.alpin.chat.domain.model.Conversation
import com.alpin.chat.domain.model.toDomain
import com.alpin.chat.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao
) {
    fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }

    fun searchConversations(query: String): Flow<List<Conversation>> =
        conversationDao.searchConversations(query).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getConversationById(id: String): Conversation? =
        conversationDao.getConversationById(id)?.toDomain()

    suspend fun createConversation(conversation: Conversation) {
        conversationDao.insertConversation(conversation.toEntity())
    }

    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation.toEntity())
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        conversationDao.updateConversationTitle(id, title)
    }

    suspend fun updateConversationTimestamp(id: String) {
        conversationDao.updateConversationTimestamp(id)
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
    }
}
