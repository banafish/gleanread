package com.gleanread.android.feature.knowledge_tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gleanread.android.core.model.FlatNodeUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeSheetUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

internal data class KnowledgeTreeRouteController(
    val expandedIds: Set<String>,
    val isSearchVisible: Boolean,
    val searchQuery: String,
    val recentQueries: List<String>,
    val nodeDialogState: NodeDialogUiState?,
    val deleteDialogState: DeleteDialogUiState?,
    val moveNodeSheetState: MoveNodeSheetUiState?,
    val setExpandedIds: (Set<String>) -> Unit,
    val expandNode: (String) -> Unit,
    val toggleNode: (String) -> Unit,
    val toggleSearch: () -> Unit,
    val updateSearchQuery: (String) -> Unit,
    val submitSearch: (String) -> Unit,
    val openAddRootDialog: () -> Unit,
    val openAddChildDialog: (NodeActionTarget) -> Unit,
    val openRenameDialog: (NodeActionTarget) -> Unit,
    val openDeleteDialog: (NodeActionTarget, Int) -> Unit,
    val openMoveNodeSheet: (NodeActionTarget) -> Unit,
    val updateMoveNodeSheetParent: (String?) -> Unit,
    val updateNodeDialogValue: (String) -> Unit,
    val dismissNodeDialog: () -> Unit,
    val dismissDeleteDialog: () -> Unit,
    val dismissMoveNodeSheet: () -> Unit,
)

@Composable
internal fun rememberKnowledgeTreeRouteController(
    routeKey: Any? = null,
): KnowledgeTreeRouteController {
    var expandedIds by rememberSaveable(routeKey, stateSaver = ExpandedIdsSaver) {
        mutableStateOf(emptySet<String>())
    }
    var isSearchVisible by rememberSaveable(routeKey) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(routeKey) { mutableStateOf("") }
    var recentQueries by rememberSaveable(routeKey) { mutableStateOf(emptyList<String>()) }
    var nodeDialogState by rememberSaveable(routeKey, stateSaver = NodeDialogUiStateSaver) {
        mutableStateOf<NodeDialogUiState?>(null)
    }
    var deleteDialogState by rememberSaveable(routeKey, stateSaver = DeleteDialogUiStateSaver) {
        mutableStateOf<DeleteDialogUiState?>(null)
    }
    var moveNodeSheetState by rememberSaveable(routeKey, stateSaver = MoveNodeSheetUiStateSaver) {
        mutableStateOf<MoveNodeSheetUiState?>(null)
    }

    return KnowledgeTreeRouteController(
        expandedIds = expandedIds,
        isSearchVisible = isSearchVisible,
        searchQuery = searchQuery,
        recentQueries = recentQueries,
        nodeDialogState = nodeDialogState,
        deleteDialogState = deleteDialogState,
        moveNodeSheetState = moveNodeSheetState,
        setExpandedIds = { expandedIds = it },
        expandNode = { nodeId -> expandedIds = expandedIds + nodeId },
        toggleNode = { nodeId ->
            expandedIds = if (expandedIds.contains(nodeId)) {
                expandedIds - nodeId
            } else {
                expandedIds + nodeId
            }
        },
        toggleSearch = {
            isSearchVisible = !isSearchVisible
            if (!isSearchVisible) {
                searchQuery = ""
            }
        },
        updateSearchQuery = { searchQuery = it },
        submitSearch = { query ->
            val trimmed = query.trim()
            if (trimmed.isNotBlank()) {
                recentQueries = listOf(trimmed) + recentQueries.filterNot { it == trimmed }.take(4)
            }
        },
        openAddRootDialog = {
            nodeDialogState = NodeDialogUiState(type = NodeDialogType.ADD_ROOT)
        },
        openAddChildDialog = { target ->
            nodeDialogState = NodeDialogUiState(
                type = NodeDialogType.ADD_CHILD,
                parentNodeId = target.nodeId,
                parentNodeTitle = target.title,
            )
        },
        openRenameDialog = { target ->
            nodeDialogState = NodeDialogUiState(
                type = NodeDialogType.RENAME,
                inputValue = target.title,
                targetNodeId = target.nodeId,
                targetNodeTitle = target.title,
            )
        },
        openDeleteDialog = { target, descendantCount ->
            deleteDialogState = DeleteDialogUiState(
                target = target,
                descendantCount = descendantCount,
            )
        },
        openMoveNodeSheet = { target ->
            moveNodeSheetState = MoveNodeSheetUiState(
                targetNodeId = target.nodeId,
                targetNodeTitle = target.title,
                sourceParentNodeId = target.parentNodeId,
                currentParentNodeId = target.parentNodeId,
            )
        },
        updateMoveNodeSheetParent = { parentNodeId ->
            moveNodeSheetState = moveNodeSheetState?.copy(currentParentNodeId = parentNodeId)
        },
        updateNodeDialogValue = { value ->
            nodeDialogState = nodeDialogState?.copy(inputValue = value)
        },
        dismissNodeDialog = { nodeDialogState = null },
        dismissDeleteDialog = { deleteDialogState = null },
        dismissMoveNodeSheet = { moveNodeSheetState = null },
    )
}

internal fun countKnowledgeTreeDescendants(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
): Int {
    val node = snapshot.flatNodes[nodeId] ?: return 0
    return node.childNodeIds.sumOf { childId ->
        1 + countKnowledgeTreeDescendants(snapshot, childId)
    }
}

internal fun openMoveSheetCreateNodeDialog(
    snapshot: WorkspaceSnapshot,
    state: MoveNodeSheetUiState?,
    openAddRootDialog: () -> Unit,
    openAddChildDialog: (NodeActionTarget) -> Unit,
) {
    if (state == null) return

    val parentNodeId = state.currentParentNodeId
    if (parentNodeId == null) {
        openAddRootDialog()
        return
    }

    snapshot.flatNodes[parentNodeId]?.let { parentNode ->
        openAddChildDialog(parentNode.toActionTarget())
    }
}

private fun FlatNodeUiModel.toActionTarget(): NodeActionTarget {
    return NodeActionTarget(
        nodeId = id,
        title = title,
        childCount = childNodeIds.size,
        parentNodeId = parentId,
    )
}
