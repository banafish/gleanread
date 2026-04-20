package com.gleanread.android.feature.excerpts.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.feature.capture.component.NodePickerOverlay

@Composable
fun AiSummaryRoute(
    snapshot: WorkspaceSnapshot,
    draft: AiSummaryDraft,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onSelectTargetNode: (String?) -> Unit,
    onSelectParentNode: (String?) -> Unit,
    onMarkdownChange: (String) -> Unit,
    onNewNodeTitleChange: (String) -> Unit,
) {
    var showNodePicker by rememberSaveable { mutableStateOf(false) }
    var createNewNode by rememberSaveable { mutableStateOf(false) }
    val selectedExcerpts = draft.selectedExcerptIds.mapNotNull(snapshot.excerptsById::get)
    val selectedNodeTitle = draft.targetNodeId?.let { snapshot.flatNodes[it]?.title }

    AiSummaryScreen(
        draft = draft,
        selectedExcerpts = selectedExcerpts,
        selectedNodeTitle = selectedNodeTitle,
        searchSuggestions = searchSuggestions,
        onClose = onClose,
        onSave = onSave,
        onOpenNodePicker = { showNodePicker = true },
        onMarkdownChange = onMarkdownChange,
    )

    if (showNodePicker) {
        NodePickerOverlay(
            snapshot = snapshot,
            createNewNode = createNewNode,
            draftTitle = draft.newNodeTitle,
            selectedTargetNodeId = draft.targetNodeId,
            selectedParentNodeId = draft.parentNodeId,
            onDismiss = { showNodePicker = false },
            onToggleCreate = { createNewNode = !createNewNode },
            onSelectTarget = {
                createNewNode = false
                onSelectTargetNode(it)
                showNodePicker = false
            },
            onSelectParent = {
                createNewNode = true
                onSelectTargetNode(null)
                onSelectParentNode(it)
            },
            onTitleChange = onNewNodeTitleChange,
        )
    }
}
