package com.gleanread.android.core.model

data class WorkspaceSnapshot(
    val isEmpty: Boolean,
    val excerpts: List<ExcerptUiModel>,
    val treeRoots: List<TreeNodeUiModel>,
    val flatNodes: Map<String, FlatNodeUiModel>,
    val excerptsById: Map<String, ExcerptUiModel>,
    val tagGroups: List<TagGroupUiModel>,
    val backlinksByNodeId: Map<String, List<BacklinkUiModel>>,
    val graphByNodeId: Map<String, GraphUiModel>,
    val suggestedTags: List<SuggestedTagUiModel>,
) {
    companion object {
        val Empty = WorkspaceSnapshot(
            isEmpty = true,
            excerpts = emptyList(),
            treeRoots = emptyList(),
            flatNodes = emptyMap(),
            excerptsById = emptyMap(),
            tagGroups = emptyList(),
            backlinksByNodeId = emptyMap(),
            graphByNodeId = emptyMap(),
            suggestedTags = emptyList(),
        )
    }
}
