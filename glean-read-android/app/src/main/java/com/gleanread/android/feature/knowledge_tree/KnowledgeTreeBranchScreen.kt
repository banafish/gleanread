package com.gleanread.android.feature.knowledge_tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.component.AddNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.BreadcrumbBar
import com.gleanread.android.feature.knowledge_tree.component.BranchNodeItem
import com.gleanread.android.feature.knowledge_tree.component.DeleteNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeBranchFab
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchContent
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchTopBar
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeTopBar
import com.gleanread.android.feature.knowledge_tree.component.RenameNodeDialog
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeBranchUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeBranchUiState
import com.gleanread.android.core.ui.theme.GleanReadTheme

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
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onOpenAddChildDialog: (NodeActionTarget) -> Unit,
    onOpenRenameDialog: (NodeActionTarget) -> Unit,
    onOpenDeleteDialog: (NodeActionTarget) -> Unit,
    onOpenCurrentAddChildDialog: () -> Unit,
    nodeDialogState: NodeDialogUiState?,
    onNodeDialogValueChange: (String) -> Unit,
    onDismissNodeDialog: () -> Unit,
    onConfirmNodeDialog: () -> Unit,
    deleteDialogState: DeleteDialogUiState?,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteDialog: () -> Unit,
) {
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val showSearchResults = isSearchVisible && searchQuery.trim().isNotEmpty()

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
        if (showSearchResults) {
            KnowledgeTreeSearchContent(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = KnowledgeTreeListContentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BreadcrumbBar(titles = uiState.breadcrumbTitles)
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
                        BranchNodeItem(
                            node = node,
                            onToggle = onToggleNode,
                            onOpenDetail = onOpenNode,
                            onOpenBranch = onOpenBranch,
                            onAddChild = onOpenAddChildDialog,
                            onRename = onOpenRenameDialog,
                            onDelete = onOpenDeleteDialog,
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
            onExpandAll = {},
            onCollapseAll = {},
            onOpenAddChildDialog = {},
            onOpenRenameDialog = {},
            onOpenDeleteDialog = {},
            onOpenCurrentAddChildDialog = {},
            nodeDialogState = null,
            onNodeDialogValueChange = {},
            onDismissNodeDialog = {},
            onConfirmNodeDialog = {},
            deleteDialogState = null,
            onDismissDeleteDialog = {},
            onConfirmDeleteDialog = {},
        )
    }
}

