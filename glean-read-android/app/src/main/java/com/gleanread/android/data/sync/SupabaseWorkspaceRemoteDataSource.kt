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
    ): List<ConditionalPushResult> {
        return pushConditional(RPC_SYNC_KNOWLEDGE_TREE_NODE, accessToken, nodes)
    }

    override suspend fun upsertTags(
        accessToken: String,
        tags: List<RemoteTag>,
    ): List<ConditionalPushResult> {
        return pushConditional(RPC_SYNC_TAGS, accessToken, tags)
    }

    override suspend fun upsertExcerpts(
        accessToken: String,
        excerpts: List<RemoteExcerpt>,
    ): List<ConditionalPushResult> {
        return pushConditional(RPC_SYNC_EXCERPTS, accessToken, excerpts)
    }

    override suspend fun upsertExcerptTags(
        accessToken: String,
        relations: List<RemoteExcerptTag>,
    ): List<ConditionalPushResult> {
        return pushConditional(RPC_SYNC_EXCERPT_TAGS, accessToken, relations)
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

    override suspend fun fetchNode(
        accessToken: String,
        userId: String,
        nodeId: String,
    ): RemoteKnowledgeTreeNode? {
        return fetchRow(REMOTE_TABLE_KNOWLEDGE_TREE_NODE, accessToken, userId, nodeId)
    }

    override suspend fun fetchTag(
        accessToken: String,
        userId: String,
        tagId: String,
    ): RemoteTag? {
        return fetchRow(REMOTE_TABLE_TAGS, accessToken, userId, tagId)
    }

    override suspend fun fetchExcerpt(
        accessToken: String,
        userId: String,
        excerptId: String,
    ): RemoteExcerpt? {
        return fetchRow(REMOTE_TABLE_EXCERPTS, accessToken, userId, excerptId)
    }

    override suspend fun fetchExcerptTag(
        accessToken: String,
        userId: String,
        relationId: String,
    ): RemoteExcerptTag? {
        return fetchRow(REMOTE_TABLE_EXCERPT_TAGS, accessToken, userId, relationId)
    }

    private suspend inline fun <reified T> pushConditional(
        rpcName: String,
        accessToken: String,
        items: List<T>,
    ): List<ConditionalPushResult> {
        if (items.isEmpty() || !config.isConfigured) return emptyList()
        val results = mutableListOf<ConditionalPushResult>()
        for (batch in items.chunked(RPC_PUSH_BATCH_SIZE)) {
            val response = httpClient.post("${config.normalizedUrl}/rest/v1/rpc/$rpcName") {
                supabaseHeaders(accessToken)
                contentType(ContentType.Application.Json)
                setBody(SupabaseRpcJson.encodeToString(ConditionalPushRequest(batch)))
            }
            response.ensureSuccess(rpcName)
            results += response.body<List<ConditionalPushResult>>()
        }
        return results
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

    private suspend inline fun <reified T> fetchRow(
        tableName: String,
        accessToken: String,
        userId: String,
        id: String,
    ): T? {
        if (!config.isConfigured) return null
        val response = httpClient.get("${config.normalizedUrl}/rest/v1/$tableName") {
            supabaseHeaders(accessToken)
            parameter("select", "*")
            parameter("user_id", "eq.$userId")
            parameter("id", "eq.$id")
            parameter("limit", "1")
        }
        response.ensureSuccess(tableName)
        return response.body<List<T>>().firstOrNull()
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

private val SupabaseRpcJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
    encodeDefaults = true
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
private data class ConditionalPushRequest<T>(
    @SerialName("p_rows") val rows: List<T>,
)

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

private const val RPC_PUSH_BATCH_SIZE = 50
private const val RPC_SYNC_KNOWLEDGE_TREE_NODE = "sync_knowledge_tree_node_conditional"
private const val RPC_SYNC_EXCERPTS = "sync_excerpts_conditional"
private const val RPC_SYNC_TAGS = "sync_tags_conditional"
private const val RPC_SYNC_EXCERPT_TAGS = "sync_excerpt_tags_conditional"
