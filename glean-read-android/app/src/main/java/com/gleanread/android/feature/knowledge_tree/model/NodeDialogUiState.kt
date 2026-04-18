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
) {
    val title: String
        get() = when (type) {
            NodeDialogType.ADD_ROOT -> "新增节点"
            NodeDialogType.ADD_CHILD -> "新增子节点"
            NodeDialogType.RENAME -> "重命名节点"
        }

    val confirmLabel: String
        get() = when (type) {
            NodeDialogType.RENAME -> "保存"
            else -> "保存"
        }
}
