package com.gleanread.android.feature.knowledge_tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.ui.motion.fabLaunchDialogMotion
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
import com.gleanread.android.feature.knowledge_tree.component.dragCallbacksFor
import com.gleanread.android.feature.knowledge_tree.component.ignoreDuringDrag
import com.gleanread.android.feature.knowledge_tree.component.rememberDragSortState
import com.gleanread.android.feature.knowledge_tree.component.visualStateFor
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.DropTargetInfo
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeHomeUiState
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeSheetUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeHomeUiState

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
    val onToggleNodeWhenIdle = dragSortState.ignoreDuringDrag(onToggleNode)
    val onOpenNodeWhenIdle = dragSortState.ignoreDuringDrag(onOpenNode)
    val onOpenBranchWhenIdle = dragSortState.ignoreDuringDrag(onOpenBranch)

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
        val topAppBarPadding = innerPadding.calculateTopPadding()

        WorkspacePullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicatorTopPadding = topAppBarPadding,
        ) {
            if (showSearchResults) {
                KnowledgeTreeSearchContent(
                    modifier = Modifier.fillMaxSize(),
                    snapshot = snapshot,
                    query = searchQuery,
                    recentQueries = recentQueries,
                    rootTitle = rootTitle,
                    contentPadding = PaddingValues(top = topAppBarPadding),
                    onQueryChange = onSearchQueryChange,
                    onSearchSubmit = onSearchSubmit,
                    onOpenNode = onOpenNode,
                )
            } else if (uiState.isEmpty) {
                KnowledgeTreeEmptyState(
                    onAddRootNode = onOpenAddRootDialog,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topAppBarPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = knowledgeTreeListContentPadding(topAppBarPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.rootCards, key = { it.nodeId }) { card ->
                        val dragVisualState = dragSortState.visualStateFor(card.nodeId)
                        val dragCallbacks = dragSortState.dragCallbacksFor(card.nodeId)
                        RootNodeCard(
                            card = card,
                            onToggle = onToggleNodeWhenIdle,
                            onOpenDetail = onOpenNodeWhenIdle,
                            onOpenBranch = onOpenBranchWhenIdle,
                            onAddChild = onOpenAddChildDialog,
                            onMove = onOpenMoveNodeSheet,
                            onRename = onOpenRenameDialog,
                            onDelete = onOpenDeleteDialog,
                            onDragStart = dragCallbacks.onDragStart,
                            onDragMove = dragCallbacks.onDragMove,
                            onDragEnd = dragCallbacks.onDragEnd,
                            onDragCancel = dragCallbacks.onDragCancel,
                            isDragging = dragVisualState.isDragging,
                            itemDisplacement = dragVisualState.itemDisplacement,
                            dragOffsetY = dragVisualState.dragOffsetY,
                            modifier = Modifier.zIndex(dragVisualState.zIndex),
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
                modifier = Modifier.fabLaunchDialogMotion(),
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
