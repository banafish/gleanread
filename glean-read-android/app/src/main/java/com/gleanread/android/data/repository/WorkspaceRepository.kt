package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.core.richtext.LinkSuggestionType
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDao
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WorkspaceRepository(
    private val database: WorkspaceDatabase,
    private val outlineGenerator: OutlineGenerator = LocalOutlineGenerator(),
) {
    private val dao: WorkspaceDao = database.workspaceDao()

    val localSnapshot: Flow<WorkspaceLocalSnapshot> = combine(
        dao.observeExcerpts(),
        dao.observeNodes(),
        dao.observeTags(),
        dao.observeExcerptTags(),
    ) { excerpts, nodes, tags, relations ->
        WorkspaceLocalSnapshot(
            excerpts = excerpts,
            nodes = nodes,
            tags = tags,
            relations = relations,
        )
    }.distinctUntilChanged()

    fun observeAvailableTagNames(): Flow<List<String>> {
        return dao.observeTags().map { tags -> tags.map(TagEntity::tagName) }
    }

    suspend fun seedSampleData() {
        val hasData = dao.countExcerpts() > 0 || dao.countNodes() > 0 || dao.countTags() > 0
        if (hasData) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.insertNodes(WorkspaceSeedData.nodes(now))
            dao.insertTags(WorkspaceSeedData.tags(now))
            dao.insertExcerpts(WorkspaceSeedData.excerpts(now))
            dao.insertExcerptTags(WorkspaceSeedData.excerptTags(now))
        }
    }

    suspend fun searchSuggestions(query: String): List<LinkSuggestion> {
        val nodes = dao.getNodesOnce()
        val excerpts = dao.getExcerptsOnce()
        val nodeSuggestions = nodes.map {
            LocalSuggestionCandidate(
                id = it.id,
                title = it.nodeTitle,
                preview = it.outlineMarkdown.orEmpty().ifBlank { "知识树节点" },
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
        val nodeId = WorkspaceSeedData.newNodeId()
        dao.insertNode(
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
        val parentNode = dao.findNodeById(parentId) ?: return ""
        val now = System.currentTimeMillis()
        val nodeId = WorkspaceSeedData.newNodeId()
        dao.insertNode(
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
        val node = dao.findNodeById(nodeId) ?: return
        val now = System.currentTimeMillis()
        dao.updateNode(
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
            val allNodes = dao.getNodesOnce()
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
                dao.updateNodes(
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
                val affectedExcerpts = dao.findExcerptsByNodeIds(subtreeIds)
                if (affectedExcerpts.isNotEmpty()) {
                    dao.updateExcerpts(
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

    suspend fun saveQuickExcerpt(
        content: String,
        thought: String,
        url: String?,
        sourceTitle: String?,
        tagNames: List<String>,
        archiveNodeId: String?,
    ): String {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return ""
        val now = System.currentTimeMillis()
        val excerptId = WorkspaceSeedData.newDraftExcerptId()
        database.withTransaction {
            val resolvedTags = resolveTags(tagNames, now)
            dao.insertExcerpt(
                ExcerptEntity(
                    id = excerptId,
                    userId = LOCAL_USER_ID,
                    content = trimmedContent,
                    url = url?.trim()?.takeIf { it.isNotEmpty() },
                    sourceTitle = sourceTitle?.trim()?.takeIf { it.isNotEmpty() },
                    userThought = thought.trim().takeIf { it.isNotEmpty() },
                    treeNodeId = archiveNodeId,
                    createTime = now,
                    updateTime = now,
                    syncStatus = SyncStatus.PENDING_CREATE.code,
                ),
            )
            if (resolvedTags.isNotEmpty()) {
                dao.insertExcerptTags(
                    resolvedTags.map { tag ->
                        ExcerptTagEntity(
                            id = WorkspaceSeedData.newRelationId(),
                            userId = LOCAL_USER_ID,
                            excerptId = excerptId,
                            tagId = tag.id,
                            createTime = now,
                            updateTime = now,
                            syncStatus = SyncStatus.PENDING_CREATE.code,
                        )
                    },
                )
            }
        }
        return excerptId
    }

    suspend fun generateOutline(selectedExcerptIds: List<String>): OutlineDraft {
        val excerpts = dao.getExcerptsOnce()
            .filter { selectedExcerptIds.contains(it.id) }
            .sortedByDescending { it.createTime }
        return outlineGenerator.generate(excerpts.map { it.content })
    }

    suspend fun saveAiSummary(
        selectedExcerptIds: List<String>,
        outlineMarkdown: String,
        targetNodeId: String?,
        newNodeTitle: String?,
        parentNodeId: String?,
    ): String {
        if (selectedExcerptIds.isEmpty()) return ""
        val now = System.currentTimeMillis()
        var resolvedNodeId = targetNodeId.orEmpty()
        database.withTransaction {
            if (resolvedNodeId.isBlank()) {
                val title = newNodeTitle?.trim().takeUnless { it.isNullOrEmpty() } ?: "新节点"
                resolvedNodeId = WorkspaceSeedData.newNodeId()
                dao.insertNode(
                    KnowledgeTreeNodeEntity(
                        id = resolvedNodeId,
                        userId = LOCAL_USER_ID,
                        parentId = parentNodeId,
                        nodeTitle = title,
                        outlineMarkdown = outlineMarkdown,
                        createTime = now,
                        updateTime = now,
                        syncStatus = SyncStatus.PENDING_CREATE.code,
                    ),
                )
            } else {
                dao.findNodeById(resolvedNodeId)?.let { node ->
                    dao.updateNode(
                        node.copy(
                            outlineMarkdown = outlineMarkdown,
                            updateTime = now,
                            syncStatus = SyncStatus.bump(node.syncStatus),
                        ),
                    )
                }
            }

            val excerpts = dao.getExcerptsOnce().filter { selectedExcerptIds.contains(it.id) }
            dao.updateExcerpts(
                excerpts.map { excerpt ->
                    excerpt.copy(
                        treeNodeId = resolvedNodeId,
                        updateTime = now,
                        syncStatus = SyncStatus.bump(excerpt.syncStatus),
                    )
                },
            )
        }
        return resolvedNodeId
    }

    suspend fun updateNodeOutline(nodeId: String, rawMarkdown: String) {
        val node = dao.findNodeById(nodeId) ?: return
        val now = System.currentTimeMillis()
        dao.updateNode(
            node.copy(
                outlineMarkdown = rawMarkdown,
                updateTime = now,
                syncStatus = SyncStatus.bump(node.syncStatus),
            ),
        )
    }

    private suspend fun resolveTags(tagNames: List<String>, now: Long): List<TagEntity> {
        return tagNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { tagName ->
                val existing = dao.findTagByName(LOCAL_USER_ID, tagName)
                if (existing == null) {
                    TagEntity(
                        id = WorkspaceSeedData.newTagId(),
                        userId = LOCAL_USER_ID,
                        tagName = tagName,
                        colorIcon = null,
                        heatWeight = 1,
                        createTime = now,
                        updateTime = now,
                        syncStatus = SyncStatus.PENDING_CREATE.code,
                    ).also { dao.insertTag(it) }
                } else {
                    existing.copy(
                        heatWeight = existing.heatWeight + 1,
                        updateTime = now,
                        syncStatus = SyncStatus.bump(existing.syncStatus),
                    ).also { dao.updateTag(it) }
                }
            }
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

    val nodes = nodeSuggestions
        .asSequence()
        .filter(::matches)
        .take(6)
        .map { LinkSuggestion(it.id, it.title, it.preview, it.type) }
        .toList()
    val excerpts = excerptSuggestions
        .asSequence()
        .filter(::matches)
        .take(6)
        .map { LinkSuggestion(it.id, it.title, it.preview, it.type) }
        .toList()
    return (nodes + excerpts).take(8)
}
