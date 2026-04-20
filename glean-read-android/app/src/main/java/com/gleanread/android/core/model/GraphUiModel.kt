package com.gleanread.android.core.model

enum class GraphNodeKind {
    CURRENT_NODE,
    LINKED_NODE,
    BACKLINK_NODE,
    EXCERPT,
}

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
