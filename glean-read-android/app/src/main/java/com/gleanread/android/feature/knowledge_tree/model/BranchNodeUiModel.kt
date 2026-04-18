package com.gleanread.android.feature.knowledge_tree.model

data class BranchNodeUiModel(
    val nodeId: String,
    val title: String,
    val count: Int,
    val depth: Int,
    val isExpanded: Boolean,
    val canExpand: Boolean,
    val visibleChildren: List<BranchNodeUiModel>,
    val showEnterBranch: Boolean,
    val titleDestination: NodeDestination,
    val detailDestination: NodeDestination,
    val branchDestination: NodeDestination,
    val actionTarget: NodeActionTarget,
)
