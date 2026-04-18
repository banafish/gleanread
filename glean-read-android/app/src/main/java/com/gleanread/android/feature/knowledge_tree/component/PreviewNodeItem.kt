package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
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
            .padding(start = ((node.depth - 2) * 16).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (node.canExpand || node.showEnterBranch) {
                Icon(
                    imageVector = if (node.canExpand && node.isExpanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = if (node.showEnterBranch) "进入面包屑页" else "展开节点",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            if (node.showEnterBranch) {
                                onOpenBranch(node.nodeId)
                            } else {
                                onToggle(node.nodeId)
                            }
                        },
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier
                        .size(8.dp)
                        .padding(start = 10.dp, end = 10.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = node.title,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenDetail(node.nodeId) },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
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
