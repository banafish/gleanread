package com.gleanread.android.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

    // 使用 EncryptedSharedPreferences 来安全存储敏感的 AI Token
    private val securePreferences = run {
        val name = "glean_ai_secure_token"
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    val aiConfigFlow: Flow<AiConfig> = context.aiConfigDataStore.data.map { preferences ->
        var token = securePreferences.getString(KEY_SECURE_TOKEN, null)
        if (token == null) {
            // 兼容性迁移：如果加密 Preference 中没有 token，尝试从原 DataStore 迁移旧的明文 token
            val oldToken = preferences[tokenKey]
            if (!oldToken.isNullOrBlank()) {
                token = oldToken
                securePreferences.edit().putString(KEY_SECURE_TOKEN, oldToken).apply()
            } else {
                token = ""
            }
        }
        AiConfig(
            baseUrl = preferences[baseUrlKey].orEmpty(),
            token = token,
            model = preferences[modelKey].orEmpty(),
        )
    }

    suspend fun currentConfig(): AiConfig {
        return aiConfigFlow.first()
    }

    suspend fun setAiConfig(config: AiConfig) {
        securePreferences.edit().putString(KEY_SECURE_TOKEN, config.token).apply()
        context.aiConfigDataStore.edit { preferences ->
            preferences[baseUrlKey] = config.baseUrl
            preferences[modelKey] = config.model
            // 彻底抹除旧的明文 token 存储，降低泄漏隐患
            preferences.remove(tokenKey)
        }
    }

    private companion object {
        const val KEY_SECURE_TOKEN = "secure_token"
    }
}
