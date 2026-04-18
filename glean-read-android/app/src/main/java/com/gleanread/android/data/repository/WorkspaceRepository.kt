package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDao
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.BacklinkType
import com.gleanread.android.data.model.BacklinkUiModel
import com.gleanread.android.data.model.ExcerptUiModel
import com.gleanread.android.data.model.FlatNodeUiModel
import com.gleanread.android.data.model.GraphNodeKind
import com.gleanread.android.data.model.GraphUiEdge
import com.gleanread.android.data.model.GraphUiModel
import com.gleanread.android.data.model.GraphUiNode
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.data.model.LinkSuggestionType
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.OutlineDraft
import com.gleanread.android.data.model.SuggestedTagUiModel
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.model.TagGroupUiModel
import com.gleanread.android.data.model.TagUiModel
import com.gleanread.android.data.model.TreeNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.data.model.buildInlineAnnotatedString
import com.gleanread.android.data.model.excerptTitleFallback
import com.gleanread.android.data.model.extractStructuredLinks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WorkspaceRepository(
    private val database: WorkspaceDatabase,
    private val outlineGenerator: OutlineGenerator = LocalOutlineGenerator(),
) {
    private val dao: WorkspaceDao = database.workspaceDao()

    val snapshot: Flow<WorkspaceSnapshot> = combine(
        dao.observeExcerpts(),
        dao.observeNodes(),
        dao.observeTags(),
        dao.observeExcerptTags(),
    ) { excerpts, nodes, tags, relations ->
        buildSnapshot(excerpts, nodes, tags, relations)
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
        val snapshot = buildSnapshot(
            dao.getExcerptsOnce(),
            dao.getNodesOnce(),
            dao.getTagsOnce(),
            dao.getExcerptTagsOnce(),
        )
        val nodeSuggestions = snapshot.flatNodes.values.map {
            LocalSuggestionCandidate(
                id = it.id,
                title = it.title,
                preview = it.outlineMarkdown.ifBlank { "知识树节点" },
                type = LinkSuggestionType.NODE,
            )
        }
        val excerptSuggestions = snapshot.excerpts.map {
            LocalSuggestionCandidate(
                id = it.id,
                title = excerptTitleFallback(it),
                preview = buildString {
                    append(it.content)
                    if (it.thought.isNotBlank()) {
                        append(" ")
                        append(it.thought)
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
            )
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
            )
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
            )
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
                    }
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
                        }
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
                )
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
                    }
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
                    )
                )
            } else {
                dao.findNodeById(resolvedNodeId)?.let { node ->
                    dao.updateNode(
                        node.copy(
                            outlineMarkdown = outlineMarkdown,
                            updateTime = now,
                            syncStatus = SyncStatus.bump(node.syncStatus),
                        )
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
                }
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
            )
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

    private fun buildSnapshot(
        excerpts: List<ExcerptEntity>,
        nodes: List<KnowledgeTreeNodeEntity>,
        tags: List<TagEntity>,
        relations: List<ExcerptTagEntity>,
    ): WorkspaceSnapshot {
        val nodeMap = nodes.associateBy { it.id }
        val tagMap = tags.associateBy { it.id }
        val tagNamesByExcerptId = relations.groupBy { it.excerptId }
            .mapValues { (_, value) -> value.mapNotNull { tagMap[it.tagId]?.tagName }.sorted() }

        val excerptsUi = excerpts.map { excerpt ->
            val archivedNode = excerpt.treeNodeId?.let(nodeMap::get)
            ExcerptUiModel(
                id = excerpt.id,
                content = excerpt.content,
                thought = excerpt.userThought.orEmpty(),
                url = excerpt.url,
                sourceTitle = excerpt.sourceTitle,
                tags = tagNamesByExcerptId[excerpt.id].orEmpty(),
                archivedNodeId = archivedNode?.id,
                archivedNodeTitle = archivedNode?.nodeTitle,
                createTime = excerpt.createTime,
            )
        }

        val excerptIdsByNodeId = excerpts.groupBy { it.treeNodeId }
            .mapValues { (_, value) -> value.map { it.id } }
        val childNodeIdsByParent = nodes.groupBy { it.parentId }
            .mapValues { (_, value) -> value.map(KnowledgeTreeNodeEntity::id) }

        val flatNodes = nodes.associate { node ->
            node.id to FlatNodeUiModel(
                id = node.id,
                parentId = node.parentId,
                title = node.nodeTitle,
                outlineMarkdown = node.outlineMarkdown.orEmpty(),
                excerptIds = excerptIdsByNodeId[node.id].orEmpty(),
                excerptCount = excerptIdsByNodeId[node.id].orEmpty().size,
                childNodeIds = childNodeIdsByParent[node.id].orEmpty(),
            )
        }

        val treeRoots = buildTree(nodeMap.values.toList(), excerptIdsByNodeId)
        val tagGroups = buildTagGroups(tags)
        val backlinksByNodeId = buildBacklinks(nodes, excerpts)
        val graphByNodeId = buildGraphs(flatNodes, excerptsUi.associateBy { it.id }, backlinksByNodeId)

        return WorkspaceSnapshot(
            isEmpty = excerpts.isEmpty() && nodes.isEmpty() && tags.isEmpty(),
            excerpts = excerptsUi.sortedByDescending { it.createTime },
            treeRoots = treeRoots,
            flatNodes = flatNodes,
            excerptsById = excerptsUi.associateBy { it.id },
            tagGroups = tagGroups,
            backlinksByNodeId = backlinksByNodeId,
            graphByNodeId = graphByNodeId,
            suggestedTags = tags.sortedByDescending { it.heatWeight }.take(6).map {
                SuggestedTagUiModel(
                    fullName = it.tagName,
                    label = normalizeTagLabel(it.tagName),
                )
            },
        )
    }

    private fun buildTree(
        nodes: List<KnowledgeTreeNodeEntity>,
        excerptIdsByNodeId: Map<String?, List<String>>,
    ): List<TreeNodeUiModel> {
        val childrenByParent = nodes.groupBy { it.parentId }
        fun toTree(parentId: String?): List<TreeNodeUiModel> {
            return childrenByParent[parentId].orEmpty().map { node ->
                val children = toTree(node.id)
                TreeNodeUiModel(
                    id = node.id,
                    title = node.nodeTitle,
                    count = excerptIdsByNodeId[node.id].orEmpty().size,
                    children = children,
                )
            }
        }
        return toTree(null)
    }

    private fun buildTagGroups(tags: List<TagEntity>): List<TagGroupUiModel> {
        return tags.groupBy { folderName(it.tagName) }
            .map { (folder, group) ->
                TagGroupUiModel(
                    folder = folder,
                    count = group.sumOf { it.heatWeight },
                    items = group.sortedByDescending { it.heatWeight }.map { tag ->
                        TagUiModel(
                            id = tag.id,
                            folder = folder,
                            displayName = displayTagName(tag.tagName),
                            fullName = tag.tagName,
                            heatWeight = tag.heatWeight,
                        )
                    }
                )
            }
            .sortedByDescending { it.count }
    }

    private fun buildBacklinks(
        nodes: List<KnowledgeTreeNodeEntity>,
        excerpts: List<ExcerptEntity>,
    ): Map<String, List<BacklinkUiModel>> {
        val backlinks = mutableMapOf<String, MutableList<BacklinkUiModel>>()
        nodes.forEach { node ->
            val text = node.outlineMarkdown.orEmpty()
            extractStructuredLinks(text).forEach { link ->
                backlinks.getOrPut(link.targetId) { mutableListOf() }.add(
                    BacklinkUiModel(
                        sourceId = node.id,
                        title = node.nodeTitle,
                        sourceType = BacklinkType.NODE,
                        snippet = toDisplay(text),
                    )
                )
            }
        }
        excerpts.forEach { excerpt ->
            listOf(excerpt.content, excerpt.userThought.orEmpty()).forEach { text ->
                extractStructuredLinks(text).forEach { link ->
                    backlinks.getOrPut(link.targetId) { mutableListOf() }.add(
                        BacklinkUiModel(
                            sourceId = excerpt.id,
                            title = excerptTitle(excerpt),
                            sourceType = BacklinkType.EXCERPT,
                            snippet = toDisplay(text),
                        )
                    )
                }
            }
        }
        return backlinks.mapValues { (_, items) -> items.distinctBy { it.sourceId } }
    }

    private fun buildGraphs(
        flatNodes: Map<String, FlatNodeUiModel>,
        excerptsById: Map<String, ExcerptUiModel>,
        backlinksByNodeId: Map<String, List<BacklinkUiModel>>,
    ): Map<String, GraphUiModel> {
        return flatNodes.values.associate { node ->
            val nodes = mutableListOf(
                GraphUiNode(node.id, node.title, GraphNodeKind.CURRENT_NODE)
            )
            val edges = mutableListOf<GraphUiEdge>()

            node.excerptIds.forEach { excerptId ->
                excerptsById[excerptId]?.let { excerpt ->
                    nodes.add(GraphUiNode(excerpt.id, excerptTitleFallback(excerpt), GraphNodeKind.EXCERPT))
                    edges.add(GraphUiEdge(node.id, excerpt.id))
                }
            }

            extractStructuredLinks(node.outlineMarkdown).forEach { link ->
                val linkedNode = flatNodes[link.targetId]
                if (linkedNode != null) {
                    nodes.add(GraphUiNode(linkedNode.id, linkedNode.title, GraphNodeKind.LINKED_NODE))
                    edges.add(GraphUiEdge(node.id, linkedNode.id))
                }
            }

            backlinksByNodeId[node.id].orEmpty().forEach { backlink ->
                val kind = if (backlink.sourceType == BacklinkType.NODE) GraphNodeKind.BACKLINK_NODE else GraphNodeKind.EXCERPT
                nodes.add(GraphUiNode(backlink.sourceId, backlink.title, kind))
                edges.add(GraphUiEdge(backlink.sourceId, node.id))
            }

            node.id to GraphUiModel(
                nodes = nodes.distinctBy { it.id },
                edges = edges.distinct(),
            )
        }
    }

    private fun folderName(tagName: String): String {
        return if (tagName.contains('/')) tagName.substringBefore('/') else "无分类"
    }

    private fun displayTagName(tagName: String): String {
        return if (tagName.contains('/')) tagName.substringAfterLast('/') else tagName
    }

    private fun normalizeTagLabel(tagName: String): String = "#${displayTagName(tagName)}"

    private fun excerptTitle(excerpt: ExcerptEntity): String {
        return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
            ?: excerpt.content.take(18).trim() + if (excerpt.content.length > 18) "..." else ""
    }

    private fun toDisplay(text: String): String {
        return buildInlineAnnotatedString(text).text.take(80)
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
