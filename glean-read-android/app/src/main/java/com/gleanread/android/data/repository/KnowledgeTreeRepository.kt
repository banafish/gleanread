package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.core.richtext.LinkSuggestionType
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus

class KnowledgeTreeRepository(
    private val database: WorkspaceDatabase,
) {
    private val excerptDao = database.excerptDao()
    private val nodeDao = database.nodeDao()

    suspend fun searchSuggestions(query: String): List<LinkSuggestion> {
        val nodes = nodeDao.getNodesOnce()
        val excerpts = excerptDao.getExcerptsOnce()
        val nodeSuggestions = nodes.map {
            LocalSuggestionCandidate(
                id = it.id,
                title = it.nodeTitle,
                preview = it.outlineMarkdown.orEmpty().ifBlank { "Knowledge tree node" },
                type = LinkSuggestionType.NODE,
            )
        }
        val excerptSuggestions = excerpts.map {
            LocalSuggestionCandidate(
                id = it.id,
                title = excerptTitle(it),
                preview = buildString {
                    append(it.content)
                    if (!it.userThought.isNullOrBlank()) {
                        append(" ")
                        append(it.userThought)
                    }
                },
                type = LinkSuggestionType.EXCERPT,
            )
        }
        return searchSuggestionsForInlineQuery(query, nodeSuggestions, excerptSuggestions)
    }

    suspend fun createRootNode(title: String): String {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return ""
        val now = System.currentTimeMillis()
        val nodeId = EntityIdGenerator.newNodeId()
        nodeDao.insertNode(
            KnowledgeTreeNodeEntity(
                id = nodeId,
                userId = LOCAL_USER_ID,
                parentId = null,
                nodeTitle = trimmed,
                outlineMarkdown = "",
                createTime = now,
                updateTime = now,
                syncStatus = SyncStatus.PENDING_CREATE.code,
            ),
        )
        return nodeId
    }

    suspend fun createChildNode(parentId: String, title: String): String {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return ""
        val parentNode = nodeDao.findNodeById(parentId) ?: return ""
        val now = System.currentTimeMillis()
        val nodeId = EntityIdGenerator.newNodeId()
        nodeDao.insertNode(
            KnowledgeTreeNodeEntity(
                id = nodeId,
                userId = LOCAL_USER_ID,
                parentId = parentNode.id,
                nodeTitle = trimmed,
                outlineMarkdown = "",
                createTime = now,
                updateTime = now,
                syncStatus = SyncStatus.PENDING_CREATE.code,
            ),
        )
        return nodeId
    }

    suspend fun renameNode(nodeId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val node = nodeDao.findNodeById(nodeId) ?: return
        val now = System.currentTimeMillis()
        nodeDao.updateNode(
            node.copy(
                nodeTitle = trimmed,
                updateTime = now,
                syncStatus = SyncStatus.bump(node.syncStatus),
            ),
        )
    }

    suspend fun deleteNodeSubtree(nodeId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val allNodes = nodeDao.getNodesOnce()
            val targetNode = allNodes.firstOrNull { it.id == nodeId } ?: return@withTransaction
            val childrenByParent = allNodes.groupBy { it.parentId }
            val subtreeIds = buildList {
                fun collect(currentId: String) {
                    add(currentId)
                    childrenByParent[currentId].orEmpty().forEach { child -> collect(child.id) }
                }
                collect(targetNode.id)
            }
            val subtreeNodes = allNodes.filter { subtreeIds.contains(it.id) }
            if (subtreeNodes.isNotEmpty()) {
                nodeDao.updateNodes(
                    subtreeNodes.map { node ->
                        node.copy(
                            isDeleted = true,
                            updateTime = now,
                            syncStatus = SyncStatus.markDeleted(node.syncStatus),
                        )
                    },
                )
            }
            if (subtreeIds.isNotEmpty()) {
                val affectedExcerpts = excerptDao.findExcerptsByNodeIds(subtreeIds)
                if (affectedExcerpts.isNotEmpty()) {
                    excerptDao.updateExcerpts(
                        affectedExcerpts.map { excerpt ->
                            excerpt.copy(
                                treeNodeId = null,
                                updateTime = now,
                                syncStatus = SyncStatus.bump(excerpt.syncStatus),
                            )
                        },
                    )
                }
            }
        }
    }

    suspend fun updateNodeOutline(nodeId: String, rawMarkdown: String) {
        val node = nodeDao.findNodeById(nodeId) ?: return
        val now = System.currentTimeMillis()
        nodeDao.updateNode(
            node.copy(
                outlineMarkdown = rawMarkdown,
                updateTime = now,
                syncStatus = SyncStatus.bump(node.syncStatus),
            ),
        )
    }

    private fun excerptTitle(excerpt: ExcerptEntity): String {
        return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
            ?: excerpt.content.take(18).trim() + if (excerpt.content.length > 18) "..." else ""
    }
}

data class LocalSuggestionCandidate(
    val id: String,
    val title: String,
    val preview: String,
    val type: LinkSuggestionType,
)

fun searchSuggestionsForInlineQuery(
    query: String,
    nodeSuggestions: List<LocalSuggestionCandidate>,
    excerptSuggestions: List<LocalSuggestionCandidate>,
): List<LinkSuggestion> {
    val normalized = query.trim()

    fun matches(candidate: LocalSuggestionCandidate): Boolean {
        return normalized.isBlank() ||
            candidate.title.contains(normalized, ignoreCase = true) ||
            candidate.preview.contains(normalized, ignoreCase = true)
    }

    val nodes = nodeSuggestions.asSequence()
        .filter(::matches)
        .take(6)
        .map { LinkSuggestion(it.id, it.title, it.preview, it.type) }
        .toList()
    val excerpts = excerptSuggestions.asSequence()
        .filter(::matches)
        .take(6)
        .map { LinkSuggestion(it.id, it.title, it.preview, it.type) }
        .toList()
    return (nodes + excerpts).take(8)
}
