package com.gleanread.android.feature.knowledge_tree.model

sealed interface KnowledgeTreeUiAction {
    data class ToggleNode(val nodeId: String) : KnowledgeTreeUiAction
    data class OpenNodeMenu(val target: NodeActionTarget) : KnowledgeTreeUiAction
    data object ExpandAll : KnowledgeTreeUiAction
    data object CollapseAll : KnowledgeTreeUiAction
    data object OpenSearch : KnowledgeTreeUiAction
    data object CloseSearch : KnowledgeTreeUiAction
}
