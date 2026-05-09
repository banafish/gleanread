package com.gleanread.android.data.auth

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.local.ActiveWorkspace
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.remote.SupabaseConfig
import com.gleanread.android.data.remote.SupabaseHttpClientFactory
import com.gleanread.android.data.sync.DeviceIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupabaseAuthRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: WorkspaceDatabase
    private lateinit var sessionStore: SupabaseSessionStore
    private lateinit var httpClient: HttpClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        sessionStore = SupabaseSessionStore(context)
        sessionStore.clearSession()
        sessionStore.saveSession(
            AuthSession(
                accessToken = "token",
                refreshToken = null,
                userId = "cloud-user",
                email = "user@example.com",
                expiresAtMillis = null,
            ),
        )
        httpClient = SupabaseHttpClientFactory.create()
    }

    @After
    fun tearDown() {
        database.close()
        sessionStore.clearSession()
        httpClient.close()
    }

    @Test
    fun `mergeLocalDataIntoCurrentAccount keeps ids and marks records dirty`() = runBlocking {
        database.tagDao().insertTag(
            TagEntity(
                id = "tag-1",
                userId = LOCAL_USER_ID,
                tagName = "sync",
                colorIcon = null,
                heatWeight = 1,
                createTime = 100L,
                updateTime = 100L,
                syncStatus = SyncStatus.SYNCED,
            ),
        )

        val result = repository().mergeLocalDataIntoCurrentAccount()
        val saved = database.tagDao().getAllTagsOnce().single()

        assertTrue(result)
        assertEquals("tag-1", saved.id)
        assertEquals("cloud-user", saved.userId)
        assertEquals("test-device", saved.deviceId)
        assertEquals(SyncStatus.PENDING_UPDATE, saved.syncStatus)
        assertEquals(null, saved.syncError)
        assertTrue(saved.localDirtyTime != null)
    }

    @Test
    fun `apply ownership merge copies guest data into active user database and clears guest`() = runBlocking {
        val guestDatabase = Room.inMemoryDatabaseBuilder(
            context,
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        val userDatabase = Room.inMemoryDatabaseBuilder(
            context,
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            guestDatabase.tagDao().insertTag(
                TagEntity(
                    id = "guest-tag",
                    userId = LOCAL_USER_ID,
                    tagName = "guest",
                    colorIcon = null,
                    heatWeight = 1,
                    createTime = 100L,
                    updateTime = 100L,
                    syncStatus = SyncStatus.SYNCED,
                ),
            )
            val repository = SupabaseAuthRepository(
                config = SupabaseConfig(url = "", anonKey = ""),
                httpClient = httpClient,
                sessionStore = sessionStore,
                activeWorkspaceProvider = {
                    ActiveWorkspace.user(
                        userId = "cloud-user",
                        databaseName = "user.db",
                        database = userDatabase,
                    )
                },
                guestDatabaseProvider = { guestDatabase },
                clearGuestDataAction = { guestDatabase.tagDao().deleteAllTags() },
                deviceIdProvider = DeviceIdProvider { "test-device" },
            )

            val result = repository.applyLocalDataOwnershipChoice(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT)
            val saved = userDatabase.tagDao().getAllTagsOnce().single()

            assertEquals(LocalDataOwnershipResult.Applied, result)
            assertEquals("guest-tag", saved.id)
            assertEquals("cloud-user", saved.userId)
            assertEquals(SyncStatus.PENDING_UPDATE, saved.syncStatus)
            assertEquals(0, guestDatabase.tagDao().getAllTagsOnce().size)
        } finally {
            guestDatabase.close()
            userDatabase.close()
        }
    }

    @Test
    fun `signInWithEmailPassword maps supabase auth error response`() = runBlocking {
        sessionStore.clearSession()
        val authClient = mockAuthClient(
            status = HttpStatusCode.BadRequest,
            body = """{"code":"invalid_credentials","message":"Invalid login credentials"}""",
        )

        val result = repository(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = authClient,
        ).signInWithEmailPassword("user@example.com", "wrong-password")

        assertEquals(AuthResult.Failure("邮箱或密码不正确"), result)
        authClient.close()
    }

    @Test
    fun `signUpWithEmailPassword maps duplicate email auth error response`() = runBlocking {
        sessionStore.clearSession()
        val authClient = mockAuthClient(
            status = HttpStatusCode.BadRequest,
            body = """{"code":400,"msg":"User already registered"}""",
        )

        val result = repository(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = authClient,
        ).signUpWithEmailPassword("user@example.com", "password")

        assertEquals(AuthResult.Failure("邮箱已注册，请直接登录"), result)
        authClient.close()
    }

    @Test
    fun `signUpWithEmailPassword maps obfuscated duplicate user response`() = runBlocking {
        sessionStore.clearSession()
        val authClient = mockAuthClient(
            status = HttpStatusCode.OK,
            body = """{"id":"fake-user","email":"user@example.com","identities":[]}""",
        )

        val result = repository(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = authClient,
        ).signUpWithEmailPassword("user@example.com", "password")

        assertEquals(AuthResult.Failure("邮箱已注册，请直接登录"), result)
        assertEquals(null, sessionStore.session.value)
        authClient.close()
    }

    @Test
    fun `signUpWithEmailPassword keeps verification prompt for new unconfirmed signup`() = runBlocking {
        sessionStore.clearSession()
        val authClient = mockAuthClient(
            status = HttpStatusCode.OK,
            body = """
                {
                  "id":"new-user",
                  "email":"new@example.com",
                  "identities":[{"id":"identity-1","user_id":"new-user","provider":"email"}]
                }
            """.trimIndent(),
        )

        val result = repository(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = authClient,
        ).signUpWithEmailPassword("new@example.com", "password")

        assertEquals(AuthResult.Failure("注册成功，请查收验证邮件以完成注册"), result)
        authClient.close()
    }

    @Test
    fun `local data ownership request can be marked and cleared`() {
        val repository = repository()

        assertEquals(false, repository.pendingLocalDataOwnership.value)

        repository.requestLocalDataOwnership()
        assertEquals(true, repository.pendingLocalDataOwnership.value)

        repository.clearLocalDataOwnershipRequest()
        assertEquals(false, repository.pendingLocalDataOwnership.value)
    }

    @Test
    fun `hasUnsyncedChanges treats syncing and failed records as protected`() = runBlocking {
        database.tagDao().insertTags(
            listOf(
                TagEntity(
                    id = "tag-syncing",
                    userId = "cloud-user",
                    tagName = "syncing",
                    colorIcon = null,
                    heatWeight = 1,
                    createTime = 100L,
                    updateTime = 100L,
                    syncStatus = SyncStatus.SYNCING,
                ),
                TagEntity(
                    id = "tag-failed",
                    userId = "cloud-user",
                    tagName = "failed",
                    colorIcon = null,
                    heatWeight = 1,
                    createTime = 100L,
                    updateTime = 100L,
                    syncStatus = SyncStatus.FAILED,
                ),
            ),
        )

        assertTrue(repository().hasUnsyncedChanges())
    }

    @Test
    fun `sendMagicLink posts otp request with redirect url`() = runBlocking {
        var requestedPath = ""
        var redirectTo = ""
        val authClient = HttpClient(
            MockEngine { request ->
                requestedPath = request.url.encodedPath
                redirectTo = request.url.parameters["redirect_to"].orEmpty()
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }
        }

        val result = repository(
            config = SupabaseConfig(
                url = "https://example.supabase.co",
                anonKey = "anon-key",
                magicLinkRedirectUrl = "gleanread://auth/callback",
            ),
            httpClient = authClient,
        ).sendMagicLink("user@example.com")

        assertEquals(MagicLinkRequestResult.Sent, result)
        assertEquals("/auth/v1/otp", requestedPath)
        assertEquals("gleanread://auth/callback", redirectTo)
        authClient.close()
    }

    @Test
    fun `completeMagicLinkSignIn fetches user and saves session`() = runBlocking {
        sessionStore.clearSession()
        val authClient = mockAuthClient(
            status = HttpStatusCode.OK,
            body = """{"id":"cloud-user","email":"user@example.com"}""",
        )

        val result = repository(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = authClient,
        ).completeMagicLinkSignIn(
            Uri.parse(
                "gleanread://auth/callback#access_token=access-token&refresh_token=refresh-token&expires_in=3600",
            ),
        )

        assertTrue(result is AuthResult.Success)
        val saved = sessionStore.session.value
        assertEquals("access-token", saved?.accessToken)
        assertEquals("refresh-token", saved?.refreshToken)
        assertEquals("cloud-user", saved?.userId)
        assertEquals("user@example.com", saved?.email)
        authClient.close()
    }

    private fun repository(
        config: SupabaseConfig = SupabaseConfig(url = "", anonKey = ""),
        httpClient: HttpClient = this.httpClient,
    ): SupabaseAuthRepository {
        return SupabaseAuthRepository(
            config = config,
            httpClient = httpClient,
            sessionStore = sessionStore,
            database = database,
            deviceIdProvider = DeviceIdProvider { "test-device" },
        )
    }

    private fun mockAuthClient(status: HttpStatusCode, body: String): HttpClient {
        return HttpClient(
            MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }
        }
    }
}
