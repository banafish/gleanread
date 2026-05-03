package com.gleanread.android.feature.knowledge_tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.KNOWLEDGE_TREE_BRANCH_PREVIEW_DEPTH
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeBranchUiState

@Composable
fun KnowledgeTreeBranchRoute(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    onBack: () -> Unit,
    onOpenRoot: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onCreateRootNode: (String, (String) -> Unit) -> Unit,
    onCreateChildNode: (String, String, (String) -> Unit) -> Unit,
    onMoveNode: (String, String?, () -> Unit) -> Unit,
    onMoveNodeToPosition: (String, Int) -> Unit,
    onRenameNode: (String, String, () -> Unit) -> Unit,
    onDeleteNode: (String, () -> Unit) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val controller = rememberKnowledgeTreeRouteController(routeKey = nodeId)
    val hapticFeedback = LocalHapticFeedback.current

    val currentNode = snapshot.flatNodes[nodeId]
    LaunchedEffect(nodeId, currentNode?.childNodeIds) {
        controller.setExpandedIds(collectExpandableIds(snapshot, nodeId))
    }
    LaunchedEffect(snapshot.flatNodes.containsKey(nodeId)) {
        if (!snapshot.flatNodes.containsKey(nodeId)) {
            onBack()
        }
    }
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)

    val uiState = remember(snapshot, nodeId, controller.expandedIds) {
        buildKnowledgeTreeBranchUiState(snapshot, nodeId, controller.expandedIds, rootTitle)
    } ?: return

    KnowledgeTreeBranchScreen(
        snapshot = snapshot,
        uiState = uiState,
        isSearchVisible = controller.isSearchVisible,
        searchQuery = controller.searchQuery,
        recentQueries = controller.recentQueries,
        onBack = onBack,
        onToggleSearch = controller.toggleSearch,
        onSearchQueryChange = controller.updateSearchQuery,
        onSearchSubmit = controller.submitSearch,
        onToggleNode = controller.toggleNode,
        onOpenNode = onOpenNode,
        onOpenBranch = onOpenBranch,
        onOpenBreadcrumb = { breadcrumbNodeId ->
            if (breadcrumbNodeId == null) {
                onOpenRoot()
            } else {
                onOpenBranch(breadcrumbNodeId)
            }
        },
        onExpandAll = { controller.setExpandedIds(collectExpandableIds(snapshot, nodeId)) },
        onCollapseAll = { controller.setExpandedIds(emptySet()) },
        onOpenAddChildDialog = controller.openAddChildDialog,
        onOpenMoveNodeSheet = controller.openMoveNodeSheet,
        onOpenRenameDialog = controller.openRenameDialog,
        onOpenDeleteDialog = { target ->
            controller.openDeleteDialog(
                target,
                countKnowledgeTreeDescendants(snapshot, target.nodeId),
            )
        },
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onOpenCurrentAddChildDialog = {
            controller.openAddChildDialog(uiState.actionTarget)
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
            controller.deleteDialogState?.target?.nodeId?.let { targetId ->
                onDeleteNode(targetId) {}
            }
            controller.dismissDeleteDialog()
        },
        moveNodeSheetState = controller.moveNodeSheetState,
        onDismissMoveNodeSheet = controller.dismissMoveNodeSheet,
        onMoveNodeSheetNavigate = controller.updateMoveNodeSheetParent,
        onOpenMoveNodeCreateDialog = {
            openMoveSheetCreateNodeDialog(
                snapshot = snapshot,
                state = controller.moveNodeSheetState,
                openAddRootDialog = controller.openAddRootDialog,
                openAddChildDialog = controller.openAddChildDialog,
            )
        },
        onConfirmMoveNodeSheet = {
            controller.moveNodeSheetState?.let { moveState ->
                onMoveNode(moveState.targetNodeId, moveState.currentParentNodeId) {
                    moveState.currentParentNodeId?.let { parentNodeId ->
                        controller.expandNode(parentNodeId)
                    }
                }
            }
            controller.dismissMoveNodeSheet()
        },
        isDragging = controller.isDragging,
        onNodeDragStart = { dragNodeId ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            controller.onDragStart(dragNodeId)
        },
        onNodeDragEnd = { draggedId, dropTarget ->
            if (draggedId != null && dropTarget != null) {
                onMoveNodeToPosition(draggedId, dropTarget.targetIndex)
            }
            controller.onDragEnd()
        },
        onNodeDragCancel = controller.onDragCancel,
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
