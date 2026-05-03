package com.gleanread.android.feature.knowledge_tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.gleanread.android.feature.knowledge_tree.component.BreadcrumbBar
import com.gleanread.android.feature.knowledge_tree.component.BranchNodeItem
import com.gleanread.android.feature.knowledge_tree.component.DeleteNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeBranchFab
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchContent
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchTopBar
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeTopBar
import com.gleanread.android.feature.knowledge_tree.component.MoveNodeBottomSheet
import com.gleanread.android.feature.knowledge_tree.component.RenameNodeDialog
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.DropTargetInfo
import com.gleanread.android.feature.knowledge_tree.model.DRAG_AUTO_SCROLL_SPEED
import com.gleanread.android.feature.knowledge_tree.model.DRAG_AUTO_SCROLL_ZONE
import com.gleanread.android.feature.knowledge_tree.model.calculateDropTarget
import com.gleanread.android.feature.knowledge_tree.model.calculateItemDisplacements
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeBranchUiState
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeSheetUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeBranchUiState

@Composable
fun KnowledgeTreeBranchScreen(
    snapshot: WorkspaceSnapshot,
    uiState: KnowledgeTreeBranchUiState,
    isSearchVisible: Boolean,
    searchQuery: String,
    recentQueries: List<String>,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onToggleNode: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onOpenBreadcrumb: (String?) -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onOpenAddChildDialog: (NodeActionTarget) -> Unit,
    onOpenMoveNodeSheet: (NodeActionTarget) -> Unit,
    onOpenRenameDialog: (NodeActionTarget) -> Unit,
    onOpenDeleteDialog: (NodeActionTarget) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenCurrentAddChildDialog: () -> Unit,
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
    val nodeIds = remember(uiState.items) { uiState.items.map { it.nodeId } }

    var localDraggedNodeId by remember { mutableStateOf<String?>(null) }
    var localDragOffsetY by remember { mutableFloatStateOf(0f) }
    var localDropTarget by remember { mutableStateOf<DropTargetInfo?>(null) }
    val currentDragOffsetY by rememberUpdatedState(localDragOffsetY)

    // 根据被拖拽节点的视觉位置自动滚动
    LaunchedEffect(localDraggedNodeId) {
        if (localDraggedNodeId == null) return@LaunchedEffect
        while (localDraggedNodeId != null) {
            val dragNodeId = localDraggedNodeId ?: break
            val dragOffsetY = currentDragOffsetY
            val draggedNodeIndex = nodeIds.indexOf(dragNodeId)
            if (draggedNodeIndex < 0) break

            // 分支页第一个节点 item 的全局 index 是 1（0 是 BreadcrumbBar）
            val draggedGlobalIndex = draggedNodeIndex + 1
            val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == draggedGlobalIndex }
            if (draggedItemInfo != null) {
                val visualTop = draggedItemInfo.offset + dragOffsetY
                val visualBottom = visualTop + draggedItemInfo.size
                val viewportHeight = lazyListState.layoutInfo.viewportSize.height

                val scrollAmount = when {
                    visualTop < DRAG_AUTO_SCROLL_ZONE ->
                        -DRAG_AUTO_SCROLL_SPEED * (1f - visualTop / DRAG_AUTO_SCROLL_ZONE).coerceIn(0f, 1f)
                    visualBottom > viewportHeight - DRAG_AUTO_SCROLL_ZONE ->
                        DRAG_AUTO_SCROLL_SPEED * (1f - (viewportHeight - visualBottom) / DRAG_AUTO_SCROLL_ZONE).coerceIn(0f, 1f)
                    else -> 0f
                }
                if (scrollAmount != 0f) {
                    lazyListState.scroll { scrollBy(scrollAmount / 60f) }
                }
            }
            kotlinx.coroutines.delay(16)
        }
    }

    // 计算每个非拖拽节点的视觉位移量
    val itemDisplacements = remember(localDraggedNodeId, localDragOffsetY) {
        if (localDraggedNodeId != null) {
            calculateItemDisplacements(
                listState = lazyListState,
                draggedNodeId = localDraggedNodeId,
                nodeIds = nodeIds,
                dragOffsetY = localDragOffsetY,
                firstNodeItemIndex = 1,
            )
        } else {
            emptyMap()
        }
    }

    val handleDragMove: (Offset) -> Unit = { offset ->
        localDragOffsetY += offset.y
        val dropTarget = calculateDropTarget(
            listState = lazyListState,
            draggedNodeId = localDraggedNodeId,
            nodeIds = nodeIds,
            dragOffsetY = localDragOffsetY,
            firstNodeItemIndex = 1,
        )
        localDropTarget = dropTarget
    }

    val handleDragStart: (String) -> Unit = { nodeId ->
        localDraggedNodeId = nodeId
        localDragOffsetY = 0f
        localDropTarget = null
        onNodeDragStart(nodeId)
    }

    val handleDragEnd: () -> Unit = {
        onNodeDragEnd(localDraggedNodeId, localDropTarget)
        localDraggedNodeId = null
        localDragOffsetY = 0f
        localDropTarget = null
    }

    val handleDragCancel: () -> Unit = {
        onNodeDragCancel()
        localDraggedNodeId = null
        localDragOffsetY = 0f
        localDropTarget = null
    }

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
                    title = uiState.title,
                    onToggleSearch = onToggleSearch,
                    onExpandAll = onExpandAll,
                    onCollapseAll = onCollapseAll,
                    onBack = onBack,
                )
            }
        },
        floatingActionButton = {
            if (!isSearchVisible) {
                KnowledgeTreeBranchFab(
                    onClick = onOpenCurrentAddChildDialog,
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = KnowledgeTreeListContentPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        BreadcrumbBar(
                            breadcrumbs = uiState.breadcrumbs,
                            onNavigateToBreadcrumb = onOpenBreadcrumb,
                        )
                    }
                    if (uiState.isEmpty) {
                        item {
                            Text(
                                text = stringResource(R.string.knowledge_tree_branch_empty),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        items(uiState.items, key = { it.nodeId }) { node ->
                            val isNodeDragging = localDraggedNodeId == node.nodeId
                            BranchNodeItem(
                                node = node,
                                onToggle = if (localDraggedNodeId != null) { {} } else onToggleNode,
                                onOpenDetail = if (localDraggedNodeId != null) { {} } else onOpenNode,
                                onOpenBranch = if (localDraggedNodeId != null) { {} } else onOpenBranch,
                                onAddChild = onOpenAddChildDialog,
                                onMove = onOpenMoveNodeSheet,
                                onRename = onOpenRenameDialog,
                                onDelete = onOpenDeleteDialog,
                                // 仅同级 depth==1 节点可拖拽
                                onDragStart = if (node.depth == 1) {
                                    { handleDragStart(node.nodeId) }
                                } else {
                                    null
                                },
                                onDragMove = if (node.depth == 1) {
                                    { handleDragMove(it) }
                                } else {
                                    null
                                },
                                onDragEnd = if (node.depth == 1) handleDragEnd else null,
                                onDragCancel = if (node.depth == 1) handleDragCancel else null,
                                isDragging = isNodeDragging,
                                itemDisplacement = itemDisplacements[node.nodeId] ?: 0f,
                                dragOffsetY = if (isNodeDragging) localDragOffsetY else 0f,
                                modifier = Modifier.zIndex(if (isNodeDragging) 1f else 0f),
                            )
                        }
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
private fun KnowledgeTreeBranchScreenPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val uiState = buildKnowledgeTreeBranchUiState(
        snapshot = snapshot,
        nodeId = "node-1",
        expandedIds = setOf("node-2"),
        rootTitle = rootTitle,
    ) ?: return

    GleanReadTheme {
        KnowledgeTreeBranchScreen(
            snapshot = snapshot,
            uiState = uiState,
            isSearchVisible = false,
            searchQuery = "",
            recentQueries = emptyList(),
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {},
            onSearchSubmit = {},
            onToggleNode = {},
            onOpenNode = {},
            onOpenBranch = {},
            onOpenBreadcrumb = {},
            onExpandAll = {},
            onCollapseAll = {},
            onOpenAddChildDialog = {},
            onOpenMoveNodeSheet = {},
            onOpenRenameDialog = {},
            onOpenDeleteDialog = {},
            isRefreshing = false,
            onRefresh = {},
            onOpenCurrentAddChildDialog = {},
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
