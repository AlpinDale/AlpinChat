package com.alpin.chat.ui.screens.tokenizer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpin.chat.tokenizer.NativeTokenizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TokenInfo(
    val id: Int,
    val text: String,
    val colorIndex: Int
)

data class TokenizerUiState(
    val tokenizerLoaded: Boolean = false,
    val tokenizerLoading: Boolean = true,
    val vocabSize: Int = 0,
    val inputText: String = "",
    val tokens: List<TokenInfo> = emptyList(),
    val isTokenizing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TokenizerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TokenizerUiState())
    val uiState: StateFlow<TokenizerUiState> = _uiState.asStateFlow()

    private var tokenizer: NativeTokenizer? = null
    private var tokenizeJob: Job? = null

    init {
        initializeTokenizer()
    }

    private fun initializeTokenizer() {
        viewModelScope.launch {
            _uiState.update { it.copy(tokenizerLoading = true) }
            withContext(Dispatchers.IO) {
                try {
                    val tok = NativeTokenizer.getGlm4Tokenizer(context)
                    tokenizer = tok
                    _uiState.update {
                        it.copy(
                            tokenizerLoaded = tok.isLoaded,
                            tokenizerLoading = false,
                            vocabSize = tok.vocabSize,
                            errorMessage = if (!tok.isLoaded) "Failed to load tokenizer" else null
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            tokenizerLoaded = false,
                            tokenizerLoading = false,
                            errorMessage = e.message ?: "Unknown error"
                        )
                    }
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }

        // Debounce tokenization
        tokenizeJob?.cancel()
        tokenizeJob = viewModelScope.launch {
            delay(150) // Small delay to avoid tokenizing on every keystroke
            tokenizeText(text)
        }
    }

    private suspend fun tokenizeText(text: String) {
        val tok = tokenizer ?: return
        if (!tok.isLoaded) return

        if (text.isEmpty()) {
            _uiState.update { it.copy(tokens = emptyList(), isTokenizing = false) }
            return
        }

        _uiState.update { it.copy(isTokenizing = true) }

        val tokenInfoList = withContext(Dispatchers.IO) {
            try {
                val tokenIds = tok.encode(text)
                tokenIds.mapIndexed { index, tokenId ->
                    // Decode each token individually to get its text representation
                    val tokenText = tok.decode(intArrayOf(tokenId))
                    TokenInfo(
                        id = tokenId,
                        text = tokenText,
                        colorIndex = index
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        _uiState.update {
            it.copy(
                tokens = tokenInfoList,
                isTokenizing = false
            )
        }
    }

    fun clearInput() {
        _uiState.update { it.copy(inputText = "", tokens = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        tokenizeJob?.cancel()
    }
}
