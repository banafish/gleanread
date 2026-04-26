package com.gleanread.android.data.auth

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.remote.SupabaseConfig
import com.gleanread.android.data.sync.DeviceIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabaseAuthRepository(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient,
    private val sessionStore: SupabaseSessionStore,
    private val database: WorkspaceDatabase,
    private val deviceIdProvider: DeviceIdProvider,
) {
    val session: StateFlow<AuthSession?> = sessionStore.session

    suspend fun signInWithEmailPassword(email: String, password: String): AuthResult {
        if (!config.isConfigured) {
            return AuthResult.Failure("Supabase 尚未配置")
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            return AuthResult.Failure("请输入邮箱和密码")
        }

        return runCatching {
            val httpResponse = httpClient.post("${config.normalizedUrl}/auth/v1/token") {
                parameter("grant_type", "password")
                header("apikey", config.anonKey)
                contentType(ContentType.Application.Json)
                setBody(EmailPasswordSignInRequest(trimmedEmail, password))
            }
            val responseBody = httpResponse.bodyAsText()
            if (!httpResponse.status.isSuccess()) {
                return AuthResult.Failure(responseBody.toSupabaseAuthErrorMessage())
            }

            val response = AuthJson.decodeFromString<AuthTokenResponse>(responseBody)

            val session = response.toSession()
            sessionStore.saveSession(session)
            AuthResult.Success(session)
        }.getOrElse { error ->
            AuthResult.Failure(error.toAuthMessage())
        }
    }

    suspend fun signOut() {
        val accessToken = session.value?.accessToken
        if (config.isConfigured && accessToken != null) {
            runCatching {
                httpClient.post("${config.normalizedUrl}/auth/v1/logout") {
                    header("apikey", config.anonKey)
                    bearerAuth(accessToken)
                }
            }
        }
        sessionStore.clearSession()
    }

    suspend fun hasLocalUserData(): Boolean {
        return database.excerptDao().countExcerptsByUserId(LOCAL_USER_ID) > 0 ||
            database.nodeDao().countNodesByUserId(LOCAL_USER_ID) > 0 ||
            database.tagDao().countTagsByUserId(LOCAL_USER_ID) > 0 ||
            database.excerptTagDao().countExcerptTagsByUserId(LOCAL_USER_ID) > 0
    }

    suspend fun mergeLocalDataIntoCurrentAccount(): Boolean {
        val currentSession = session.value ?: return false
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        database.withTransaction {
            database.nodeDao().updateNodes(
                database.nodeDao().getAllNodesOnce()
                    .filter { it.userId == LOCAL_USER_ID }
                    .map { node ->
                        node.copy(
                            userId = currentSession.userId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(node.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
            )
            database.tagDao().updateTags(
                database.tagDao().getAllTagsOnce()
                    .filter { it.userId == LOCAL_USER_ID }
                    .map { tag ->
                        tag.copy(
                            userId = currentSession.userId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(tag.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
            )
            database.excerptDao().updateExcerpts(
                database.excerptDao().getAllExcerptsOnce()
                    .filter { it.userId == LOCAL_USER_ID }
                    .map { excerpt ->
                        excerpt.copy(
                            userId = currentSession.userId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(excerpt.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
            )
            database.excerptTagDao().updateExcerptTags(
                database.excerptTagDao().getAllExcerptTagsOnce()
                    .filter { it.userId == LOCAL_USER_ID }
                    .map { relation ->
                        relation.copy(
                            userId = currentSession.userId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(relation.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
            )
        }
        return true
    }

    suspend fun clearLocalWorkspaceData() {
        database.withTransaction {
            database.excerptTagDao().deleteAllExcerptTags()
            database.excerptDao().deleteAllExcerpts()
            database.tagDao().deleteAllTags()
            database.nodeDao().deleteAllNodes()
        }
    }

    private fun Throwable.toAuthMessage(): String {
        return when (this) {
            is ClientRequestException -> "邮箱或密码不正确"
            else -> message ?: "登录失败"
        }
    }

}

internal val AuthJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
private data class EmailPasswordSignInRequest(
    val email: String,
    val password: String,
)

@Serializable
internal data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long? = null,
    val user: AuthUserResponse,
)

@Serializable
internal data class AuthUserResponse(
    val id: String,
    val email: String? = null,
)

@Serializable
private data class SupabaseAuthErrorResponse(
    val message: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
) {
    val displayMessage: String?
        get() = message ?: errorDescription ?: error
}

internal fun AuthTokenResponse.toSession(
    fallbackRefreshToken: String? = null,
    fallbackEmail: String? = null,
    nowMillis: Long = System.currentTimeMillis(),
): AuthSession {
    val expiresAtMillis = expiresInSeconds?.let { nowMillis + it * 1_000L }
    return AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken ?: fallbackRefreshToken,
        userId = user.id,
        email = user.email ?: fallbackEmail,
        expiresAtMillis = expiresAtMillis,
    )
}

internal fun String.toSupabaseAuthErrorMessage(): String {
    val remoteMessage = runCatching {
        AuthJson.decodeFromString<SupabaseAuthErrorResponse>(this).displayMessage
    }.getOrNull()
    return when {
        remoteMessage.isNullOrBlank() -> "登录失败，请检查邮箱和密码"
        remoteMessage.contains("Invalid login credentials", ignoreCase = true) -> "邮箱或密码不正确"
        remoteMessage.contains("Email not confirmed", ignoreCase = true) -> "邮箱尚未确认，请先完成邮箱验证"
        remoteMessage.contains("JWT expired", ignoreCase = true) -> "登录已过期，请重新登录"
        else -> remoteMessage
    }
}
