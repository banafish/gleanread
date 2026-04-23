package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.ui.component.CaptureBottomSheet
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeBreadcrumbUiModel
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeNodePickerDestinationUiModel
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeNodePickerUiModel

@Composable
fun KnowledgeTreeNodePickerBottomSheet(
    title: String,
    breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    destinations: List<KnowledgeTreeNodePickerDestinationUiModel>,
    helperText: String,
    emptyListText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToNode: (String?) -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    onCreateNode: (() -> Unit)? = null,
    createNodeContentDescription: String? = null,
    headlineContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier.fillMaxWidth(),
    ) {
        KnowledgeTreeNodePickerContent(
            title = title,
            breadcrumbs = breadcrumbs,
            destinations = destinations,
            helperText = helperText,
            emptyListText = emptyListText,
            confirmEnabled = confirmEnabled,
            onDismiss = onDismiss,
            onCreateNode = onCreateNode,
            createNodeContentDescription = createNodeContentDescription,
            onConfirm = onConfirm,
            onNavigateToNode = onNavigateToNode,
            headlineContent = headlineContent,
        )
    }
}

@Composable
fun KnowledgeTreeNodePickerContent(
    title: String,
    breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    destinations: List<KnowledgeTreeNodePickerDestinationUiModel>,
    helperText: String,
    emptyListText: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onCreateNode: (() -> Unit)?,
    createNodeContentDescription: String?,
    onConfirm: () -> Unit,
    onNavigateToNode: (String?) -> Unit,
    modifier: Modifier = Modifier,
    headlineContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            onCreateNode?.let { createNode ->
                IconButton(onClick = createNode) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = createNodeContentDescription,
                    )
                }
            }
            IconButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
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
            headlineContent?.invoke(this)
            BreadcrumbBar(
                breadcrumbs = breadcrumbs,
                onNavigateToBreadcrumb = onNavigateToNode,
            )
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        if (destinations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = emptyListText,
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
                items(destinations, key = { it.nodeId }) { destination ->
                    Card(
                        onClick = { onNavigateToNode(destination.nodeId) },
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
private fun KnowledgeTreeNodePickerContentPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val uiModel = buildKnowledgeTreeNodePickerUiModel(
        snapshot = snapshot,
        currentNodeId = "node-1",
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
        )
    }
}
