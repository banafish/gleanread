package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.core.richtext.LinkSuggestionType
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LocalSuggestionCandidate
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider

class KnowledgeTreeRepository(
    private val database: WorkspaceDatabase,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
    private val currentUserIdProvider: CurrentUserIdProvider = LocalCurrentUserIdProvider,
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
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        val sortOrder = calculateSortOrderForAppend(null)
        nodeDao.insertNode(
            KnowledgeTreeNodeEntity(
                id = nodeId,
                userId = ownerUserId,
                parentId = null,
                nodeTitle = trimmed,
                outlineMarkdown = "",
                createTime = now,
                updateTime = now,
                deviceId = deviceId,
                syncStatus = SyncStatus.PENDING_CREATE,
                localDirtyTime = now,
                sortOrder = sortOrder,
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
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        val sortOrder = calculateSortOrderForAppend(parentId)
        nodeDao.insertNode(
            KnowledgeTreeNodeEntity(
                id = nodeId,
                userId = ownerUserId,
                parentId = parentNode.id,
                nodeTitle = trimmed,
                outlineMarkdown = "",
                createTime = now,
                updateTime = now,
                deviceId = deviceId,
                syncStatus = SyncStatus.PENDING_CREATE,
                localDirtyTime = now,
                sortOrder = sortOrder,
            ),
        )
        return nodeId
    }

    suspend fun renameNode(nodeId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val node = nodeDao.findNodeById(nodeId) ?: return
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        nodeDao.updateNode(
            node.copy(
                userId = ownerUserId,
                nodeTitle = trimmed,
                updateTime = now,
                deviceId = deviceId,
                syncStatus = SyncStatus.bump(node.syncStatus),
                syncError = null,
                localDirtyTime = now,
            ),
        )
    }

    suspend fun moveNode(nodeId: String, newParentId: String?) {
        database.withTransaction {
            val allNodes = nodeDao.getNodesOnce()
            val targetNode = allNodes.firstOrNull { it.id == nodeId } ?: return@withTransaction
            if (targetNode.parentId == newParentId || newParentId == nodeId) return@withTransaction

            if (newParentId != null && allNodes.none { it.id == newParentId }) {
                return@withTransaction
            }

            val childrenByParent = allNodes.groupBy { it.parentId }
            val descendantIds = buildSet {
                fun collect(currentId: String) {
                    childrenByParent[currentId].orEmpty().forEach { child ->
                        if (add(child.id)) {
                            collect(child.id)
                        }
                    }
                }
                collect(targetNode.id)
            }
            if (newParentId in descendantIds) return@withTransaction

            val sortOrder = calculateSortOrderForAppend(newParentId)
            val now = System.currentTimeMillis()
            val deviceId = deviceIdProvider.currentDeviceId()
            val ownerUserId = currentUserIdProvider.currentUserId()
            nodeDao.updateNode(
                targetNode.copy(
                    userId = ownerUserId,
                    parentId = newParentId,
                    sortOrder = sortOrder,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.bump(targetNode.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                ),
            )
        }
    }

    /**
     * 将节点移动到同级节点列表中的指定位置（仅更新排序，不改变层级）。
     * @param nodeId 被移动的节点 ID
     * @param targetIndex 在同级节点列表中的插入位置
     */
    suspend fun moveNodeToPosition(nodeId: String, targetIndex: Int) {
        database.withTransaction {
            val targetNode = nodeDao.findNodeById(nodeId) ?: return@withTransaction

            val newSortOrder = calculateSortOrderAt(
                targetIndex = targetIndex,
                parentId = targetNode.parentId,
                excludeNodeId = nodeId,
            )

            val now = System.currentTimeMillis()
            val deviceId = deviceIdProvider.currentDeviceId()
            val ownerUserId = currentUserIdProvider.currentUserId()
            nodeDao.updateNode(
                targetNode.copy(
                    userId = ownerUserId,
                    sortOrder = newSortOrder,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.bump(targetNode.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                ),
            )
        }
    }

    suspend fun deleteNodeSubtree(nodeId: String) {
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
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
                            userId = ownerUserId,
                            isDeleted = true,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.markDeleted(node.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
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
                                userId = ownerUserId,
                                treeNodeId = null,
                                updateTime = now,
                                deviceId = deviceId,
                                syncStatus = SyncStatus.bump(excerpt.syncStatus),
                                syncError = null,
                                localDirtyTime = now,
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
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        nodeDao.updateNode(
            node.copy(
                userId = ownerUserId,
                outlineMarkdown = rawMarkdown,
                updateTime = now,
                deviceId = deviceId,
                syncStatus = SyncStatus.bump(node.syncStatus),
                syncError = null,
                localDirtyTime = now,
            ),
        )
    }

    suspend fun moveExcerptToInbox(excerptId: String) {
        val excerpt = excerptDao.findExcerptById(excerptId) ?: return
        if (excerpt.treeNodeId == null || excerpt.isDeleted) return

        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        excerptDao.updateExcerpts(
            listOf(
                excerpt.copy(
                    userId = ownerUserId,
                    treeNodeId = null,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.bump(excerpt.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                ),
            ),
        )
    }

    private fun excerptTitle(excerpt: ExcerptEntity): String {
        return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
            ?: excerpt.content.take(EXCERPT_TITLE_MAX_LENGTH).trim() +
            if (excerpt.content.length > EXCERPT_TITLE_MAX_LENGTH) "..." else ""
    }

    /**
     * 计算追加到指定父节点末尾的排序值。
     */
    private suspend fun calculateSortOrderForAppend(parentId: String?): Long {
        val maxSortOrder = nodeDao.maxSortOrder(parentId)
        return (maxSortOrder ?: 0) + SORT_ORDER_GAP
    }

    /**
     * 计算插入到目标索引的排序值。
     * 如果前后节点都为 null，则追加到末尾。
     * 间隔不足时触发局部重排。
     */
    private suspend fun calculateSortOrderAt(
        targetIndex: Int,
        parentId: String?,
        excludeNodeId: String? = null,
    ): Long {
        val siblings = nodeDao.getSiblingsOnce(parentId).let { list ->
            if (excludeNodeId != null) list.filter { it.id != excludeNodeId } else list
        }
        val prevSortOrder = siblings.getOrNull(targetIndex - 1)?.sortOrder
        val nextSortOrder = siblings.getOrNull(targetIndex)?.sortOrder

        // 无前后兄弟，追加到末尾
        if (prevSortOrder == null && nextSortOrder == null) {
            return calculateSortOrderForAppend(parentId)
        }
        // 插入到列表开头
        if (prevSortOrder == null) {
            val newOrder = nextSortOrder!! - SORT_ORDER_GAP
            if (newOrder <= Long.MIN_VALUE + 1) {
                rebalanceSiblings(parentId)
                return calculateSortOrderAt(targetIndex, parentId, excludeNodeId)
            }
            return newOrder
        }
        // 插入到列表末尾
        if (nextSortOrder == null) {
            return prevSortOrder + SORT_ORDER_GAP
        }
        // 插入到两个节点之间
        val mid = (prevSortOrder + nextSortOrder) / 2
        if (mid <= prevSortOrder + 1 || mid >= nextSortOrder - 1) {
            rebalanceSiblings(parentId)
            return calculateSortOrderAt(targetIndex, parentId, excludeNodeId)
        }
        return mid
    }

    /**
     * 对指定父节点下所有兄弟节点重新分配排序值（间隔 SORT_ORDER_GAP），在事务中执行。
     */
    private suspend fun rebalanceSiblings(parentId: String?) {
        database.withTransaction {
            val siblings = nodeDao.getSiblingsOnce(parentId)
            if (siblings.isEmpty()) return@withTransaction
            val now = System.currentTimeMillis()
            val deviceId = deviceIdProvider.currentDeviceId()
            val ownerUserId = currentUserIdProvider.currentUserId()
            val reordered = siblings.mapIndexed { index, node ->
                node.copy(
                    sortOrder = (index + 1).toLong() * SORT_ORDER_GAP,
                    userId = ownerUserId,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.bump(node.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                )
            }
            nodeDao.updateNodes(reordered)
        }
    }

    companion object {
        private const val EXCERPT_TITLE_MAX_LENGTH = 18
        private const val SUGGESTION_PER_TYPE_LIMIT = 6
        private const val SUGGESTION_TOTAL_LIMIT = 8
        private const val SORT_ORDER_GAP: Long = 65536L
    }
}

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
        .take(SEARCH_SUGGESTION_PER_TYPE_LIMIT)
        .map { LinkSuggestion(it.id, it.title, it.preview, it.type) }
        .toList()
    val excerpts = excerptSuggestions.asSequence()
        .filter(::matches)
        .take(SEARCH_SUGGESTION_PER_TYPE_LIMIT)
        .map { LinkSuggestion(it.id, it.title, it.preview, it.type) }
        .toList()
    return (nodes + excerpts).take(SEARCH_SUGGESTION_TOTAL_LIMIT)
}

private const val SEARCH_SUGGESTION_PER_TYPE_LIMIT = 6
private const val SEARCH_SUGGESTION_TOTAL_LIMIT = 8
