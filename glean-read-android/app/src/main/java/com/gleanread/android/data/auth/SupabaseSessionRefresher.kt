package com.gleanread.android.data.auth

import com.gleanread.android.data.remote.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseSessionRefresher(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient,
    private val sessionStore: SupabaseSessionStore,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val refreshMutex = Mutex()

    suspend fun currentSessionOrRefresh(force: Boolean = false): AuthSession? {
        val session = sessionStore.session.value ?: return null
        if (!force && !session.shouldRefresh(nowMillis())) {
            return session
        }
        return refreshMutex.withLock {
            val latestSession = sessionStore.session.value ?: return@withLock null
            if (!force && !latestSession.shouldRefresh(nowMillis())) {
                return@withLock latestSession
            }
            refreshSession(latestSession)
        }
    }

    private suspend fun refreshSession(session: AuthSession): AuthSession {
        val refreshToken = session.refreshToken?.takeIf(String::isNotBlank)
            ?: run {
                sessionStore.clearSession()
                throw SupabaseSessionRefreshException("登录已过期，请重新登录")
            }
        if (!config.isConfigured) {
            throw SupabaseSessionRefreshException("Supabase 尚未配置")
        }

        val response = httpClient.post("${config.normalizedUrl}/auth/v1/token") {
            parameter("grant_type", "refresh_token")
            header("apikey", config.anonKey)
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }
        val responseBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            sessionStore.clearSession()
            throw SupabaseSessionRefreshException(responseBody.toSupabaseAuthErrorMessage())
        }

        val refreshedSession = AuthJson.decodeFromString<AuthTokenResponse>(responseBody)
            .toSession(
                fallbackRefreshToken = session.refreshToken,
                fallbackEmail = session.email,
                nowMillis = nowMillis(),
            )
        sessionStore.saveSession(refreshedSession)
        return refreshedSession
    }

    private fun AuthSession.shouldRefresh(now: Long): Boolean {
        if (refreshToken.isNullOrBlank()) return false
        val expiresAt = expiresAtMillis ?: return true
        return expiresAt <= now + REFRESH_LEEWAY_MILLIS
    }

    private companion object {
        const val REFRESH_LEEWAY_MILLIS = 60_000L
    }
}

class SupabaseSessionRefreshException(message: String) : RuntimeException(message)

@Serializable
private data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
)
