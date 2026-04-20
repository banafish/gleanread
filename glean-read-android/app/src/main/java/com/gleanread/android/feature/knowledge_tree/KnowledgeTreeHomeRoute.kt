package com.gleanread.android.feature.knowledge_tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.KNOWLEDGE_TREE_HOME_PREVIEW_DEPTH
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeHomeUiState

@Composable
fun KnowledgeTreeHomeRoute(
    snapshot: WorkspaceSnapshot,
    onOpenNode: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onCreateRootNode: (String, (String) -> Unit) -> Unit,
    onCreateChildNode: (String, String, (String) -> Unit) -> Unit,
    onRenameNode: (String, String, () -> Unit) -> Unit,
    onDeleteNode: (String, () -> Unit) -> Unit,
) {
    var expandedIds by rememberSaveable(stateSaver = ExpandedIdsSaver) { mutableStateOf(emptySet<String>()) }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var recentQueries by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var nodeDialogState by rememberSaveable(stateSaver = NodeDialogUiStateSaver) {
        mutableStateOf<NodeDialogUiState?>(null)
    }
    var deleteDialogState by rememberSaveable(stateSaver = DeleteDialogUiStateSaver) {
        mutableStateOf<DeleteDialogUiState?>(null)
    }

    val rootIds = remember(snapshot.treeRoots) { snapshot.treeRoots.map { it.id } }
    LaunchedEffect(rootIds) {
        expandedIds = if (rootIds.isEmpty()) {
            emptySet()
        } else if (expandedIds.isEmpty()) {
            rootIds.toSet()
        } else {
            expandedIds.intersect(snapshot.flatNodes.keys)
        }
    }

    val uiState = remember(snapshot, expandedIds) {
        buildKnowledgeTreeHomeUiState(snapshot, expandedIds)
    }

    KnowledgeTreeHomeScreen(
        snapshot = snapshot,
        uiState = uiState,
        isSearchVisible = isSearchVisible,
        searchQuery = searchQuery,
        recentQueries = recentQueries,
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
        onToggleNode = { nodeId ->
            expandedIds = if (expandedIds.contains(nodeId)) {
                expandedIds - nodeId
            } else {
                expandedIds + nodeId
            }
        },
        onOpenNode = onOpenNode,
        onOpenBranch = onOpenBranch,
        onExpandAll = { expandedIds = collectExpandableIds(snapshot) },
        onCollapseAll = { expandedIds = emptySet() },
        onOpenAddRootDialog = {
            nodeDialogState = NodeDialogUiState(type = NodeDialogType.ADD_ROOT)
        },
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
        nodeDialogState = nodeDialogState,
        onNodeDialogValueChange = { value ->
            nodeDialogState = nodeDialogState?.copy(inputValue = value)
        },
        onDismissNodeDialog = { nodeDialogState = null },
        onConfirmNodeDialog = {
            when (val dialogState = nodeDialogState) {
                null -> Unit
                else -> when (dialogState.type) {
                    NodeDialogType.ADD_ROOT -> {
                        onCreateRootNode(dialogState.inputValue) { createdId ->
                            if (createdId.isNotBlank()) {
                                expandedIds = expandedIds + createdId
                            }
                        }
                    }

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
            deleteDialogState?.target?.nodeId?.let { nodeId ->
                onDeleteNode(nodeId) {}
            }
            deleteDialogState = null
        },
    )
}

private fun collectExpandableIds(
    snapshot: WorkspaceSnapshot,
): Set<String> {
    return buildSet {
        snapshot.treeRoots.forEach { root ->
            val rootNode = snapshot.flatNodes[root.id] ?: return@forEach
            if (rootNode.childNodeIds.isNotEmpty()) {
                add(rootNode.id)
            }
            addAll(
                collectExpandablePreviewIds(
                    snapshot = snapshot,
                    nodeIds = rootNode.childNodeIds,
                    remainingPreviewDepth = KNOWLEDGE_TREE_HOME_PREVIEW_DEPTH,
                ),
            )
        }
    }
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
