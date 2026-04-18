package com.gleanread.android.feature.knowledge_tree.model

data class PreviewNodeUiModel(
    val nodeId: String,
    val title: String,
    val count: Int,
    val depth: Int,
    val isExpanded: Boolean,
    val canExpand: Boolean,
    val visibleChildren: List<PreviewNodeUiModel>,
    val showEnterBranch: Boolean,
    val detailDestination: NodeDestination,
    val branchDestination: NodeDestination,
    val actionTarget: NodeActionTarget,
)
