package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeSheetUiState
import com.gleanread.android.feature.knowledge_tree.model.buildMoveNodeBottomSheetUiModel

@Composable
fun MoveNodeBottomSheet(
    snapshot: WorkspaceSnapshot,
    state: MoveNodeSheetUiState,
    rootTitle: String,
    onDismiss: () -> Unit,
    onCreateNode: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToParent: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiModel = remember(snapshot, state, rootTitle) {
        buildMoveNodeBottomSheetUiModel(
            snapshot = snapshot,
            state = state,
            rootTitle = rootTitle,
        )
    } ?: return

    KnowledgeTreeNodePickerBottomSheet(
        title = stringResource(R.string.knowledge_tree_move_to),
        breadcrumbs = uiModel.breadcrumbs,
        destinations = uiModel.destinations,
        helperText = stringResource(
            if (uiModel.confirmEnabled) {
                R.string.knowledge_tree_move_confirm_hint
            } else {
                R.string.knowledge_tree_move_already_here
            },
        ),
        emptyListText = stringResource(R.string.knowledge_tree_move_empty_list),
        confirmEnabled = uiModel.confirmEnabled,
        onDismiss = onDismiss,
        onCreateNode = onCreateNode,
        createNodeContentDescription = stringResource(R.string.knowledge_tree_add_node),
        onConfirm = onConfirm,
        onNavigateToNode = onNavigateToParent,
        modifier = modifier,
    ) {
        MoveNodeTargetCard(targetNodeTitle = uiModel.targetNodeTitle)
    }
}

@Composable
private fun MoveNodeTargetCard(
    targetNodeTitle: String,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = targetNodeTitle,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun MoveNodeBottomSheetPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val uiModel = buildMoveNodeBottomSheetUiModel(
        snapshot = snapshot,
        state = MoveNodeSheetUiState(
            targetNodeId = "node-2",
            targetNodeTitle = "State Management",
            sourceParentNodeId = "node-1",
            currentParentNodeId = "node-1",
        ),
        rootTitle = rootTitle,
    ) ?: return

    GleanReadTheme {
        KnowledgeTreeNodePickerContent(
            title = stringResource(R.string.knowledge_tree_move_to),
            breadcrumbs = uiModel.breadcrumbs,
            destinations = uiModel.destinations,
            helperText = stringResource(R.string.knowledge_tree_move_confirm_hint),
            emptyListText = stringResource(R.string.knowledge_tree_move_empty_list),
            confirmEnabled = true,
            onDismiss = {},
            onCreateNode = {},
            createNodeContentDescription = stringResource(R.string.knowledge_tree_add_node),
            onConfirm = {},
            onNavigateToNode = {},
        ) {
            MoveNodeTargetCard(targetNodeTitle = uiModel.targetNodeTitle)
        }
    }
}
