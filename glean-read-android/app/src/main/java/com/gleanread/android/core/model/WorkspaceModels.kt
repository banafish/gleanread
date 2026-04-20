package com.gleanread.android.core.model

enum class BacklinkType {
    NODE,
    EXCERPT,
}

enum class GraphNodeKind {
    CURRENT_NODE,
    LINKED_NODE,
    BACKLINK_NODE,
    EXCERPT,
}

data class ExcerptUiModel(
    val id: String,
    val content: String,
    val thought: String,
    val url: String?,
    val sourceTitle: String?,
    val tags: List<String>,
    val archivedNodeId: String?,
    val archivedNodeTitle: String?,
    val createTime: Long,
)

data class FlatNodeUiModel(
    val id: String,
    val parentId: String?,
    val title: String,
    val outlineMarkdown: String,
    val excerptIds: List<String>,
    val excerptCount: Int,
    val childNodeIds: List<String>,
)

data class TreeNodeUiModel(
    val id: String,
    val title: String,
    val count: Int,
    val children: List<TreeNodeUiModel>,
)

data class TagUiModel(
    val id: String,
    val folder: String,
    val displayName: String,
    val fullName: String,
    val heatWeight: Int,
)

data class TagGroupUiModel(
    val folder: String,
    val count: Int,
    val items: List<TagUiModel>,
)

data class BacklinkUiModel(
    val sourceId: String,
    val title: String,
    val sourceType: BacklinkType,
    val snippet: String,
)

data class GraphUiNode(
    val id: String,
    val title: String,
    val kind: GraphNodeKind,
)

data class GraphUiEdge(
    val fromId: String,
    val toId: String,
)

data class GraphUiModel(
    val nodes: List<GraphUiNode>,
    val edges: List<GraphUiEdge>,
)

data class SuggestedTagUiModel(
    val fullName: String,
    val label: String,
)

data class WorkspaceSnapshot(
    val isEmpty: Boolean,
    val excerpts: List<ExcerptUiModel>,
    val treeRoots: List<TreeNodeUiModel>,
    val flatNodes: Map<String, FlatNodeUiModel>,
    val excerptsById: Map<String, ExcerptUiModel>,
    val tagGroups: List<TagGroupUiModel>,
    val backlinksByNodeId: Map<String, List<BacklinkUiModel>>,
    val graphByNodeId: Map<String, GraphUiModel>,
    val suggestedTags: List<SuggestedTagUiModel>,
) {
    companion object {
        val Empty = WorkspaceSnapshot(
            isEmpty = true,
            excerpts = emptyList(),
            treeRoots = emptyList(),
            flatNodes = emptyMap(),
            excerptsById = emptyMap(),
            tagGroups = emptyList(),
            backlinksByNodeId = emptyMap(),
            graphByNodeId = emptyMap(),
            suggestedTags = emptyList(),
        )
    }
}

fun excerptTitleFallback(excerpt: ExcerptUiModel): String {
    return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
        ?: excerpt.content.take(18).trim() + if (excerpt.content.length > 18) "..." else ""
}
