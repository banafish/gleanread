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
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseWorkspaceRemoteDataSourceTest {
    @Test
    fun `upsert posts rpc payload with p rows and matching null keys`() = runBlocking {
        val requestBodies = mutableListOf<String>()
        val requestPaths = mutableListOf<String>()
        val httpClient = jsonClient(
            MockEngine { request ->
                val body = (request.body as TextContent).text
                requestBodies += body
                requestPaths += request.url.encodedPath
                respond(
                    content = appliedResponseFor(body),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
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
                remoteExcerpt(
                    id = "excerpt-null",
                    content = "empty optional fields",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    deviceId = null,
                ),
                remoteExcerpt(
                    id = "excerpt-filled",
                    content = "filled optional fields",
                    url = "https://example.com",
                    sourceTitle = "Example",
                    userThought = "note",
                    treeNodeId = "node-1",
                    deviceId = "device-1",
                ),
            ),
        )

        val rows = rpcRows(requestBodies.single())
        val first = rows[0].jsonObject
        val second = rows[1].jsonObject

        assertEquals("/rest/v1/rpc/sync_excerpts_conditional", requestPaths.single())
        assertEquals(first.keys, second.keys)
        assertEquals(JsonNull, first.getValue("source_title"))
        assertEquals(JsonNull, first.getValue("tree_node_id"))
        assertEquals(JsonNull, first.getValue("device_id"))
        httpClient.close()
    }

    @Test
    fun `upsert splits rpc requests into fixed size chunks`() = runBlocking {
        val requestBatchSizes = mutableListOf<Int>()
        val requestPaths = mutableListOf<String>()
        val httpClient = jsonClient(
            MockEngine { request ->
                val body = (request.body as TextContent).text
                requestBatchSizes += rpcRows(body).size
                requestPaths += request.url.encodedPath
                respond(
                    content = appliedResponseFor(body),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
            },
        )
        val dataSource = SupabaseWorkspaceRemoteDataSource(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = httpClient,
        )

        val result = dataSource.upsertExcerpts(
            accessToken = "token",
            excerpts = (1..51).map { index ->
                remoteExcerpt(id = "excerpt-$index", updateTime = index.toLong())
            },
        )

        assertEquals(51, result.size)
        assertEquals(listOf(50, 1), requestBatchSizes)
        assertTrue(requestPaths.all { it == "/rest/v1/rpc/sync_excerpts_conditional" })
        httpClient.close()
    }

    @Test
    fun `upsert throws readable error when rpc returns non success status`() = runBlocking {
        val httpClient = jsonClient(
            MockEngine {
                respond(
                    content = """{"code":"PGRST204","message":"Could not find the 'content' column"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = jsonHeaders(),
                )
            },
        )
        val dataSource = SupabaseWorkspaceRemoteDataSource(
            config = SupabaseConfig(url = "https://example.supabase.co", anonKey = "anon-key"),
            httpClient = httpClient,
        )

        val error = runCatching {
            dataSource.upsertExcerpts(
                accessToken = "token",
                excerpts = listOf(remoteExcerpt(id = "excerpt-1")),
            )
        }.exceptionOrNull()

        assertTrue(error is SupabaseRemoteException)
        assertTrue(error?.message.orEmpty().contains("sync_excerpts_conditional"))
        assertTrue(error?.message.orEmpty().contains("content"))
        httpClient.close()
    }
}

private val SupabaseRemoteTestJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun jsonClient(engine: MockEngine): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(SupabaseRemoteTestJson)
        }
    }
}

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun rpcRows(body: String) = Json.parseToJsonElement(body).jsonObject.getValue("p_rows").jsonArray

private fun appliedResponseFor(body: String): String {
    return rpcRows(body).joinToString(prefix = "[", postfix = "]") { row ->
        val id = row.jsonObject.getValue("id").jsonPrimitive.content
        """{"id":"$id","status":"applied"}"""
    }
}

private fun remoteExcerpt(
    id: String,
    content: String = "content",
    url: String? = null,
    sourceTitle: String? = null,
    userThought: String? = null,
    treeNodeId: String? = null,
    deviceId: String? = "device-1",
    updateTime: Long = 100L,
): RemoteExcerpt {
    return RemoteExcerpt(
        id = id,
        userId = "user-1",
        content = content,
        url = url,
        sourceTitle = sourceTitle,
        userThought = userThought,
        treeNodeId = treeNodeId,
        createTime = 100L,
        updateTime = updateTime,
        isDeleted = false,
        deviceId = deviceId,
    )
}
