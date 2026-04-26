package com.gleanread.android.data.sync

import com.gleanread.android.data.remote.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val REMOTE_TABLE_KNOWLEDGE_TREE_NODE = "knowledge_tree_node"
internal const val REMOTE_TABLE_TAGS = "tags"
internal const val REMOTE_TABLE_EXCERPTS = "excerpts"
internal const val REMOTE_TABLE_EXCERPT_TAGS = "excerpt_tags"

class SupabaseWorkspaceRemoteDataSource(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient,
) : WorkspaceRemoteDataSource {
    override suspend fun upsertNodes(
        accessToken: String,
        nodes: List<RemoteKnowledgeTreeNode>,
    ) {
        upsert(REMOTE_TABLE_KNOWLEDGE_TREE_NODE, accessToken, nodes)
    }

    override suspend fun upsertTags(
        accessToken: String,
        tags: List<RemoteTag>,
    ) {
        upsert(REMOTE_TABLE_TAGS, accessToken, tags)
    }

    override suspend fun upsertExcerpts(
        accessToken: String,
        excerpts: List<RemoteExcerpt>,
    ) {
        upsert(REMOTE_TABLE_EXCERPTS, accessToken, excerpts)
    }

    override suspend fun upsertExcerptTags(
        accessToken: String,
        relations: List<RemoteExcerptTag>,
    ) {
        upsert(REMOTE_TABLE_EXCERPT_TAGS, accessToken, relations)
    }

    override suspend fun fetchChanges(
        accessToken: String,
        userId: String,
        updatedAfter: Long?,
    ): RemoteWorkspaceSnapshot {
        return RemoteWorkspaceSnapshot(
            nodes = fetchTable(REMOTE_TABLE_KNOWLEDGE_TREE_NODE, accessToken, userId, updatedAfter),
            tags = fetchTable(REMOTE_TABLE_TAGS, accessToken, userId, updatedAfter),
            excerpts = fetchTable(REMOTE_TABLE_EXCERPTS, accessToken, userId, updatedAfter),
            excerptTags = fetchTable(REMOTE_TABLE_EXCERPT_TAGS, accessToken, userId, updatedAfter),
        )
    }

    private suspend inline fun <reified T> upsert(
        tableName: String,
        accessToken: String,
        items: List<T>,
    ) {
        if (items.isEmpty() || !config.isConfigured) return
        val response = httpClient.post("${config.normalizedUrl}/rest/v1/$tableName") {
            supabaseHeaders(accessToken)
            header(HttpHeaders.Prefer, "resolution=merge-duplicates,return=minimal")
            parameter("on_conflict", "id")
            contentType(ContentType.Application.Json)
            setBody(SupabaseUpsertJson.encodeToString(items))
        }
        response.ensureSuccess(tableName)
    }

    private suspend inline fun <reified T> fetchTable(
        tableName: String,
        accessToken: String,
        userId: String,
        updatedAfter: Long?,
    ): List<T> {
        if (!config.isConfigured) return emptyList()
        val response = httpClient.get("${config.normalizedUrl}/rest/v1/$tableName") {
            supabaseHeaders(accessToken)
            parameter("select", "*")
            parameter("user_id", "eq.$userId")
            updatedAfter?.let { parameter("update_time", "gt.$it") }
            parameter("order", "update_time.asc")
        }
        response.ensureSuccess(tableName)
        return response.body()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.supabaseHeaders(accessToken: String) {
        header("apikey", config.anonKey)
        bearerAuth(accessToken)
    }
}

private val SupabaseRemoteJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private val SupabaseUpsertJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

private suspend fun HttpResponse.ensureSuccess(tableName: String) {
    if (status.isSuccess()) return
    val responseBody = bodyAsText()
    throw SupabaseRemoteException("同步 $tableName 失败：${responseBody.toPostgrestMessage(status.value)}")
}

private fun String.toPostgrestMessage(statusCode: Int): String {
    val remoteMessage = runCatching {
        SupabaseRemoteJson.decodeFromString<PostgrestErrorResponse>(this).displayMessage
    }.getOrNull()
    return remoteMessage?.takeIf(String::isNotBlank)
        ?: "Supabase 返回 HTTP $statusCode"
}

class SupabaseRemoteException(message: String) : RuntimeException(message)

@Serializable
private data class PostgrestErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val details: String? = null,
    val hint: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
) {
    val displayMessage: String?
        get() = listOfNotNull(message, errorDescription, details, hint, code)
            .firstOrNull(String::isNotBlank)
}
