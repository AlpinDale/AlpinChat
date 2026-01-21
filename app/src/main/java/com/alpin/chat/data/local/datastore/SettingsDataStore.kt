package com.alpin.chat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val systemPrompt: String = "You are a helpful assistant.",
    val defaultModelId: String? = null,
    val streamResponses: Boolean = true,
    val confirmDeleteConversation: Boolean = true,
    val thinkingEnabled: Boolean = false,
    // Sampling parameters - null means disabled/not sent
    val temperature: Float? = 0.7f,
    val topP: Float? = null,
    val topK: Int? = null,
    val minP: Float? = null,
    val presencePenalty: Float? = null,
    val frequencyPenalty: Float? = null,
    val repetitionPenalty: Float? = null
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val STREAM_RESPONSES = booleanPreferencesKey("stream_responses")
        val CONFIRM_DELETE_CONVERSATION = booleanPreferencesKey("confirm_delete_conversation")
        val THINKING_ENABLED = booleanPreferencesKey("thinking_enabled")
        // Sampling params - we use special sentinel values to indicate "disabled"
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TEMPERATURE_ENABLED = booleanPreferencesKey("temperature_enabled")
        val TOP_P = floatPreferencesKey("top_p")
        val TOP_P_ENABLED = booleanPreferencesKey("top_p_enabled")
        val TOP_K = intPreferencesKey("top_k")
        val TOP_K_ENABLED = booleanPreferencesKey("top_k_enabled")
        val MIN_P = floatPreferencesKey("min_p")
        val MIN_P_ENABLED = booleanPreferencesKey("min_p_enabled")
        val PRESENCE_PENALTY = floatPreferencesKey("presence_penalty")
        val PRESENCE_PENALTY_ENABLED = booleanPreferencesKey("presence_penalty_enabled")
        val FREQUENCY_PENALTY = floatPreferencesKey("frequency_penalty")
        val FREQUENCY_PENALTY_ENABLED = booleanPreferencesKey("frequency_penalty_enabled")
        val REPETITION_PENALTY = floatPreferencesKey("repetition_penalty")
        val REPETITION_PENALTY_ENABLED = booleanPreferencesKey("repetition_penalty_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            systemPrompt = preferences[Keys.SYSTEM_PROMPT] ?: "You are a helpful assistant.",
            defaultModelId = preferences[Keys.DEFAULT_MODEL_ID],
            streamResponses = preferences[Keys.STREAM_RESPONSES] ?: true,
            confirmDeleteConversation = preferences[Keys.CONFIRM_DELETE_CONVERSATION] ?: true,
            thinkingEnabled = preferences[Keys.THINKING_ENABLED] ?: false,
            temperature = if (preferences[Keys.TEMPERATURE_ENABLED] != false) {
                preferences[Keys.TEMPERATURE] ?: 0.7f
            } else null,
            topP = if (preferences[Keys.TOP_P_ENABLED] == true) {
                preferences[Keys.TOP_P] ?: 1.0f
            } else null,
            topK = if (preferences[Keys.TOP_K_ENABLED] == true) {
                preferences[Keys.TOP_K] ?: 40
            } else null,
            minP = if (preferences[Keys.MIN_P_ENABLED] == true) {
                preferences[Keys.MIN_P] ?: 0.0f
            } else null,
            presencePenalty = if (preferences[Keys.PRESENCE_PENALTY_ENABLED] == true) {
                preferences[Keys.PRESENCE_PENALTY] ?: 0.0f
            } else null,
            frequencyPenalty = if (preferences[Keys.FREQUENCY_PENALTY_ENABLED] == true) {
                preferences[Keys.FREQUENCY_PENALTY] ?: 0.0f
            } else null,
            repetitionPenalty = if (preferences[Keys.REPETITION_PENALTY_ENABLED] == true) {
                preferences[Keys.REPETITION_PENALTY] ?: 1.0f
            } else null
        )
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateSystemPrompt(systemPrompt: String) {
        dataStore.edit { preferences ->
            preferences[Keys.SYSTEM_PROMPT] = systemPrompt
        }
    }

    suspend fun updateDefaultModelId(modelId: String?) {
        dataStore.edit { preferences ->
            if (modelId != null) {
                preferences[Keys.DEFAULT_MODEL_ID] = modelId
            } else {
                preferences.remove(Keys.DEFAULT_MODEL_ID)
            }
        }
    }

    suspend fun updateStreamResponses(stream: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STREAM_RESPONSES] = stream
        }
    }

    suspend fun updateConfirmDeleteConversation(confirm: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.CONFIRM_DELETE_CONVERSATION] = confirm
        }
    }

    suspend fun updateThinkingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.THINKING_ENABLED] = enabled
        }
    }

    suspend fun updateTemperature(value: Float?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TEMPERATURE_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.TEMPERATURE] = value.coerceIn(0f, 2f)
            }
        }
    }

    suspend fun updateTopP(value: Float?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TOP_P_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.TOP_P] = value.coerceIn(0f, 1f)
            }
        }
    }

    suspend fun updateTopK(value: Int?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TOP_K_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.TOP_K] = value.coerceIn(1, 1000)
            }
        }
    }

    suspend fun updateMinP(value: Float?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.MIN_P_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.MIN_P] = value.coerceIn(0f, 1f)
            }
        }
    }

    suspend fun updatePresencePenalty(value: Float?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.PRESENCE_PENALTY_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.PRESENCE_PENALTY] = value.coerceIn(-2f, 2f)
            }
        }
    }

    suspend fun updateFrequencyPenalty(value: Float?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.FREQUENCY_PENALTY_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.FREQUENCY_PENALTY] = value.coerceIn(-2f, 2f)
            }
        }
    }

    suspend fun updateRepetitionPenalty(value: Float?, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.REPETITION_PENALTY_ENABLED] = enabled
            if (value != null) {
                preferences[Keys.REPETITION_PENALTY] = value.coerceIn(0f, 2f)
            }
        }
    }
}
