package com.gleanread.android.feature.knowledge_tree.model

data class KnowledgeTreeHomeUiState(
    val rootCards: List<RootNodeCardUiModel>,
    val isEmpty: Boolean,
)
