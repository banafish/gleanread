package com.gleanread.android.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_config_preferences",
)

class AiConfigRepository(private val context: Context) {
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val tokenKey = stringPreferencesKey("token")
    private val modelKey = stringPreferencesKey("model")

    val aiConfigFlow: Flow<AiConfig> = context.aiConfigDataStore.data.map { preferences ->
        AiConfig(
            baseUrl = preferences[baseUrlKey].orEmpty(),
            token = preferences[tokenKey].orEmpty(),
            model = preferences[modelKey].orEmpty(),
        )
    }

    suspend fun currentConfig(): AiConfig {
        return aiConfigFlow.first()
    }

    suspend fun setAiConfig(config: AiConfig) {
        context.aiConfigDataStore.edit { preferences ->
            preferences[baseUrlKey] = config.baseUrl
            preferences[tokenKey] = config.token
            preferences[modelKey] = config.model
        }
    }
}
