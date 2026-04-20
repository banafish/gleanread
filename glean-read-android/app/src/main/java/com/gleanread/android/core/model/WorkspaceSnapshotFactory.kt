package com.gleanread.android.core.model

import com.gleanread.android.core.richtext.extractStructuredLinks
import com.gleanread.android.core.richtext.toDisplayInlineText
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.repository.WorkspaceLocalSnapshot

class WorkspaceSnapshotFactory {
    fun create(snapshot: WorkspaceLocalSnapshot): WorkspaceSnapshot {
        return buildSnapshot(
            excerpts = snapshot.excerpts,
            nodes = snapshot.nodes,
            tags = snapshot.tags,
            relations = snapshot.relations,
        )
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
                    },
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
                    ),
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
                        ),
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
                GraphUiNode(node.id, node.title, GraphNodeKind.CURRENT_NODE),
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
                val kind = if (backlink.sourceType == BacklinkType.NODE) {
                    GraphNodeKind.BACKLINK_NODE
                } else {
                    GraphNodeKind.EXCERPT
                }
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
        return if (tagName.contains('/')) tagName.substringBefore('/') else "Uncategorized"
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
        return toDisplayInlineText(text).take(80)
    }
}
