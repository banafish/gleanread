package com.gleanread.android.feature.knowledge_tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.component.AddNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.DeleteNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeEmptyState
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeHomeFab
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeSearchContent
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeTopBar
import com.gleanread.android.feature.knowledge_tree.component.RenameNodeDialog
import com.gleanread.android.feature.knowledge_tree.component.RootNodeCard
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.KNOWLEDGE_TREE_ROOT_TITLE
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeHomeUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

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
    onOpenRenameDialog: (NodeActionTarget) -> Unit,
    onOpenDeleteDialog: (NodeActionTarget) -> Unit,
    nodeDialogState: NodeDialogUiState?,
    onNodeDialogValueChange: (String) -> Unit,
    onDismissNodeDialog: () -> Unit,
    onConfirmNodeDialog: () -> Unit,
    deleteDialogState: DeleteDialogUiState?,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteDialog: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            KnowledgeTreeTopBar(
                title = KNOWLEDGE_TREE_ROOT_TITLE,
                onToggleSearch = onToggleSearch,
                onExpandAll = onExpandAll,
                onCollapseAll = onCollapseAll,
                showTreeIcon = true,
            )
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
        if (isSearchVisible) {
            KnowledgeTreeSearchContent(
                snapshot = snapshot,
                query = searchQuery,
                recentQueries = recentQueries,
                onQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                onOpenNode = onOpenNode,
            )
        } else if (uiState.isEmpty) {
            KnowledgeTreeEmptyState(
                onAddRootNode = onOpenAddRootDialog,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = KnowledgeTreeListContentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.rootCards, key = { it.nodeId }) { card ->
                    RootNodeCard(
                        card = card,
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
