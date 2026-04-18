package com.gleanread.android.feature.knowledge_tree.model

sealed interface NodeDestination {
    data class Detail(val nodeId: String) : NodeDestination
    data class Branch(val nodeId: String) : NodeDestination
    object None : NodeDestination
}
