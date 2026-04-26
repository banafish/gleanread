package com.gleanread.android.data.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseSessionStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _session = MutableStateFlow(readSession())

    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    fun saveSession(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis ?: 0L)
            .apply()
        _session.value = session
    }

    fun clearSession() {
        preferences.edit().clear().apply()
        _session.value = null
    }

    private fun readSession(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)?.takeIf(String::isNotBlank)
            ?: return null
        val userId = preferences.getString(KEY_USER_ID, null)?.takeIf(String::isNotBlank)
            ?: return null
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0L }
        return AuthSession(
            accessToken = accessToken,
            refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null),
            userId = userId,
            email = preferences.getString(KEY_EMAIL, null),
            expiresAtMillis = expiresAt,
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "glean_supabase_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
