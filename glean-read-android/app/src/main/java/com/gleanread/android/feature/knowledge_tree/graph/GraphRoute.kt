package com.gleanread.android.feature.knowledge_tree.graph

import androidx.compose.runtime.Composable
import com.gleanread.android.core.model.WorkspaceSnapshot

@Composable
fun GraphRoute(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    val graph = snapshot.graphByNodeId[nodeId] ?: return
    val currentNode = snapshot.flatNodes[nodeId] ?: return

    GraphScreen(
        graph = graph,
        title = currentNode.title,
        onBack = onBack,
        onOpenNode = onOpenNode,
        onPreviewExcerpt = onPreviewExcerpt,
    )
}
