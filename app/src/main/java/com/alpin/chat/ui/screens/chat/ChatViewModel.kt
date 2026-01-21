package com.alpin.chat.ui.screens.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpin.chat.data.local.datastore.AppSettings
import com.alpin.chat.data.local.datastore.SettingsDataStore
import com.alpin.chat.data.repository.ConversationRepository
import com.alpin.chat.data.repository.MessageRepository
import com.alpin.chat.data.repository.ModelConfigRepository
import com.alpin.chat.domain.model.Conversation
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.MessageAlternative
import com.alpin.chat.domain.model.MessageRole
import com.alpin.chat.domain.model.ModelConfig
import com.alpin.chat.service.GenerationService
import com.alpin.chat.tokenizer.ContextManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isGenerating: Boolean = false,
    val error: String? = null,
    val currentConversationId: String? = null,
    val selectedModel: ModelConfig? = null,
    val models: List<ModelConfig> = emptyList(),
    val inputText: String = "",
    val selectedImageUri: Uri? = null,
    val settings: AppSettings = AppSettings(),
    val thinkingEnabled: Boolean = false,
    val showInputOptions: Boolean = false,
    val messageTokenCounts: Map<String, Int> = emptyMap()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val settingsDataStore: SettingsDataStore,
    private val contextManager: ContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentStreamingMessageId: String? = null

    // Service binding for background generation
    private var generationService: GenerationService? = null
    private var serviceBound = false
    private var pendingGeneration: (() -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GenerationService.GenerationBinder
            generationService = binder.getService()
            serviceBound = true
            setupServiceObservers()
            pendingGeneration?.invoke()
            pendingGeneration = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            generationService = null
            serviceBound = false
        }
    }

    init {
        loadModels()
        loadSettings()
        bindGenerationService()
        initializeTokenizer()
    }

    private fun initializeTokenizer() {
        viewModelScope.launch {
            contextManager.initialize()
        }
    }

    private fun computeTokenCount(message: Message): Int {
        return contextManager.countDisplayTokens(message)
    }

    private fun updateTokenCountsForMessages(messages: List<Message>) {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            messages
                .filter { !it.isStreaming }
                .forEach { message ->
                    counts[message.id] = computeTokenCount(message)
                }
            _uiState.update { it.copy(messageTokenCounts = counts) }
        }
    }

    private fun bindGenerationService() {
        val intent = Intent(context, GenerationService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupServiceObservers() {
        val service = generationService ?: return

        viewModelScope.launch {
            service.streamingContent.collect { chunk ->
                handleStreamChunk(chunk)
            }
        }

        viewModelScope.launch {
            service.generationError.collect { error ->
                handleGenerationError(error)
            }
        }

        viewModelScope.launch {
            service.generationComplete.collect {
                handleGenerationComplete()
            }
        }

        viewModelScope.launch {
            service.isGenerating.collect { isGenerating ->
                _uiState.update { it.copy(isGenerating = isGenerating) }
            }
        }
    }

    private var fullRawContent = ""
    private var currentGeneratingConversationId: String? = null
    private var currentGeneratingModel: ModelConfig? = null
    private var currentGeneratingThinkingEnabled: Boolean = false

    private fun handleStreamChunk(chunk: String) {
        fullRawContent += chunk
        val messageId = currentStreamingMessageId ?: return

        val (currentThinking, currentContent, stillThinking) = parseThinkingContentStreaming(
            fullRawContent,
            currentGeneratingThinkingEnabled
        )

        _uiState.update {
            it.copy(
                messages = it.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            content = currentContent,
                            thinkingContent = currentThinking.takeIf { it.isNotEmpty() },
                            isThinking = stillThinking
                        )
                    } else {
                        msg
                    }
                }
            )
        }
    }

    private fun handleGenerationError(error: String) {
        val messageId = currentStreamingMessageId ?: return
        _uiState.update {
            it.copy(
                error = error,
                isGenerating = false,
                messages = it.messages.filter { msg -> msg.id != messageId }
            )
        }
        currentStreamingMessageId = null
        fullRawContent = ""
    }

    private fun handleGenerationComplete() {
        val messageId = currentStreamingMessageId ?: return
        val conversationId = currentGeneratingConversationId ?: return

        if (fullRawContent.isNotEmpty()) {
            val finalThinking = parseThinkingContent(fullRawContent, currentGeneratingThinkingEnabled).first
            val finalContent = parseThinkingContent(fullRawContent, currentGeneratingThinkingEnabled).second

            // Check if this is a regeneration (message already has alternatives)
            val existingMessage = _uiState.value.messages.find { it.id == messageId }
            val existingAlternatives = existingMessage?.alternatives ?: emptyList()
            val isRegeneration = existingAlternatives.isNotEmpty()

            val finalMessage = if (isRegeneration) {
                // For regeneration: original content is in alternatives[0], rest are other alternatives
                // We need to restore original to content and add new generation as the last alternative
                val originalContent = existingAlternatives.firstOrNull()?.content ?: ""
                val originalThinking = existingAlternatives.firstOrNull()?.thinkingContent
                val otherAlternatives = existingAlternatives.drop(1)
                val newAlternative = MessageAlternative(finalContent, finalThinking.takeIf { it.isNotEmpty() })

                Message(
                    id = messageId,
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = originalContent,
                    thinkingContent = originalThinking,
                    isStreaming = false,
                    isThinking = false,
                    alternatives = otherAlternatives + newAlternative,
                    currentAlternativeIndex = otherAlternatives.size + 1 // Point to the newly generated one
                )
            } else {
                // Normal generation (no alternatives yet)
                Message(
                    id = messageId,
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = finalContent,
                    thinkingContent = finalThinking.takeIf { it.isNotEmpty() },
                    isStreaming = false,
                    isThinking = false,
                    alternatives = emptyList(),
                    currentAlternativeIndex = 0
                )
            }

            viewModelScope.launch {
                messageRepository.insertMessage(finalMessage)
                conversationRepository.updateConversationTimestamp(conversationId)
            }

            // Compute token count for the completed message
            val tokenCount = computeTokenCount(finalMessage)

            _uiState.update {
                it.copy(
                    messages = it.messages.map { msg ->
                        if (msg.id == messageId) finalMessage else msg
                    },
                    isGenerating = false,
                    messageTokenCounts = it.messageTokenCounts + (messageId to tokenCount)
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    messages = it.messages.filter { msg -> msg.id != messageId }
                )
            }
        }

        currentStreamingMessageId = null
        currentGeneratingConversationId = null
        currentGeneratingModel = null
        currentGeneratingThinkingEnabled = false
        fullRawContent = ""
    }

    override fun onCleared() {
        super.onCleared()
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            modelConfigRepository.getAllModelConfigs().collect { models ->
                val currentSelectedId = _uiState.value.selectedModel?.id
                // Find the updated version of the currently selected model, or fall back to default/first
                val selectedModel = currentSelectedId?.let { id -> models.find { it.id == id } }
                    ?: models.find { it.isDefault }
                    ?: models.firstOrNull()

                _uiState.update {
                    it.copy(
                        models = models,
                        selectedModel = selectedModel
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(
                    settings = settings,
                    thinkingEnabled = settings.thinkingEnabled
                ) }
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentConversationId = conversationId) }
            messageRepository.getMessagesForConversation(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
                updateTokenCountsForMessages(messages)
            }
        }
    }

    fun startNewConversation() {
        generationService?.stopGeneration()
        _uiState.update {
            it.copy(
                currentConversationId = null,
                messages = emptyList(),
                isGenerating = false,
                error = null,
                inputText = "",
                selectedImageUri = null
            )
        }
    }

    fun selectModel(model: ModelConfig) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun toggleThinking() {
        val newValue = !_uiState.value.thinkingEnabled
        _uiState.update { it.copy(thinkingEnabled = newValue) }
        viewModelScope.launch {
            settingsDataStore.updateThinkingEnabled(newValue)
        }
    }

    fun toggleInputOptions() {
        _uiState.update { it.copy(showInputOptions = !it.showInputOptions) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val model = state.selectedModel ?: return
        val text = state.inputText.trim()
        val imageUri = state.selectedImageUri

        if (text.isEmpty() && imageUri == null) return

        viewModelScope.launch {
            val conversationId = state.currentConversationId ?: createNewConversation(text, model.id)

            val imageData = imageUri?.let { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    bytes?.let {
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        "data:$mimeType;base64,${Base64.encodeToString(it, Base64.NO_WRAP)}"
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val userMessage = Message(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                imageData = imageData
            )

            messageRepository.insertMessage(userMessage)
            conversationRepository.updateConversationTimestamp(conversationId)

            // Compute token count for the user message
            val userTokenCount = computeTokenCount(userMessage)

            _uiState.update {
                it.copy(
                    currentConversationId = conversationId,
                    messages = it.messages + userMessage,
                    inputText = "",
                    selectedImageUri = null,
                    isGenerating = true,
                    error = null,
                    messageTokenCounts = it.messageTokenCounts + (userMessage.id to userTokenCount)
                )
            }

            generateResponse(conversationId, model)
        }
    }

    private suspend fun createNewConversation(firstMessage: String, modelId: String): String {
        val title = if (firstMessage.length > 50) {
            firstMessage.take(47) + "..."
        } else {
            firstMessage.ifEmpty { "New conversation" }
        }

        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            modelConfigId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        conversationRepository.createConversation(conversation)
        return conversation.id
    }

    private fun generateResponse(conversationId: String, model: ModelConfig) {
        val assistantMessageId = UUID.randomUUID().toString()
        currentStreamingMessageId = assistantMessageId
        currentGeneratingConversationId = conversationId
        currentGeneratingModel = model
        fullRawContent = ""

        val thinkingEnabled = _uiState.value.thinkingEnabled && model.supportsThinking
        currentGeneratingThinkingEnabled = thinkingEnabled

        val streamingMessage = Message(
            id = assistantMessageId,
            conversationId = conversationId,
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
            isThinking = thinkingEnabled
        )

        _uiState.update {
            it.copy(messages = it.messages + streamingMessage, isGenerating = true)
        }

        val settings = _uiState.value.settings
        val messages = _uiState.value.messages.filter { it.id != assistantMessageId }

        val startGeneration: () -> Unit = {
            generationService?.startGeneration(
                modelConfig = model,
                messages = messages,
                settings = settings,
                thinkingEnabled = thinkingEnabled
            )
        }

        if (serviceBound && generationService != null) {
            startGeneration()
        } else {
            // Service not yet bound, queue the generation
            pendingGeneration = startGeneration
            GenerationService.startService(context)
        }
    }

    private fun parseThinkingContent(rawContent: String, modelSupportsThinking: Boolean): Pair<String, String> {
        // Check for explicit <think>...</think> tags
        val thinkPattern = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val match = thinkPattern.find(rawContent)

        return when {
            match != null -> {
                val thinking = match.groupValues[1].trim()
                val content = rawContent.replace(match.value, "").trim()
                Pair(thinking, content)
            }
            modelSupportsThinking -> {
                // If model supports thinking but no explicit tags, check for </think> as end marker
                val endIndex = rawContent.indexOf("</think>")
                if (endIndex != -1) {
                    val thinking = rawContent.substring(0, endIndex).removePrefix("<think>").trim()
                    val content = rawContent.substring(endIndex + "</think>".length).trim()
                    Pair(thinking, content)
                } else {
                    Pair("", rawContent)
                }
            }
            else -> Pair("", rawContent)
        }
    }

    private fun parseThinkingContentStreaming(
        rawContent: String,
        modelSupportsThinking: Boolean
    ): Triple<String, String, Boolean> {
        val thinking: String
        val content: String
        val stillThinking: Boolean

        // Check if we have a complete </think> tag
        val thinkEndIndex = rawContent.indexOf("</think>")
        val thinkStartIndex = rawContent.indexOf("<think>")

        when {
            // Has explicit <think>...</think> pattern
            thinkStartIndex != -1 && thinkEndIndex != -1 && thinkEndIndex > thinkStartIndex -> {
                thinking = rawContent.substring(thinkStartIndex + "<think>".length, thinkEndIndex).trim()
                content = rawContent.substring(thinkEndIndex + "</think>".length).trim()
                stillThinking = false
            }
            // Has explicit <think> but no </think> yet
            thinkStartIndex != -1 && thinkEndIndex == -1 -> {
                thinking = rawContent.substring(thinkStartIndex + "<think>".length).trim()
                content = ""
                stillThinking = true
            }
            // Model supports thinking (implicit thinking) and has </think>
            modelSupportsThinking && thinkEndIndex != -1 -> {
                thinking = rawContent.substring(0, thinkEndIndex).removePrefix("<think>").trim()
                content = rawContent.substring(thinkEndIndex + "</think>".length).trim()
                stillThinking = false
            }
            // Model supports thinking (implicit thinking), no </think> yet
            modelSupportsThinking && thinkEndIndex == -1 -> {
                thinking = rawContent.removePrefix("<think>").trim()
                content = ""
                stillThinking = true
            }
            // No thinking support and no <think> tag
            else -> {
                thinking = ""
                content = rawContent
                stillThinking = false
            }
        }

        return Triple(thinking, content, stillThinking)
    }

    fun stopGeneration() {
        generationService?.stopGeneration()
        currentStreamingMessageId?.let { messageId ->
            val currentContent = _uiState.value.messages.find { it.id == messageId }?.content ?: ""
            val currentThinking = _uiState.value.messages.find { it.id == messageId }?.thinkingContent
            if (currentContent.isNotEmpty()) {
                viewModelScope.launch {
                    val conversationId = _uiState.value.currentConversationId ?: return@launch
                    val finalMessage = Message(
                        id = messageId,
                        conversationId = conversationId,
                        role = MessageRole.ASSISTANT,
                        content = currentContent,
                        thinkingContent = currentThinking,
                        isStreaming = false,
                        isThinking = false
                    )
                    messageRepository.insertMessage(finalMessage)

                    _uiState.update {
                        it.copy(
                            messages = it.messages.map { msg ->
                                if (msg.id == messageId) {
                                    finalMessage
                                } else {
                                    msg
                                }
                            },
                            isGenerating = false
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        messages = it.messages.filter { it.id != messageId },
                        isGenerating = false
                    )
                }
            }
        }
        currentStreamingMessageId = null
        currentGeneratingConversationId = null
        currentGeneratingModel = null
        currentGeneratingThinkingEnabled = false
        fullRawContent = ""
    }

    fun regenerateMessage(messageId: String) {
        val state = _uiState.value
        val conversationId = state.currentConversationId ?: return
        val model = state.selectedModel ?: return
        if (state.isGenerating) return

        val messageIndex = state.messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return

        val originalMessage = state.messages[messageIndex]
        if (originalMessage.role != MessageRole.ASSISTANT) return

        // Store original content (from main content field) as alternatives[0]
        // Then keep all existing alternatives after that
        // The new generation will be added by handleGenerationComplete
        val originalAsAlternative = MessageAlternative(
            content = originalMessage.content,
            thinkingContent = originalMessage.thinkingContent
        )

        // Build alternatives list: [original, ...existing alternatives]
        // handleGenerationComplete will add the new generation and restore original to content
        val alternativesForStreaming = listOf(originalAsAlternative) + originalMessage.alternatives

        // Calculate the final index that will be used after completion
        // After completion: alternatives = originalMessage.alternatives + newAlternative
        // So finalIndex = originalMessage.alternatives.size + 1
        // Using this same index during streaming prevents animation on completion
        val finalIndex = originalMessage.alternatives.size + 1

        // Create a placeholder message that will be updated during streaming
        // Using finalIndex ensures no animation triggers when streaming completes
        // displayContent checks isStreaming and returns content directly during streaming
        val regeneratingMessage = originalMessage.copy(
            content = "",  // Will be filled with streaming content
            thinkingContent = null,
            isStreaming = true,
            isThinking = _uiState.value.thinkingEnabled && model.supportsThinking,
            alternatives = alternativesForStreaming,
            currentAlternativeIndex = finalIndex
        )

        currentStreamingMessageId = messageId
        currentGeneratingConversationId = conversationId
        currentGeneratingModel = model
        fullRawContent = ""

        val thinkingEnabled = _uiState.value.thinkingEnabled && model.supportsThinking
        currentGeneratingThinkingEnabled = thinkingEnabled

        _uiState.update { uiState ->
            uiState.copy(
                messages = uiState.messages.map { msg ->
                    if (msg.id == messageId) regeneratingMessage else msg
                },
                isGenerating = true
            )
        }

        // Get messages up to but not including this assistant message
        val messagesForContext = state.messages.take(messageIndex)

        val settings = _uiState.value.settings

        val startGeneration: () -> Unit = {
            generationService?.startGeneration(
                modelConfig = model,
                messages = messagesForContext,
                settings = settings,
                thinkingEnabled = thinkingEnabled
            )
        }

        if (serviceBound && generationService != null) {
            startGeneration()
        } else {
            pendingGeneration = startGeneration
            GenerationService.startService(context)
        }
    }

    fun previousAlternative(messageId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId && msg.currentAlternativeIndex > 0) {
                        msg.copy(currentAlternativeIndex = msg.currentAlternativeIndex - 1)
                    } else {
                        msg
                    }
                }
            )
        }
    }

    fun nextAlternative(messageId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId && msg.currentAlternativeIndex < msg.totalAlternatives - 1) {
                        msg.copy(currentAlternativeIndex = msg.currentAlternativeIndex + 1)
                    } else {
                        msg
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun deleteMessage(messageId: String) {
        val state = _uiState.value
        val messageIndex = state.messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return

        val message = state.messages[messageIndex]

        viewModelScope.launch {
            // Delete from database
            messageRepository.deleteMessage(messageId)

            // If this is a user message, also delete the following assistant response
            val messagesToRemove = mutableListOf(messageId)
            if (message.role == MessageRole.USER &&
                messageIndex + 1 < state.messages.size &&
                state.messages[messageIndex + 1].role == MessageRole.ASSISTANT) {
                val assistantMessage = state.messages[messageIndex + 1]
                messageRepository.deleteMessage(assistantMessage.id)
                messagesToRemove.add(assistantMessage.id)
            }

            // Update UI state
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages.filter { it.id !in messagesToRemove },
                    messageTokenCounts = currentState.messageTokenCounts.filterKeys { it !in messagesToRemove }
                )
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        val state = _uiState.value
        val messageIndex = state.messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return

        val message = state.messages[messageIndex]
        val updatedMessage = message.copy(content = newContent)

        viewModelScope.launch {
            // Update in database
            messageRepository.insertMessage(updatedMessage)

            // Update conversation timestamp
            state.currentConversationId?.let {
                conversationRepository.updateConversationTimestamp(it)
            }

            // Compute new token count
            val newTokenCount = computeTokenCount(updatedMessage)

            // Update UI state
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages.map { msg ->
                        if (msg.id == messageId) updatedMessage else msg
                    },
                    messageTokenCounts = currentState.messageTokenCounts + (messageId to newTokenCount)
                )
            }
        }
    }
}
