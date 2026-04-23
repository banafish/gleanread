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
    onMoveExcerptToInbox: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenExcerpt: (String) -> Unit,
    onAddExcerpt: () -> Unit,
) {
    val node = snapshot.flatNodes[nodeId] ?: return
    val nodeExcerpts = node.excerptIds.mapNotNull(snapshot.excerptsById::get)
    val backlinks = snapshot.backlinksByNodeId[nodeId].orEmpty()
    val onOpenLinkedTarget: (String) -> Unit = { targetId ->
        when {
            snapshot.flatNodes.containsKey(targetId) -> onOpenNode(targetId)
            snapshot.excerptsById.containsKey(targetId) -> onOpenExcerpt(targetId)
        }
    }
    var editing by rememberSaveable(nodeId) { mutableStateOf(false) }
    var localOutline by rememberSaveable(nodeId) { mutableStateOf(node.outlineMarkdown) }
    var revealedExcerptId by rememberSaveable(nodeId) { mutableStateOf<String?>(null) }

    LaunchedEffect(nodeId, node.outlineMarkdown, editing) {
        if (!editing && localOutline != node.outlineMarkdown) {
            localOutline = node.outlineMarkdown
        }
    }

    NodeDetailScreen(
        node = node,
        nodeExcerpts = nodeExcerpts,
        backlinks = backlinks,
        editing = editing,
        localOutline = localOutline,
        revealedExcerptId = revealedExcerptId,
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
        onOpenLinkedTarget = onOpenLinkedTarget,
        onOpenNode = onOpenNode,
        onOpenExcerpt = onOpenExcerpt,
        onRevealExcerptActions = { revealedExcerptId = it },
        onDismissExcerptActions = { excerptId ->
            if (revealedExcerptId == excerptId) {
                revealedExcerptId = null
            }
        },
        onRemoveExcerptFromNode = { excerptId ->
            if (revealedExcerptId == excerptId) {
                revealedExcerptId = null
            }
            onMoveExcerptToInbox(excerptId)
        },
        onAddExcerpt = onAddExcerpt,
    )
}
