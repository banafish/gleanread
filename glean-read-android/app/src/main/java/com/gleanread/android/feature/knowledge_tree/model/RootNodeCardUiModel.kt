package com.gleanread.android.feature.knowledge_tree.model

data class RootNodeCardUiModel(
    val nodeId: String,
    val title: String,
    val count: Int,
    val isExpanded: Boolean,
    val canExpand: Boolean,
    val previewItems: List<PreviewNodeUiModel>,
    val detailDestination: NodeDestination,
    val branchDestination: NodeDestination,
    val actionTarget: NodeActionTarget,
)
