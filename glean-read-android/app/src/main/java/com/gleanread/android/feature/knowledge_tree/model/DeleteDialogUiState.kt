package com.gleanread.android.feature.knowledge_tree.model

data class DeleteDialogUiState(
    val target: NodeActionTarget,
    val descendantCount: Int,
) {
    val hasChildren: Boolean
        get() = descendantCount > 0
}
