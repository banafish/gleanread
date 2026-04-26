package com.gleanread.android.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.remote.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
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
class SupabaseSessionRefresherTest {
    private lateinit var context: Context
    private lateinit var sessionStore: SupabaseSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionStore = SupabaseSessionStore(context)
        sessionStore.clearSession()
    }

    @After
    fun tearDown() {
        sessionStore.clearSession()
    }

    @Test
    fun `currentSessionOrRefresh refreshes expired access token and stores rotated tokens`() = runBlocking {
        var requestedUrl = ""
        var requestBody = ""
        val httpClient = HttpClient(
            MockEngine { request ->
                requestedUrl = request.url.toString()
                requestBody = (request.body as TextContent).text
                respond(
                    content = """
                        {
                          "access_token": "new-access-token",
                          "refresh_token": "new-refresh-token",
                          "expires_in": 3600,
                          "user": {
                            "id": "user-1",
                            "email": "user@example.com"
                          }
                        }
                    """.trimIndent(),
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
        sessionStore.saveSession(
            AuthSession(
                accessToken = "expired-access-token",
                refreshToken = "old-refresh-token",
                userId = "user-1",
                email = "user@example.com",
                expiresAtMillis = 1_000L,
            ),
        )
        val refresher = SupabaseSessionRefresher(
            config = SupabaseConfig(
                url = "https://example.supabase.co",
                anonKey = "anon-key",
            ),
            httpClient = httpClient,
            sessionStore = sessionStore,
            nowMillis = { 2_000L },
        )

        val session = refresher.currentSessionOrRefresh()
        val saved = sessionStore.session.value

        assertEquals("https://example.supabase.co/auth/v1/token?grant_type=refresh_token", requestedUrl)
        assertTrue(requestBody.contains("old-refresh-token"))
        assertEquals("new-access-token", session?.accessToken)
        assertEquals("new-refresh-token", saved?.refreshToken)
        assertEquals(3_602_000L, saved?.expiresAtMillis)
        httpClient.close()
    }
}
