package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDestination
import com.gleanread.android.feature.knowledge_tree.model.PreviewNodeUiModel

@Composable
fun PreviewNodeItem(
    node: PreviewNodeUiModel,
    onToggle: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onAddChild: (NodeActionTarget) -> Unit,
    onRename: (NodeActionTarget) -> Unit,
    onDelete: (NodeActionTarget) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ((node.depth - 1) * 16).dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KnowledgeTreeLeadingIndicator(
                showActionIcon = node.canExpand || node.showEnterBranch,
                isExpanded = node.canExpand && node.isExpanded,
                contentDescription = if (node.showEnterBranch) "进入面包屑页" else "展开节点",
                onClick = if (node.canExpand || node.showEnterBranch) {
                    {
                        if (node.showEnterBranch) {
                            onOpenBranch(node.nodeId)
                        } else {
                            onToggle(node.nodeId)
                        }
                    }
                } else {
                    null
                },
            )
            KnowledgeTreeNodeTitle(
                title = node.title,
                onClick = {
                    when (val destination = node.titleDestination) {
                        is NodeDestination.Branch -> onOpenBranch(destination.nodeId)
                        is NodeDestination.Detail -> onOpenDetail(destination.nodeId)
                        NodeDestination.None -> Unit
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Text(
                text = "${node.count}条",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            NodeActionMenu(
                onEnterBranch = { onOpenBranch(node.nodeId) },
                onAddChild = { onAddChild(node.actionTarget) },
                onRename = { onRename(node.actionTarget) },
                onDelete = { onDelete(node.actionTarget) },
            )
        }

        if (node.isExpanded) {
            node.visibleChildren.forEach { child ->
                PreviewNodeItem(
                    node = child,
                    onToggle = onToggle,
                    onOpenDetail = onOpenDetail,
                    onOpenBranch = onOpenBranch,
                    onAddChild = onAddChild,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
    }
}
