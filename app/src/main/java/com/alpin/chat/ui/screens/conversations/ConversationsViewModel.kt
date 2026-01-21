package com.alpin.chat.ui.screens.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpin.chat.data.local.datastore.SettingsDataStore
import com.alpin.chat.data.repository.ConversationRepository
import com.alpin.chat.domain.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val confirmDeleteConversation: Boolean = true
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(confirmDeleteConversation = settings.confirmDeleteConversation) }
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            conversationRepository.getAllConversations().collect { conversations ->
                _uiState.update {
                    it.copy(
                        conversations = conversations,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun searchConversations(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isEmpty()) {
                conversationRepository.getAllConversations().collect { conversations ->
                    _uiState.update { it.copy(conversations = conversations) }
                }
            } else {
                conversationRepository.searchConversations(query).collect { conversations ->
                    _uiState.update { it.copy(conversations = conversations) }
                }
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
        }
    }

    fun disableDeleteConfirmation() {
        viewModelScope.launch {
            settingsDataStore.updateConfirmDeleteConversation(false)
        }
    }
}
