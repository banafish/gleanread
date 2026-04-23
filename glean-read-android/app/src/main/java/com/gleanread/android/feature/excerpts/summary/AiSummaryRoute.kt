package com.gleanread.android.feature.excerpts.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.feature.knowledge_tree.NodeDialogUiStateSaver
import com.gleanread.android.feature.knowledge_tree.component.AddNodeDialog
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

@Composable
fun AiSummaryRoute(
    snapshot: WorkspaceSnapshot,
    draft: AiSummaryDraft,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onCreateRootNode: (String, (String) -> Unit) -> Unit,
    onCreateChildNode: (String, String, (String) -> Unit) -> Unit,
    onSelectTargetNode: (String?) -> Unit,
    onMarkdownChange: (String) -> Unit,
) {
    var showMountNodeSheet by rememberSaveable { mutableStateOf(false) }
    var currentMountNodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var nodeDialogState by rememberSaveable(stateSaver = NodeDialogUiStateSaver) {
        mutableStateOf<NodeDialogUiState?>(null)
    }
    val selectedExcerpts = draft.selectedExcerptIds.mapNotNull(snapshot.excerptsById::get)
    val selectedNodeTitle = draft.targetNodeId?.let { snapshot.flatNodes[it]?.title }
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)

    AiSummaryScreen(
        draft = draft,
        selectedExcerpts = selectedExcerpts,
        selectedNodeTitle = selectedNodeTitle,
        searchSuggestions = searchSuggestions,
        onClose = onClose,
        onSave = onSave,
        onOpenMountNodeSheet = {
            currentMountNodeId = draft.targetNodeId?.takeIf(snapshot.flatNodes::containsKey)
            showMountNodeSheet = true
        },
        onMarkdownChange = onMarkdownChange,
    )

    if (showMountNodeSheet) {
        AiSummaryMountNodeBottomSheet(
            snapshot = snapshot,
            currentNodeId = currentMountNodeId,
            selectedTargetNodeId = draft.targetNodeId,
            rootTitle = rootTitle,
            onDismiss = { showMountNodeSheet = false },
            onCreateNode = {
                val parentNodeId = currentMountNodeId
                nodeDialogState = if (parentNodeId == null) {
                    NodeDialogUiState(type = NodeDialogType.ADD_ROOT)
                } else {
                    snapshot.flatNodes[parentNodeId]?.let { parentNode ->
                        NodeDialogUiState(
                            type = NodeDialogType.ADD_CHILD,
                            parentNodeId = parentNode.id,
                            parentNodeTitle = parentNode.title,
                        )
                    }
                }
            },
            onConfirm = {
                currentMountNodeId?.let { nodeId ->
                    onSelectTargetNode(nodeId)
                    showMountNodeSheet = false
                }
            },
            onNavigateToNode = { currentMountNodeId = it },
        )
    }

    nodeDialogState?.let { state ->
        AddNodeDialog(
            state = state,
            onValueChange = { value ->
                nodeDialogState = nodeDialogState?.copy(inputValue = value)
            },
            onDismiss = { nodeDialogState = null },
            onConfirm = {
                when (val dialogState = nodeDialogState) {
                    null -> Unit
                    else -> when (dialogState.type) {
                        NodeDialogType.ADD_ROOT -> {
                            onCreateRootNode(dialogState.inputValue) {}
                        }

                        NodeDialogType.ADD_CHILD -> {
                            dialogState.parentNodeId?.let { parentNodeId ->
                                onCreateChildNode(parentNodeId, dialogState.inputValue) {}
                            }
                        }

                        NodeDialogType.RENAME -> Unit
                    }
                }
                nodeDialogState = null
            },
        )
    }
}

