package com.gleanread.android.data.sync

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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseWorkspaceRemoteDataSourceTest {
    @Test
    fun `upsert keeps null keys so postgrest batch objects have matching keys`() = runBlocking {
        var requestBody = ""
        val httpClient = HttpClient(
            MockEngine { request ->
                requestBody = (request.body as TextContent).text
                respond(
                    content = "",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )
        val dataSource = SupabaseWorkspaceRemoteDataSource(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = httpClient,
        )

        dataSource.upsertExcerpts(
            accessToken = "token",
            excerpts = listOf(
                RemoteExcerpt(
                    id = "excerpt-null",
                    userId = "user-1",
                    content = "空字段摘录",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 100L,
                    updateTime = 100L,
                    isDeleted = false,
                    deviceId = null,
                ),
                RemoteExcerpt(
                    id = "excerpt-filled",
                    userId = "user-1",
                    content = "有字段摘录",
                    url = "https://example.com",
                    sourceTitle = "Example",
                    userThought = "note",
                    treeNodeId = "node-1",
                    createTime = 200L,
                    updateTime = 200L,
                    isDeleted = false,
                    deviceId = "device-1",
                ),
            ),
        )

        val payload = Json.parseToJsonElement(requestBody).jsonArray
        val first = payload[0].jsonObject
        val second = payload[1].jsonObject

        assertEquals(first.keys, second.keys)
        assertEquals(JsonNull, first.getValue("source_title"))
        assertEquals(JsonNull, first.getValue("tree_node_id"))
        assertEquals(JsonNull, first.getValue("device_id"))
        httpClient.close()
    }

    @Test
    fun `upsert throws readable error when postgrest returns non success status`() = runBlocking {
        val httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"code":"PGRST204","message":"Could not find the 'content' column"}""",
                    status = HttpStatusCode.BadRequest,
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
        val dataSource = SupabaseWorkspaceRemoteDataSource(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = httpClient,
        )

        val error = runCatching {
            dataSource.upsertExcerpts(
                accessToken = "token",
                excerpts = listOf(
                    RemoteExcerpt(
                        id = "excerpt-1",
                        userId = "user-1",
                        content = "正文",
                        url = null,
                        sourceTitle = null,
                        userThought = null,
                        treeNodeId = null,
                        createTime = 100L,
                        updateTime = 100L,
                        isDeleted = false,
                        deviceId = "device-1",
                    ),
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is SupabaseRemoteException)
        assertTrue(error?.message.orEmpty().contains("excerpts"))
        assertTrue(error?.message.orEmpty().contains("content"))
        httpClient.close()
    }
}
