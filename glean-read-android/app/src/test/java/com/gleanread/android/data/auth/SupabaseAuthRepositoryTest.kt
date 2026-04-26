package com.gleanread.android.data.auth

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
