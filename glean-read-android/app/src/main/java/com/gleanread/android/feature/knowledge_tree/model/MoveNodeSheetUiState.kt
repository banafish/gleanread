package com.gleanread.android.feature.knowledge_tree.model

data class MoveNodeSheetUiState(
    val targetNodeId: String,
    val targetNodeTitle: String,
    val sourceParentNodeId: String?,
    val currentParentNodeId: String?,
)

data class MoveNodeBottomSheetUiModel(
    val targetNodeTitle: String,
    val breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    val destinations: List<KnowledgeTreeNodePickerDestinationUiModel>,
    val confirmEnabled: Boolean,
)
