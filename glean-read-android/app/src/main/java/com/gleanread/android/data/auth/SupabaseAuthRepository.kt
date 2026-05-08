package com.gleanread.android.data.auth

import android.net.Uri
import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.remote.SupabaseConfig
import com.gleanread.android.data.sync.DeviceIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val EMAIL_ALREADY_REGISTERED_MESSAGE = "邮箱已注册，请直接登录"
private const val SIGN_UP_PENDING_VERIFICATION_MESSAGE = "注册成功，请查收验证邮件以完成注册"

class SupabaseAuthRepository private constructor(
    internal val config: SupabaseConfig,
    private val httpClient: HttpClient,
    private val sessionStore: SupabaseSessionStore,
    private val databaseProvider: () -> WorkspaceDatabase,
    private val guestDatabaseProvider: () -> WorkspaceDatabase,
    private val switchToUser: (String) -> Unit,
    private val closeCurrentDatabase: () -> Unit,
    private val clearGuestDataAction: suspend () -> Unit,
    private val deviceIdProvider: DeviceIdProvider,
) {
    private val database: WorkspaceDatabase
        get() = databaseProvider()

    private val _pendingLocalDataOwnership = MutableStateFlow(false)
    val pendingLocalDataOwnership: StateFlow<Boolean> = _pendingLocalDataOwnership.asStateFlow()

    constructor(
        config: SupabaseConfig,
        httpClient: HttpClient,
        sessionStore: SupabaseSessionStore,
        databaseManager: WorkspaceDatabaseManager,
        deviceIdProvider: DeviceIdProvider,
    ) : this(
        config = config,
        httpClient = httpClient,
        sessionStore = sessionStore,
        databaseProvider = { databaseManager.activeWorkspace.value.database },
        guestDatabaseProvider = { databaseManager.guestDatabase },
        switchToUser = databaseManager::switchToUser,
        closeCurrentDatabase = databaseManager::closeCurrentDatabase,
        clearGuestDataAction = { databaseManager.clearGuestData() },
        deviceIdProvider = deviceIdProvider,
    )

    internal constructor(
        config: SupabaseConfig,
        httpClient: HttpClient,
        sessionStore: SupabaseSessionStore,
        database: WorkspaceDatabase,
        deviceIdProvider: DeviceIdProvider,
    ) : this(
        config = config,
        httpClient = httpClient,
        sessionStore = sessionStore,
        databaseProvider = { database },
        guestDatabaseProvider = { database },
        switchToUser = {},
        closeCurrentDatabase = {},
        clearGuestDataAction = {},
        deviceIdProvider = deviceIdProvider,
    )

    val session: StateFlow<AuthSession?> = sessionStore.session

    fun requestLocalDataOwnership() {
        _pendingLocalDataOwnership.value = true
    }

    fun clearLocalDataOwnershipRequest() {
        _pendingLocalDataOwnership.value = false
    }

    suspend fun applyLocalDataOwnershipChoice(choice: LocalDataOwnershipChoice): Boolean {
        return when (choice) {
            LocalDataOwnershipChoice.MERGE_TO_ACCOUNT -> mergeLocalDataIntoCurrentAccount()
            LocalDataOwnershipChoice.KEEP_LOCAL -> true
            LocalDataOwnershipChoice.USE_CLOUD -> {
                clearLocalWorkspaceData()
                true
            }
        }
    }

    suspend fun sendMagicLink(email: String): MagicLinkRequestResult {
        return signInWithOtp(email, config.magicLinkRedirectUrl)
    }

    suspend fun signInWithOtp(email: String, redirectTo: String? = null): MagicLinkRequestResult {
        if (!config.isConfigured) {
            return MagicLinkRequestResult.Failure("Supabase 尚未配置")
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            return MagicLinkRequestResult.Failure("请输入邮箱")
        }

        val actionName = if (redirectTo != null) "Magic Link" else "验证码"

        return runCatching {
            val httpResponse = httpClient.post("${config.normalizedUrl}/auth/v1/otp") {
                if (redirectTo != null) {
                    parameter("redirect_to", redirectTo)
                }
                header("apikey", config.anonKey)
                contentType(ContentType.Application.Json)
                setBody(MagicLinkRequest(email = trimmedEmail))
            }
            val responseBody = httpResponse.bodyAsText()
            if (!httpResponse.status.isSuccess()) {
                return MagicLinkRequestResult.Failure(
                    responseBody.toSupabaseAuthErrorMessage(defaultMessage = "发送${actionName}失败"),
                )
            }

            MagicLinkRequestResult.Sent
        }.getOrElse { error ->
            MagicLinkRequestResult.Failure(error.message ?: "发送${actionName}失败")
        }
    }

    suspend fun verifyOtp(email: String, token: String, type: String = "email"): AuthResult {
        if (!config.isConfigured) {
            return AuthResult.Failure("Supabase 尚未配置")
        }
        val trimmedEmail = email.trim()
        val trimmedToken = token.trim()
        if (trimmedEmail.isEmpty() || trimmedToken.isEmpty()) {
            return AuthResult.Failure("验证码不能为空")
        }

        return runCatching {
            val httpResponse = httpClient.post("${config.normalizedUrl}/auth/v1/verify") {
                header("apikey", config.anonKey)
                contentType(ContentType.Application.Json)
                setBody(VerifyOtpRequest(email = trimmedEmail, token = trimmedToken, type = type))
            }
            val responseBody = httpResponse.bodyAsText()
            if (!httpResponse.status.isSuccess()) {
                return AuthResult.Failure(responseBody.toSupabaseAuthErrorMessage(defaultMessage = "验证码无效或已过期"))
            }

            val response = AuthJson.decodeFromString<AuthTokenResponse>(responseBody)
            val session = response.toSession()
            sessionStore.saveSession(session)
            switchToUser(session.userId)
            AuthResult.Success(session)
        }.getOrElse { error ->
            AuthResult.Failure(error.message ?: "验证失败")
        }
    }

    suspend fun completeMagicLinkSignIn(uri: Uri?): AuthResult {
        if (!config.isConfigured) {
            return AuthResult.Failure("Supabase 尚未配置")
        }
        if (uri == null || !isMagicLinkRedirect(uri)) {
            return AuthResult.Failure("Magic Link 回调地址无效")
        }

        val parameters = uri.authCallbackParameters()
        parameters["error_description"]?.takeIf(String::isNotBlank)?.let { error ->
            return AuthResult.Failure(error)
        }
        parameters["error"]?.takeIf(String::isNotBlank)?.let { error ->
            return AuthResult.Failure(error)
        }

        val accessToken = parameters["access_token"].orEmpty()
        if (accessToken.isBlank()) {
            return AuthResult.Failure("Magic Link 缺少登录凭据")
        }

        return runCatching {
            val user = fetchCurrentUser(accessToken)
            val session = AuthSession(
                accessToken = accessToken,
                refreshToken = parameters["refresh_token"],
                userId = user.id,
                email = user.email,
                expiresAtMillis = parameters.authExpiresAtMillis(),
            )
            sessionStore.saveSession(session)
            switchToUser(session.userId)
            AuthResult.Success(session)
        }.getOrElse { error ->
            AuthResult.Failure(error.message ?: "Magic Link 登录失败")
        }
    }

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
            switchToUser(session.userId)
            AuthResult.Success(session)
        }.getOrElse { error ->
            AuthResult.Failure(error.toAuthMessage())
        }
    }

    suspend fun signUpWithEmailPassword(email: String, password: String): AuthResult {
        if (!config.isConfigured) {
            return AuthResult.Failure("Supabase 尚未配置")
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            return AuthResult.Failure("请输入邮箱和密码")
        }

        return runCatching {
            val httpResponse = httpClient.post("${config.normalizedUrl}/auth/v1/signup") {
                header("apikey", config.anonKey)
                contentType(ContentType.Application.Json)
                setBody(EmailPasswordSignInRequest(trimmedEmail, password))
            }
            val responseBody = httpResponse.bodyAsText()
            if (!httpResponse.status.isSuccess()) {
                return AuthResult.Failure(responseBody.toSupabaseAuthErrorMessage(defaultMessage = "注册失败，请检查邮箱格式或密码要求"))
            }

            val response = AuthJson.decodeFromString<SignUpResponse>(responseBody)

            if (response.identities?.isEmpty() == true) {
                return AuthResult.Failure(EMAIL_ALREADY_REGISTERED_MESSAGE)
            }

            if (response.accessToken.isNullOrBlank()) {
                return AuthResult.Failure(SIGN_UP_PENDING_VERIFICATION_MESSAGE)
            }

            val user = AuthUserResponse(id = response.id ?: "", email = response.email)
            val session = AuthTokenResponse(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresInSeconds = response.expiresInSeconds,
                user = user
            ).toSession()

            sessionStore.saveSession(session)
            switchToUser(session.userId)
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
        closeCurrentDatabase()
    }

    suspend fun updateUserMetadata(metadata: Map<String, JsonElement>): Result<Unit> {
        val token = session.value?.accessToken ?: return Result.failure(Exception("尚未登录"))
        if (!config.isConfigured) return Result.failure(Exception("Supabase 尚未配置"))

        return runCatching {
            val response = httpClient.put("${config.normalizedUrl}/auth/v1/user") {
                header("apikey", config.anonKey)
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(JsonObject(mapOf("data" to JsonObject(metadata))))
            }
            if (!response.status.isSuccess()) {
                throw Exception("更新用户元数据失败: ${response.status} - ${response.bodyAsText()}")
            }
        }
    }

    internal suspend fun fetchCurrentUserProfile(): Result<AuthUserResponse> {
        val token = session.value?.accessToken ?: return Result.failure(Exception("尚未登录"))
        if (!config.isConfigured) return Result.failure(Exception("Supabase 尚未配置"))

        return runCatching {
            fetchCurrentUser(token)
        }
    }

    /**
     * 检查访客数据库是否有业务数据。
     */
    suspend fun hasLocalUserData(): Boolean {
        val guestDb = guestDatabaseProvider()
        return guestDb.excerptDao().countExcerpts() > 0 ||
            guestDb.nodeDao().countNodes() > 0 ||
            guestDb.tagDao().countTags() > 0 ||
            guestDb.excerptTagDao().countExcerptTagsByUserId(LOCAL_USER_ID) > 0
    }

    /**
     * 将访客数据库中的数据迁移到当前登录用户的数据库。
     * 读取 guest.db 中所有记录，重新绑定 userId 后批量写入当前用户的数据库。
     */
    suspend fun mergeLocalDataIntoCurrentAccount(): Boolean {
        val currentSession = session.value ?: return false
        val guestDb = guestDatabaseProvider()
        val userDb = databaseProvider()

        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val targetUserId = currentSession.userId

        // 从 guest.db 读取所有数据
        val nodes = guestDb.nodeDao().getAllNodesOnce()
            .filter { it.userId == LOCAL_USER_ID }
        val tags = guestDb.tagDao().getAllTagsOnce()
            .filter { it.userId == LOCAL_USER_ID }
        val excerpts = guestDb.excerptDao().getAllExcerptsOnce()
            .filter { it.userId == LOCAL_USER_ID }
        val excerptTags = guestDb.excerptTagDao().getAllExcerptTagsOnce()
            .filter { it.userId == LOCAL_USER_ID }

        if (nodes.isEmpty() && tags.isEmpty() && excerpts.isEmpty() && excerptTags.isEmpty()) {
            return true
        }

        // 批量写入用户数据库
        userDb.withTransaction {
            if (nodes.isNotEmpty()) {
                userDb.nodeDao().insertNodes(
                    nodes.map { node ->
                        node.copy(
                            userId = targetUserId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(node.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
                )
            }
            if (tags.isNotEmpty()) {
                userDb.tagDao().insertTags(
                    tags.map { tag ->
                        tag.copy(
                            userId = targetUserId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(tag.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
                )
            }
            if (excerpts.isNotEmpty()) {
                userDb.excerptDao().insertExcerpts(
                    excerpts.map { excerpt ->
                        excerpt.copy(
                            userId = targetUserId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(excerpt.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
                )
            }
            if (excerptTags.isNotEmpty()) {
                userDb.excerptTagDao().insertExcerptTags(
                    excerptTags.map { relation ->
                        relation.copy(
                            userId = targetUserId,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(relation.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    },
                )
            }
        }

        // 迁移完成后清空 guest.db
        clearGuestDataAction()
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

    /**
     * 检查当前用户数据库中是否有未同步的修改记录。
     */
    suspend fun hasUnsyncedChanges(): Boolean {
        val pendingStatuses = listOf(
            SyncStatus.PENDING_CREATE.name,
            SyncStatus.PENDING_UPDATE.name,
            SyncStatus.PENDING_DELETE.name,
            SyncStatus.SYNCING.name,
            SyncStatus.FAILED.name,
        )
        return database.nodeDao().findNodesBySyncStatuses(pendingStatuses).isNotEmpty() ||
            database.tagDao().findTagsBySyncStatuses(pendingStatuses).isNotEmpty() ||
            database.excerptDao().findExcerptsBySyncStatuses(pendingStatuses).isNotEmpty() ||
            database.excerptTagDao().findExcerptTagsBySyncStatuses(pendingStatuses).isNotEmpty()
    }

    private suspend fun fetchCurrentUser(accessToken: String): AuthUserResponse {
        val httpResponse = httpClient.get("${config.normalizedUrl}/auth/v1/user") {
            header("apikey", config.anonKey)
            bearerAuth(accessToken)
        }
        val responseBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            throw SupabaseAuthException(
                responseBody.toSupabaseAuthErrorMessage(defaultMessage = "Magic Link 登录失败"),
            )
        }
        return AuthJson.decodeFromString<AuthUserResponse>(responseBody)
    }

    private fun Throwable.toAuthMessage(): String {
        return when (this) {
            is ClientRequestException -> "邮箱或密码不正确"
            else -> message ?: "登录失败"
        }
    }

    companion object {
        fun isMagicLinkRedirect(uri: Uri): Boolean {
            return uri.scheme == MAGIC_LINK_SCHEME &&
                uri.host == MAGIC_LINK_HOST &&
                uri.path == MAGIC_LINK_PATH
        }

        private const val MAGIC_LINK_SCHEME = "gleanread"
        private const val MAGIC_LINK_HOST = "auth"
        private const val MAGIC_LINK_PATH = "/callback"
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
private data class VerifyOtpRequest(
    val email: String,
    val token: String,
    val type: String,
)

@Serializable
private data class MagicLinkRequest(
    val email: String,
    @SerialName("create_user") val createUser: Boolean = true,
)

@Serializable
internal data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long? = null,
    val user: AuthUserResponse,
)

@Serializable
internal data class SignUpResponse(
    val id: String? = null,
    val email: String? = null,
    val identities: List<JsonObject>? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long? = null,
)

@Serializable
internal data class AuthUserResponse(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: JsonObject? = null,
)

@Serializable
private data class SupabaseAuthErrorResponse(
    val code: JsonElement? = null,
    val msg: String? = null,
    val message: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
) {
    val displayMessage: String?
        get() = message ?: msg ?: errorDescription ?: error

    val codeText: String?
        get() = code?.jsonPrimitive?.toString()?.trim('"')
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

internal fun String.toSupabaseAuthErrorMessage(defaultMessage: String = "登录失败，请检查邮箱和密码"): String {
    val errorResponse = runCatching {
        AuthJson.decodeFromString<SupabaseAuthErrorResponse>(this)
    }.getOrNull()
    val remoteMessage = errorResponse?.displayMessage
    return when {
        errorResponse?.codeText?.isEmailAlreadyRegisteredMessage() == true -> EMAIL_ALREADY_REGISTERED_MESSAGE
        remoteMessage.isNullOrBlank() -> defaultMessage
        remoteMessage.isEmailAlreadyRegisteredMessage() -> EMAIL_ALREADY_REGISTERED_MESSAGE
        remoteMessage.contains("Invalid login credentials", ignoreCase = true) -> "邮箱或密码不正确"
        remoteMessage.contains("Email not confirmed", ignoreCase = true) -> "邮箱尚未确认，请先完成邮箱验证"
        remoteMessage.contains("JWT expired", ignoreCase = true) -> "登录已过期，请重新登录"
        else -> remoteMessage
    }
}

private fun String.isEmailAlreadyRegisteredMessage(): Boolean {
    return contains("User already registered", ignoreCase = true) ||
        contains("already registered", ignoreCase = true) ||
        contains("already been registered", ignoreCase = true) ||
        contains("user_already_exists", ignoreCase = true) ||
        contains("email_exists", ignoreCase = true)
}

private fun Uri.authCallbackParameters(): Map<String, String> {
    return sequenceOf(encodedQuery, encodedFragment)
        .filterNotNull()
        .flatMap { encodedPart -> encodedPart.split("&").asSequence() }
        .mapNotNull { pair ->
            val separatorIndex = pair.indexOf('=')
            if (separatorIndex <= 0) {
                null
            } else {
                val key = pair.substring(0, separatorIndex).urlDecode()
                val value = pair.substring(separatorIndex + 1).urlDecode()
                key to value
            }
        }
        .toMap()
}

private fun Map<String, String>.authExpiresAtMillis(
    nowMillis: Long = System.currentTimeMillis(),
): Long? {
    val expiresInMillis = this["expires_in"]?.toLongOrNull()?.let { nowMillis + it * 1_000L }
    val expiresAtMillis = this["expires_at"]?.toLongOrNull()?.let { it * 1_000L }
    return expiresAtMillis ?: expiresInMillis
}

private fun String.urlDecode(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.name())
}

private class SupabaseAuthException(message: String) : RuntimeException(message)
