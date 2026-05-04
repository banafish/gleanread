package com.gleanread.android.feature.knowledge_tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.ui.sync.WorkspacePullToRefreshBox
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.feature.knowledge_tree.component.AddNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.DeleteNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeEmptyState
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeHomeFab
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchContent
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchTopBar
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeTopBar
import com.gleanread.android.feature.knowledge_tree.component.MoveNodeBottomSheet
import com.gleanread.android.feature.knowledge_tree.component.RenameNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.RootNodeCard
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.DropTargetInfo
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeHomeUiState
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeSheetUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeHomeUiState
import com.gleanread.android.feature.knowledge_tree.model.rememberDragSortState

@Composable
fun KnowledgeTreeHomeScreen(
    snapshot: WorkspaceSnapshot,
    uiState: KnowledgeTreeHomeUiState,
    isSearchVisible: Boolean,
    searchQuery: String,
    recentQueries: List<String>,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onToggleNode: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onOpenAddRootDialog: () -> Unit,
    onOpenAddChildDialog: (NodeActionTarget) -> Unit,
    onOpenMoveNodeSheet: (NodeActionTarget) -> Unit,
    onOpenRenameDialog: (NodeActionTarget) -> Unit,
    onOpenDeleteDialog: (NodeActionTarget) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    nodeDialogState: NodeDialogUiState?,
    onNodeDialogValueChange: (String) -> Unit,
    onDismissNodeDialog: () -> Unit,
    onConfirmNodeDialog: () -> Unit,
    deleteDialogState: DeleteDialogUiState?,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteDialog: () -> Unit,
    moveNodeSheetState: MoveNodeSheetUiState?,
    onDismissMoveNodeSheet: () -> Unit,
    onMoveNodeSheetNavigate: (String?) -> Unit,
    onOpenMoveNodeCreateDialog: () -> Unit,
    onConfirmMoveNodeSheet: () -> Unit,
    isDragging: Boolean = false,
    onNodeDragStart: (String) -> Unit = {},
    onNodeDragEnd: (String?, DropTargetInfo?) -> Unit = { _, _ -> },
    onNodeDragCancel: () -> Unit = {},
) {
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val showSearchResults = isSearchVisible && searchQuery.trim().isNotEmpty()
    val lazyListState = rememberLazyListState()
    val nodeIds = remember(uiState.rootCards) { uiState.rootCards.map { it.nodeId } }

    val dragSortState = rememberDragSortState(
        lazyListState = lazyListState,
        nodeIds = nodeIds,
        onNodeDragStart = onNodeDragStart,
        onNodeDragEnd = onNodeDragEnd,
        onNodeDragCancel = onNodeDragCancel,
        firstNodeItemIndex = 0,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (isSearchVisible) {
                KnowledgeTreeSearchTopBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = onToggleSearch,
                )
            } else {
                KnowledgeTreeTopBar(
                    title = rootTitle,
                    onToggleSearch = onToggleSearch,
                    onExpandAll = onExpandAll,
                    onCollapseAll = onCollapseAll,
                    showTreeIcon = true,
                )
            }
        },
        floatingActionButton = {
            if (!isSearchVisible) {
                KnowledgeTreeHomeFab(
                    onClick = onOpenAddRootDialog,
                    modifier = Modifier.padding(bottom = KnowledgeTreeFabBottomPadding),
                )
            }
        },
    ) { innerPadding ->
        WorkspacePullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (showSearchResults) {
                KnowledgeTreeSearchContent(
                    modifier = Modifier.fillMaxSize(),
                    snapshot = snapshot,
                    query = searchQuery,
                    recentQueries = recentQueries,
                    rootTitle = rootTitle,
                    onQueryChange = onSearchQueryChange,
                    onSearchSubmit = onSearchSubmit,
                    onOpenNode = onOpenNode,
                )
            } else if (uiState.isEmpty) {
                KnowledgeTreeEmptyState(
                    onAddRootNode = onOpenAddRootDialog,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = KnowledgeTreeListContentPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.rootCards, key = { it.nodeId }) { card ->
                        val isCardDragging = dragSortState.isDragging(card.nodeId)
                        RootNodeCard(
                            card = card,
                            onToggle = if (dragSortState.draggedNodeId != null) { {} } else onToggleNode,
                            onOpenDetail = if (dragSortState.draggedNodeId != null) { {} } else onOpenNode,
                            onOpenBranch = if (dragSortState.draggedNodeId != null) { {} } else onOpenBranch,
                            onAddChild = onOpenAddChildDialog,
                            onMove = onOpenMoveNodeSheet,
                            onRename = onOpenRenameDialog,
                            onDelete = onOpenDeleteDialog,
                            onDragStart = { offset -> dragSortState.onDragStart(card.nodeId, offset) },
                            onDragMove = dragSortState.onDragMove,
                            onDragEnd = dragSortState.onDragEnd,
                            onDragCancel = dragSortState.onDragCancel,
                            isDragging = isCardDragging,
                            itemDisplacement = dragSortState.itemDisplacement(card.nodeId),
                            dragOffsetY = if (isCardDragging) dragSortState.dragOffsetY else 0f,
                            modifier = Modifier.zIndex(if (isCardDragging) 1f else 0f),
                        )
                    }
                }
            }
        }
    }

    nodeDialogState?.let { state ->
        when (state.type) {
            NodeDialogType.ADD_ROOT,
            NodeDialogType.ADD_CHILD,
            -> AddNodeDialog(
                state = state,
                onValueChange = onNodeDialogValueChange,
                onDismiss = onDismissNodeDialog,
                onConfirm = onConfirmNodeDialog,
            )

            NodeDialogType.RENAME -> RenameNodeDialog(
                state = state,
                onValueChange = onNodeDialogValueChange,
                onDismiss = onDismissNodeDialog,
                onConfirm = onConfirmNodeDialog,
            )
        }
    }

    deleteDialogState?.let { state ->
        DeleteNodeDialog(
            state = state,
            onDismiss = onDismissDeleteDialog,
            onConfirm = onConfirmDeleteDialog,
        )
    }

    moveNodeSheetState?.let { state ->
        MoveNodeBottomSheet(
            snapshot = snapshot,
            state = state,
            rootTitle = rootTitle,
            onDismiss = onDismissMoveNodeSheet,
            onCreateNode = onOpenMoveNodeCreateDialog,
            onConfirm = onConfirmMoveNodeSheet,
            onNavigateToParent = onMoveNodeSheetNavigate,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KnowledgeTreeHomeScreenPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    val expandedIds = snapshot.treeRoots.map { it.id }.toSet() + setOf("node-2")

    GleanReadTheme {
        KnowledgeTreeHomeScreen(
            snapshot = snapshot,
            uiState = buildKnowledgeTreeHomeUiState(snapshot, expandedIds),
            isSearchVisible = false,
            searchQuery = "",
            recentQueries = listOf("Compose"),
            onToggleSearch = {},
            onSearchQueryChange = {},
            onSearchSubmit = {},
            onToggleNode = {},
            onOpenNode = {},
            onOpenBranch = {},
            onExpandAll = {},
            onCollapseAll = {},
            onOpenAddRootDialog = {},
            onOpenAddChildDialog = {},
            onOpenMoveNodeSheet = {},
            onOpenRenameDialog = {},
            onOpenDeleteDialog = {},
            isRefreshing = false,
            onRefresh = {},
            nodeDialogState = null,
            onNodeDialogValueChange = {},
            onDismissNodeDialog = {},
            onConfirmNodeDialog = {},
            deleteDialogState = null,
            onDismissDeleteDialog = {},
            onConfirmDeleteDialog = {},
            moveNodeSheetState = null,
            onDismissMoveNodeSheet = {},
            onMoveNodeSheetNavigate = {},
            onOpenMoveNodeCreateDialog = {},
            onConfirmMoveNodeSheet = {},
        )
    }
}
