package com.gleanread.android.feature.excerpts.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeNodePickerBottomSheet
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeNodePickerUiModel

@Composable
fun AiSummaryMountNodeBottomSheet(
    snapshot: WorkspaceSnapshot,
    currentNodeId: String?,
    selectedTargetNodeId: String?,
    rootTitle: String,
    onDismiss: () -> Unit,
    onCreateNode: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToNode: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiModel = remember(snapshot, currentNodeId, rootTitle) {
        buildKnowledgeTreeNodePickerUiModel(
            snapshot = snapshot,
            currentNodeId = currentNodeId,
            rootTitle = rootTitle,
        )
    } ?: return
    val confirmEnabled = currentNodeId != null && currentNodeId != selectedTargetNodeId

    KnowledgeTreeNodePickerBottomSheet(
        title = stringResource(R.string.ai_summary_mount_title),
        breadcrumbs = uiModel.breadcrumbs,
        destinations = uiModel.destinations,
        helperText = stringResource(
            when {
                currentNodeId == null -> R.string.ai_summary_mount_root_hint
                !confirmEnabled -> R.string.ai_summary_mount_already_here
                else -> R.string.ai_summary_mount_confirm_hint
            },
        ),
        emptyListText = stringResource(R.string.ai_summary_mount_empty_list),
        confirmEnabled = confirmEnabled,
        onDismiss = onDismiss,
        onCreateNode = onCreateNode,
        createNodeContentDescription = stringResource(R.string.knowledge_tree_add_node),
        onConfirm = onConfirm,
        onNavigateToNode = onNavigateToNode,
        modifier = modifier,
    )
}
