package com.alpin.chat.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpin.chat.data.local.datastore.AppSettings
import com.alpin.chat.data.local.datastore.SettingsDataStore
import com.alpin.chat.data.local.datastore.ThemeMode
import com.alpin.chat.data.remote.api.LLMApiService
import com.alpin.chat.data.repository.ModelConfigRepository
import com.alpin.chat.domain.model.ModelConfig
import com.alpin.chat.tokenizer.NativeTokenizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val models: List<ModelConfig> = emptyList(),
    val isTestingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null,
    val availableModels: List<String> = emptyList(),
    val isFetchingModels: Boolean = false,
    val fetchModelsError: String? = null,
    // Tokenizer status (just for indicator)
    val tokenizerLoaded: Boolean = false
)

sealed class ConnectionTestResult {
    data object Success : ConnectionTestResult()
    data class Failure(val message: String) : ConnectionTestResult()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val modelConfigRepository: ModelConfigRepository,
    private val llmApiService: LLMApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadModels()
        initializeTokenizer()
    }

    private fun initializeTokenizer() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val tok = NativeTokenizer.getGlm4Tokenizer(context)
                    _uiState.update { it.copy(tokenizerLoaded = tok.isLoaded) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(tokenizerLoaded = false) }
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            modelConfigRepository.getAllModelConfigs().collect { models ->
                _uiState.update { it.copy(models = models) }
            }
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.updateThemeMode(themeMode)
        }
    }

    fun updateTemperature(value: Float?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateTemperature(value, enabled)
        }
    }

    fun updateTopP(value: Float?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateTopP(value, enabled)
        }
    }

    fun updateTopK(value: Int?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateTopK(value, enabled)
        }
    }

    fun updateMinP(value: Float?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateMinP(value, enabled)
        }
    }

    fun updatePresencePenalty(value: Float?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updatePresencePenalty(value, enabled)
        }
    }

    fun updateFrequencyPenalty(value: Float?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateFrequencyPenalty(value, enabled)
        }
    }

    fun updateRepetitionPenalty(value: Float?, enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateRepetitionPenalty(value, enabled)
        }
    }

    fun updateSystemPrompt(systemPrompt: String) {
        viewModelScope.launch {
            settingsDataStore.updateSystemPrompt(systemPrompt)
        }
    }

    fun saveModel(model: ModelConfig) {
        viewModelScope.launch {
            if (_uiState.value.models.isEmpty()) {
                modelConfigRepository.insertModelConfig(model.copy(isDefault = true))
            } else {
                modelConfigRepository.insertModelConfig(model)
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            modelConfigRepository.deleteModelConfig(modelId)
        }
    }

    fun setDefaultModel(modelId: String) {
        viewModelScope.launch {
            modelConfigRepository.setDefaultModel(modelId)
        }
    }

    fun testConnection(model: ModelConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            val result = llmApiService.testConnection(model)
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    connectionTestResult = if (result.isSuccess) {
                        ConnectionTestResult.Success
                    } else {
                        ConnectionTestResult.Failure(
                            result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    }
                )
            }
        }
    }

    fun clearConnectionTestResult() {
        _uiState.update { it.copy(connectionTestResult = null) }
    }

    fun fetchAvailableModels(baseUrl: String, apiKey: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, fetchModelsError = null, availableModels = emptyList()) }
            val result = llmApiService.fetchAvailableModels(baseUrl, apiKey)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isFetchingModels = false,
                        availableModels = result.getOrDefault(emptyList())
                    )
                } else {
                    it.copy(
                        isFetchingModels = false,
                        fetchModelsError = result.exceptionOrNull()?.message ?: "Failed to fetch models"
                    )
                }
            }
        }
    }

    fun clearAvailableModels() {
        _uiState.update { it.copy(availableModels = emptyList(), fetchModelsError = null) }
    }
}
