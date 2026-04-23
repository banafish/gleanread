package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gleanread.android.core.ui.component.CaptureBottomSheet
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeBottomSheetUiModel
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

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier.fillMaxWidth()
    ) {
        MoveNodeBottomSheetContent(
            uiModel = uiModel,
            onDismiss = onDismiss,
            onCreateNode = onCreateNode,
            onConfirm = onConfirm,
            onNavigateToParent = onNavigateToParent,
        )
    }
}

@Composable
private fun MoveNodeBottomSheetContent(
    uiModel: MoveNodeBottomSheetUiModel,
    onDismiss: () -> Unit,
    onCreateNode: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToParent: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 0.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close),
                )
            }
            Text(
                text = stringResource(R.string.knowledge_tree_move_to),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCreateNode) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.knowledge_tree_add_node),
                )
            }
            IconButton(
                onClick = onConfirm,
                enabled = uiModel.confirmEnabled,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.common_confirm),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = RoundedCornerShape(24.dp),
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
                        text = uiModel.targetNodeTitle,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            BreadcrumbBar(
                breadcrumbs = uiModel.breadcrumbs,
                onNavigateToBreadcrumb = onNavigateToParent,
            )
            Text(
                text = stringResource(
                    if (uiModel.confirmEnabled) {
                        R.string.knowledge_tree_move_confirm_hint
                    } else {
                        R.string.knowledge_tree_move_already_here
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        if (uiModel.destinations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.knowledge_tree_move_empty_list),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiModel.destinations, key = { it.nodeId }) { destination ->
                    Card(
                        onClick = { onNavigateToParent(destination.nodeId) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = destination.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = nodeSummaryText(
                                        childCount = destination.childCount,
                                        excerptCount = destination.excerptCount,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun nodeSummaryText(
    childCount: Int,
    excerptCount: Int,
): String {
    val parts = buildList {
        if (childCount > 0) {
            add(stringResource(R.string.knowledge_tree_move_children_count, childCount))
        }
        if (excerptCount > 0) {
            add(stringResource(R.string.knowledge_tree_move_excerpt_count, excerptCount))
        }
    }
    return if (parts.isEmpty()) {
        stringResource(R.string.knowledge_tree_move_destination_empty)
    } else {
        parts.joinToString(" · ")
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun MoveNodeBottomSheetPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    val uiModel = buildMoveNodeBottomSheetUiModel(
        snapshot = snapshot,
        state = MoveNodeSheetUiState(
            targetNodeId = "node-2",
            targetNodeTitle = "State Management",
            sourceParentNodeId = "node-1",
            currentParentNodeId = "node-1",
        ),
        rootTitle = "知识体系",
    ) ?: return

    GleanReadTheme {
        MoveNodeBottomSheetContent(
            uiModel = uiModel,
            onDismiss = {},
            onCreateNode = {},
            onConfirm = {},
            onNavigateToParent = {},
        )
    }
}
