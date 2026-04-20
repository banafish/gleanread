package com.gleanread.android.feature.knowledge_tree.node_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion

@Composable
fun NodeDetailRoute(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onOpenGraph: () -> Unit,
    onUpdateOutline: (String, String) -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
    onAddExcerpt: () -> Unit,
) {
    val node = snapshot.flatNodes[nodeId] ?: return
    val nodeExcerpts = node.excerptIds.mapNotNull(snapshot.excerptsById::get)
    val backlinks = snapshot.backlinksByNodeId[nodeId].orEmpty()
    var editing by rememberSaveable(nodeId) { mutableStateOf(false) }
    var localOutline by rememberSaveable(nodeId) { mutableStateOf(node.outlineMarkdown) }

    LaunchedEffect(nodeId, node.outlineMarkdown, editing) {
        if (!editing && localOutline != node.outlineMarkdown) {
            localOutline = node.outlineMarkdown
        }
    }

    NodeDetailScreen(
        node = node,
        nodeExcerpts = nodeExcerpts,
        backlinks = backlinks,
        snapshot = snapshot,
        editing = editing,
        localOutline = localOutline,
        searchSuggestions = searchSuggestions,
        onBack = onBack,
        onOpenGraph = onOpenGraph,
        onToggleEditing = {
            if (editing) {
                onUpdateOutline(nodeId, localOutline)
            }
            editing = !editing
        },
        onOutlineChange = { localOutline = it },
        onOpenNode = onOpenNode,
        onPreviewExcerpt = onPreviewExcerpt,
        onAddExcerpt = onAddExcerpt,
    )
}
