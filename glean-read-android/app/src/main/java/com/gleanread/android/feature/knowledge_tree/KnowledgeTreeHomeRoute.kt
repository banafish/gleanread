package com.gleanread.android.feature.knowledge_tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.KNOWLEDGE_TREE_HOME_PREVIEW_DEPTH
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
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
    val controller = rememberKnowledgeTreeRouteController(routeKey = "knowledge_tree_home")

    val rootIds = remember(snapshot.treeRoots) { snapshot.treeRoots.map { it.id } }
    LaunchedEffect(rootIds) {
        controller.setExpandedIds(
            if (rootIds.isEmpty()) {
                emptySet()
            } else if (controller.expandedIds.isEmpty()) {
                rootIds.toSet()
            } else {
                controller.expandedIds.intersect(snapshot.flatNodes.keys)
            },
        )
    }

    val uiState = remember(snapshot, controller.expandedIds) {
        buildKnowledgeTreeHomeUiState(snapshot, controller.expandedIds)
    }

    KnowledgeTreeHomeScreen(
        snapshot = snapshot,
        uiState = uiState,
        isSearchVisible = controller.isSearchVisible,
        searchQuery = controller.searchQuery,
        recentQueries = controller.recentQueries,
        onToggleSearch = controller.toggleSearch,
        onSearchQueryChange = controller.updateSearchQuery,
        onSearchSubmit = controller.submitSearch,
        onToggleNode = controller.toggleNode,
        onOpenNode = onOpenNode,
        onOpenBranch = onOpenBranch,
        onExpandAll = { controller.setExpandedIds(collectExpandableIds(snapshot)) },
        onCollapseAll = { controller.setExpandedIds(emptySet()) },
        onOpenAddRootDialog = controller.openAddRootDialog,
        onOpenAddChildDialog = controller.openAddChildDialog,
        onOpenRenameDialog = controller.openRenameDialog,
        onOpenDeleteDialog = { target ->
            controller.openDeleteDialog(
                target,
                countKnowledgeTreeDescendants(snapshot, target.nodeId),
            )
        },
        nodeDialogState = controller.nodeDialogState,
        onNodeDialogValueChange = controller.updateNodeDialogValue,
        onDismissNodeDialog = controller.dismissNodeDialog,
        onConfirmNodeDialog = {
            when (val dialogState = controller.nodeDialogState) {
                null -> Unit
                else -> when (dialogState.type) {
                    NodeDialogType.ADD_ROOT -> {
                        onCreateRootNode(dialogState.inputValue) { createdId ->
                            if (createdId.isNotBlank()) {
                                controller.expandNode(createdId)
                            }
                        }
                    }

                    NodeDialogType.ADD_CHILD -> {
                        dialogState.parentNodeId?.let { parentId ->
                            onCreateChildNode(parentId, dialogState.inputValue) {
                                controller.expandNode(parentId)
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
            controller.dismissNodeDialog()
        },
        deleteDialogState = controller.deleteDialogState,
        onDismissDeleteDialog = controller.dismissDeleteDialog,
        onConfirmDeleteDialog = {
            controller.deleteDialogState?.target?.nodeId?.let { nodeId ->
                onDeleteNode(nodeId) {}
            }
            controller.dismissDeleteDialog()
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
