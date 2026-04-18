package com.gleanread.android.feature.knowledge_tree.model

sealed interface KnowledgeTreeUiEvent {
    data class OpenNodeDetail(val nodeId: String) : KnowledgeTreeUiEvent
    data class OpenBranch(val nodeId: String) : KnowledgeTreeUiEvent
}
