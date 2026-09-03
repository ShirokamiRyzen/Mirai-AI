package com.ryzumi.miraiai.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ThemeSettings(
    val themeMode: String = "system", // "system", "light", "dark"
    val isMonetEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private val gson = Gson()

    companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_TOP_P = floatPreferencesKey("top_p")
        val KEY_REPETITION_PENALTY = floatPreferencesKey("repetition_penalty")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_CUSTOM_HEADERS = stringPreferencesKey("custom_headers")
        val KEY_AVAILABLE_MODELS = stringPreferencesKey("available_models")

        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_MONET_ENABLED = booleanPreferencesKey("monet_enabled")
        val KEY_DEBUG_LOGGING_ENABLED = booleanPreferencesKey("debug_logging_enabled")
        val KEY_SHOW_THINKING_PROCESS = booleanPreferencesKey("show_thinking_process")
        val KEY_TOKEN_COUNTER_ENABLED = booleanPreferencesKey("token_counter_enabled")
        val KEY_ALLOW_DEVICE_CONTEXT = booleanPreferencesKey("allow_device_context")
    }

    val allowDeviceContextFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ALLOW_DEVICE_CONTEXT] ?: false
    }

    suspend fun updateAllowDeviceContext(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ALLOW_DEVICE_CONTEXT] = enabled
        }
    }

    val debugLoggingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEBUG_LOGGING_ENABLED] ?: false
    }

    suspend fun updateDebugLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEBUG_LOGGING_ENABLED] = enabled
        }
    }

    val showThinkingProcessFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_THINKING_PROCESS] ?: false
    }

    suspend fun updateShowThinkingProcess(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_THINKING_PROCESS] = enabled
        }
    }

    val tokenCounterEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOKEN_COUNTER_ENABLED] ?: false
    }

    suspend fun updateTokenCounterEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN_COUNTER_ENABLED] = enabled
        }
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { preferences ->
        ThemeSettings(
            themeMode = preferences[KEY_THEME_MODE] ?: "system",
            isMonetEnabled = preferences[KEY_MONET_ENABLED] ?: true
        )
    }

    suspend fun updateThemeSettings(themeMode: String? = null, isMonetEnabled: Boolean? = null) {
        context.dataStore.edit { preferences ->
            themeMode?.let { preferences[KEY_THEME_MODE] = it }
            isMonetEnabled?.let { preferences[KEY_MONET_ENABLED] = it }
        }
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val modelsJson = preferences[KEY_AVAILABLE_MODELS] ?: "[]"
        val modelsList: List<String> = try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(modelsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        AppSettings(
            baseUrl = preferences[KEY_BASE_URL] ?: "https://openrouter.ai/api/v1",
            apiKey = preferences[KEY_API_KEY] ?: "",
            selectedModelId = preferences[KEY_SELECTED_MODEL_ID] ?: "gpt-3.5-turbo",
            temperature = preferences[KEY_TEMPERATURE] ?: 0.7f,
            topP = preferences[KEY_TOP_P] ?: 0.9f,
            repetitionPenalty = preferences[KEY_REPETITION_PENALTY] ?: 1.1f,
            maxTokens = preferences[KEY_MAX_TOKENS] ?: 2048,
            customHeaders = preferences[KEY_CUSTOM_HEADERS] ?: "",
            availableModels = modelsList
        )
    }

    suspend fun updateSettings(
        baseUrl: String? = null,
        apiKey: String? = null,
        selectedModelId: String? = null,
        temperature: Float? = null,
        topP: Float? = null,
        repetitionPenalty: Float? = null,
        maxTokens: Int? = null,
        customHeaders: String? = null
    ) {
        context.dataStore.edit { preferences ->
            baseUrl?.let { preferences[KEY_BASE_URL] = it }
            apiKey?.let { preferences[KEY_API_KEY] = it }
            selectedModelId?.let { preferences[KEY_SELECTED_MODEL_ID] = it }
            temperature?.let { preferences[KEY_TEMPERATURE] = it }
            topP?.let { preferences[KEY_TOP_P] = it }
            repetitionPenalty?.let { preferences[KEY_REPETITION_PENALTY] = it }
            maxTokens?.let { preferences[KEY_MAX_TOKENS] = it }
            customHeaders?.let { preferences[KEY_CUSTOM_HEADERS] = it }
        }
    }

    suspend fun updateAvailableModels(models: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AVAILABLE_MODELS] = gson.toJson(models)
        }
    }

    suspend fun updateSelectedModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODEL_ID] = modelId
        }
    }
}
