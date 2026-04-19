package com.gleanread.android.feature.knowledge_tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gleanread.android.feature.workspace.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.KNOWLEDGE_TREE_BRANCH_PREVIEW_DEPTH
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeBranchUiState

@Composable
fun KnowledgeTreeBranchRoute(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onCreateChildNode: (String, String, (String) -> Unit) -> Unit,
    onRenameNode: (String, String, () -> Unit) -> Unit,
    onDeleteNode: (String, () -> Unit) -> Unit,
) {
    var expandedIds by remember(nodeId) { mutableStateOf(emptySet<String>()) }
    var isSearchVisible by remember(nodeId) { mutableStateOf(false) }
    var searchQuery by remember(nodeId) { mutableStateOf("") }
    var recentQueries by remember(nodeId) { mutableStateOf(emptyList<String>()) }
    var nodeDialogState by remember { mutableStateOf<NodeDialogUiState?>(null) }
    var deleteDialogState by remember { mutableStateOf<DeleteDialogUiState?>(null) }

    val currentNode = snapshot.flatNodes[nodeId]
    LaunchedEffect(nodeId, currentNode?.childNodeIds) {
        expandedIds = collectExpandableIds(snapshot, nodeId)
    }
    LaunchedEffect(snapshot.flatNodes.containsKey(nodeId)) {
        if (!snapshot.flatNodes.containsKey(nodeId)) {
            onBack()
        }
    }

    val uiState = remember(snapshot, nodeId, expandedIds) {
        buildKnowledgeTreeBranchUiState(snapshot, nodeId, expandedIds)
    } ?: return

    KnowledgeTreeBranchScreen(
        snapshot = snapshot,
        uiState = uiState,
        isSearchVisible = isSearchVisible,
        searchQuery = searchQuery,
        recentQueries = recentQueries,
        onBack = onBack,
        onToggleSearch = {
            isSearchVisible = !isSearchVisible
            if (!isSearchVisible) {
                searchQuery = ""
            }
        },
        onSearchQueryChange = { searchQuery = it },
        onSearchSubmit = { query ->
            val trimmed = query.trim()
            if (trimmed.isNotBlank()) {
                recentQueries = listOf(trimmed) + recentQueries.filterNot { it == trimmed }.take(4)
            }
        },
        onToggleNode = { targetId ->
            expandedIds = if (expandedIds.contains(targetId)) {
                expandedIds - targetId
            } else {
                expandedIds + targetId
            }
        },
        onOpenNode = onOpenNode,
        onOpenBranch = onOpenBranch,
        onExpandAll = { expandedIds = collectExpandableIds(snapshot, nodeId) },
        onCollapseAll = { expandedIds = emptySet() },
        onOpenAddChildDialog = { target ->
            nodeDialogState = NodeDialogUiState(
                type = NodeDialogType.ADD_CHILD,
                parentNodeId = target.nodeId,
                parentNodeTitle = target.title,
            )
        },
        onOpenRenameDialog = { target ->
            nodeDialogState = NodeDialogUiState(
                type = NodeDialogType.RENAME,
                inputValue = target.title,
                targetNodeId = target.nodeId,
                targetNodeTitle = target.title,
            )
        },
        onOpenDeleteDialog = { target ->
            deleteDialogState = DeleteDialogUiState(
                target = target,
                descendantCount = countDescendants(snapshot, target.nodeId),
            )
        },
        onOpenCurrentAddChildDialog = {
            nodeDialogState = NodeDialogUiState(
                type = NodeDialogType.ADD_CHILD,
                parentNodeId = uiState.currentNodeId,
                parentNodeTitle = uiState.title,
            )
        },
        nodeDialogState = nodeDialogState,
        onNodeDialogValueChange = { value ->
            nodeDialogState = nodeDialogState?.copy(inputValue = value)
        },
        onDismissNodeDialog = { nodeDialogState = null },
        onConfirmNodeDialog = {
            when (val dialogState = nodeDialogState) {
                null -> Unit
                else -> when (dialogState.type) {
                    NodeDialogType.ADD_ROOT -> Unit
                    NodeDialogType.ADD_CHILD -> {
                        dialogState.parentNodeId?.let { parentId ->
                            onCreateChildNode(parentId, dialogState.inputValue) {
                                expandedIds = expandedIds + parentId
                            }
                        }
                    }

                    NodeDialogType.RENAME -> {
                        dialogState.targetNodeId?.let { targetId ->
                            onRenameNode(targetId, dialogState.inputValue) {}
                        }
                    }
                }
            }
            nodeDialogState = null
        },
        deleteDialogState = deleteDialogState,
        onDismissDeleteDialog = { deleteDialogState = null },
        onConfirmDeleteDialog = {
            deleteDialogState?.target?.nodeId?.let { targetId ->
                onDeleteNode(targetId) {}
            }
            deleteDialogState = null
        },
    )
}

private fun collectExpandableIds(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
): Set<String> {
    return collectExpandablePreviewIds(
        snapshot = snapshot,
        nodeIds = snapshot.flatNodes[nodeId]?.childNodeIds.orEmpty(),
        remainingPreviewDepth = KNOWLEDGE_TREE_BRANCH_PREVIEW_DEPTH,
    )
}

private fun countDescendants(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
): Int {
    val node = snapshot.flatNodes[nodeId] ?: return 0
    return node.childNodeIds.sumOf { childId ->
        1 + countDescendants(snapshot, childId)
    }
}
