package com.gleanread.android.feature.knowledge_tree.model

data class KnowledgeTreeBranchUiState(
    val currentNodeId: String,
    val title: String,
    val breadcrumbTitles: List<String>,
    val items: List<BranchNodeUiModel>,
    val isEmpty: Boolean,
    val actionTarget: NodeActionTarget,
)
