package com.alpin.chat.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alpin.chat.R
import com.alpin.chat.ui.components.ChatBubble
import com.alpin.chat.ui.components.ChatInput
import com.alpin.chat.ui.components.ModelSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
    conversationId: String? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Only load/start conversation when conversationId actually changes, not on every recomposition
    LaunchedEffect(conversationId) {
        if (conversationId != null) {
            // Only load if different from current
            if (uiState.currentConversationId != conversationId) {
                viewModel.loadConversation(conversationId)
            }
        } else if (uiState.currentConversationId == null && uiState.messages.isEmpty()) {
            // Only start new if we don't already have a conversation going
            viewModel.startNewConversation()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ModelSelector(
                        selectedModel = uiState.selectedModel,
                        models = uiState.models,
                        onModelSelected = viewModel::selectModel
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                    EmptyStateMessage(
                        hasModels = uiState.models.isNotEmpty(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            ChatBubble(
                                message = message,
                                tokenCount = uiState.messageTokenCounts[message.id],
                                onRegenerate = { viewModel.regenerateMessage(message.id) },
                                onPreviousAlternative = { viewModel.previousAlternative(message.id) },
                                onNextAlternative = { viewModel.nextAlternative(message.id) },
                                onDelete = { viewModel.deleteMessage(message.id) },
                                onEdit = { newContent -> viewModel.editMessage(message.id, newContent) }
                            )
                        }
                    }
                }
            }

            ChatInput(
                value = uiState.inputText,
                onValueChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
                isGenerating = uiState.isGenerating,
                supportsVision = uiState.selectedModel?.supportsVision == true,
                selectedImageUri = uiState.selectedImageUri,
                onImageSelected = viewModel::selectImage,
                thinkingEnabled = uiState.thinkingEnabled,
                onThinkingToggle = viewModel::toggleThinking,
                showOptions = uiState.showInputOptions,
                onToggleOptions = viewModel::toggleInputOptions,
                supportsThinking = uiState.selectedModel?.supportsThinking == true
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(
    hasModels: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasModels) {
                "Start a conversation"
            } else {
                "No models configured"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasModels) {
                "Type a message below to begin chatting with your local LLM"
            } else {
                "Go to Settings to add your first model"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
