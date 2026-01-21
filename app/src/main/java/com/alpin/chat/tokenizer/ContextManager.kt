package com.alpin.chat.tokenizer

import android.content.Context
import com.alpin.chat.data.local.datastore.AppSettings
import com.alpin.chat.domain.model.Message
import com.alpin.chat.domain.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages conversation context by ensuring messages fit within the model's context length.
 *
 * Trimming strategy:
 * - Always preserve the system prompt
 * - Always preserve the first user message and its assistant response (if any)
 * - Trim older messages from the middle when context limit is exceeded
 * - Messages are removed in pairs (user + assistant) to maintain conversation coherence
 */
@Singleton
class ContextManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private var tokenizer: NativeTokenizer? = null
    private var tokenizerInitialized = false

    /**
     * Initialize the tokenizer. Call this before using other methods.
     */
    suspend fun initialize() {
        if (tokenizerInitialized) return

        withContext(Dispatchers.IO) {
            try {
                tokenizer = NativeTokenizer.getGlm4Tokenizer(context)
                tokenizerInitialized = true
            } catch (e: Exception) {
                // Tokenizer failed to load - will fall back to estimation
                tokenizerInitialized = true
            }
        }
    }

    /**
     * Count tokens in a text string.
     * Falls back to character-based estimation if tokenizer is not available.
     */
    fun countTokens(text: String): Int {
        val tok = tokenizer
        return if (tok != null && tok.isLoaded) {
            tok.countTokens(text)
        } else {
            // Fallback: estimate ~4 characters per token (rough approximation)
            (text.length / 4).coerceAtLeast(1)
        }
    }

    /**
     * Count tokens for a message, including role formatting overhead and thinking content.
     * Used for context management calculations.
     */
    fun countMessageTokens(message: Message): Int {
        // Account for message formatting overhead (role tags, etc.)
        // Typical overhead: ~4 tokens for role + delimiters
        val overhead = 4
        val contentTokens = countTokens(message.displayContent)
        val thinkingTokens = message.displayThinkingContent?.let {
            // Add overhead for <think> tags (~2 tokens)
            2 + countTokens(it)
        } ?: 0
        val imageTokens = if (message.imageData != null) {
            // Vision models typically use ~85 tokens per image tile
            // Conservative estimate for a single image
            170
        } else {
            0
        }
        return overhead + contentTokens + thinkingTokens + imageTokens
    }

    /**
     * Count tokens for display in the UI (content only, no formatting overhead).
     */
    fun countDisplayTokens(message: Message): Int {
        val contentTokens = countTokens(message.displayContent)
        val thinkingTokens = message.displayThinkingContent?.let { countTokens(it) } ?: 0
        val imageTokens = if (message.imageData != null) 170 else 0
        return contentTokens + thinkingTokens + imageTokens
    }

    /**
     * Count tokens for the system prompt with formatting overhead.
     */
    fun countSystemPromptTokens(systemPrompt: String): Int {
        if (systemPrompt.isBlank()) return 0
        // System prompt overhead: ~4 tokens
        return 4 + countTokens(systemPrompt)
    }

    /**
     * Trim messages to fit within the context length.
     *
     * @param messages All messages in the conversation
     * @param settings App settings containing the system prompt
     * @param contextLength Maximum tokens allowed
     * @param reserveForResponse Tokens to reserve for the model's response (default 1024)
     * @return List of messages that fit within the context limit
     */
    suspend fun trimToContext(
        messages: List<Message>,
        settings: AppSettings,
        contextLength: Int,
        reserveForResponse: Int = 1024
    ): List<Message> {
        initialize()

        val maxTokens = contextLength - reserveForResponse
        if (maxTokens <= 0) return emptyList()

        // Calculate system prompt tokens
        val systemPromptTokens = countSystemPromptTokens(settings.systemPrompt)
        var availableTokens = maxTokens - systemPromptTokens

        if (availableTokens <= 0) {
            // System prompt alone exceeds limit - return empty (shouldn't happen normally)
            return emptyList()
        }

        // If messages fit, return all of them
        val allTokens = messages.sumOf { countMessageTokens(it) }
        if (allTokens <= availableTokens) {
            return messages
        }

        // Find the first user message and its response (if any)
        val firstUserIndex = messages.indexOfFirst { it.role == MessageRole.USER }
        if (firstUserIndex == -1) {
            // No user messages - return all that fit
            return takeMessagesWithinLimit(messages, availableTokens)
        }

        // Identify protected messages: first user message and following assistant message
        val protectedIndices = mutableSetOf(firstUserIndex)
        if (firstUserIndex + 1 < messages.size &&
            messages[firstUserIndex + 1].role == MessageRole.ASSISTANT) {
            protectedIndices.add(firstUserIndex + 1)
        }

        // Calculate tokens for protected messages
        val protectedTokens = protectedIndices.sumOf { countMessageTokens(messages[it]) }
        availableTokens -= protectedTokens

        if (availableTokens <= 0) {
            // Only protected messages fit
            return messages.filterIndexed { index, _ -> index in protectedIndices }
        }

        // Get remaining messages (not protected)
        val remainingMessages = messages.filterIndexed { index, _ -> index !in protectedIndices }

        // Take as many recent messages as possible from the end
        val fittingMessages = takeMessagesFromEndWithinLimit(remainingMessages, availableTokens)

        // Reconstruct the message list: protected messages first, then fitting recent messages
        val result = mutableListOf<Message>()

        // Add protected messages in order
        protectedIndices.sorted().forEach { index ->
            result.add(messages[index])
        }

        // Add fitting messages (these are the most recent ones that fit)
        result.addAll(fittingMessages)

        return result
    }

    /**
     * Take messages from the start of the list until the token limit is reached.
     */
    private fun takeMessagesWithinLimit(messages: List<Message>, maxTokens: Int): List<Message> {
        val result = mutableListOf<Message>()
        var tokenCount = 0

        for (message in messages) {
            val messageTokens = countMessageTokens(message)
            if (tokenCount + messageTokens > maxTokens) break
            result.add(message)
            tokenCount += messageTokens
        }

        return result
    }

    /**
     * Take messages from the end of the list (most recent) until the token limit is reached.
     * Returns messages in their original order.
     */
    private fun takeMessagesFromEndWithinLimit(messages: List<Message>, maxTokens: Int): List<Message> {
        val result = mutableListOf<Message>()
        var tokenCount = 0

        // Iterate from the end
        for (message in messages.reversed()) {
            val messageTokens = countMessageTokens(message)
            if (tokenCount + messageTokens > maxTokens) break
            result.add(0, message) // Add to front to maintain order
            tokenCount += messageTokens
        }

        return result
    }

    /**
     * Get debug info about token counts for messages.
     */
    fun getTokenInfo(
        messages: List<Message>,
        settings: AppSettings
    ): TokenInfo {
        val systemPromptTokens = countSystemPromptTokens(settings.systemPrompt)
        val messageTokens = messages.sumOf { countMessageTokens(it) }

        return TokenInfo(
            systemPromptTokens = systemPromptTokens,
            messageTokens = messageTokens,
            totalTokens = systemPromptTokens + messageTokens,
            messageCount = messages.size,
            tokenizerLoaded = tokenizer?.isLoaded == true
        )
    }

    data class TokenInfo(
        val systemPromptTokens: Int,
        val messageTokens: Int,
        val totalTokens: Int,
        val messageCount: Int,
        val tokenizerLoaded: Boolean
    )
}
