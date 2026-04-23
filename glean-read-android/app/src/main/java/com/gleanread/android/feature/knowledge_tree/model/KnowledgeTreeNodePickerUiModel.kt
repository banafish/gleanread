package com.gleanread.android.feature.knowledge_tree.model

data class KnowledgeTreeNodePickerUiModel(
    val breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    val destinations: List<KnowledgeTreeNodePickerDestinationUiModel>,
)

data class KnowledgeTreeNodePickerDestinationUiModel(
    val nodeId: String,
    val title: String,
    val childCount: Int,
    val excerptCount: Int,
)
