package com.gleanread.android.feature.knowledge_tree.model

enum class NodeDialogType {
    ADD_ROOT,
    ADD_CHILD,
    RENAME,
}

data class NodeDialogUiState(
    val type: NodeDialogType,
    val inputValue: String = "",
    val parentNodeId: String? = null,
    val parentNodeTitle: String? = null,
    val targetNodeId: String? = null,
    val targetNodeTitle: String? = null,
)
