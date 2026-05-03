package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.feature.knowledge_tree.model.BranchNodeUiModel
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDestination

@Composable
fun BranchNodeItem(
    node: BranchNodeUiModel,
    onToggle: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onAddChild: (NodeActionTarget) -> Unit,
    onMove: (NodeActionTarget) -> Unit,
    onRename: (NodeActionTarget) -> Unit,
    onDelete: (NodeActionTarget) -> Unit,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    isDragging: Boolean = false,
    itemDisplacement: Float = 0f,
    dragOffsetY: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "dragScale",
    )
    val animatedDisplacement by animateFloatAsState(
        targetValue = itemDisplacement,
        label = "itemDisplacement",
    )

    if (node.depth == 1) {
        // depth==1 的节点：整张卡片可拖拽
        Card(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationY = if (isDragging) dragOffsetY else animatedDisplacement
                }
                .draggableNode(
                    onDragStart = { onDragStart?.invoke(it) },
                    onDragMove = { onDragMove?.invoke(it) },
                    onDragEnd = { onDragEnd?.invoke() },
                    onDragCancel = { onDragCancel?.invoke() },
                    enabled = onDragStart != null,
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 8.dp else 1.dp,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                BranchNodeRow(
                    node = node,
                    onToggle = onToggle,
                    onOpenDetail = onOpenDetail,
                    onOpenBranch = onOpenBranch,
                    onAddChild = onAddChild,
                    onMove = onMove,
                    onRename = onRename,
                    onDelete = onDelete,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    titleFontWeight = FontWeight.SemiBold,
                )

                if (node.isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        // 卡片内子节点不可单独拖拽，不传拖拽回调
                        node.visibleChildren.forEach { child ->
                            BranchNodeItem(
                                node = child,
                                onToggle = onToggle,
                                onOpenDetail = onOpenDetail,
                                onOpenBranch = onOpenBranch,
                                onAddChild = onAddChild,
                                onMove = onMove,
                                onRename = onRename,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // depth>1 的子节点：不可拖拽，无位移效果
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ((node.depth - 1) * 16).dp),
    ) {
        BranchNodeRow(
            node = node,
            onToggle = onToggle,
            onOpenDetail = onOpenDetail,
            onOpenBranch = onOpenBranch,
            onAddChild = onAddChild,
            onMove = onMove,
            onRename = onRename,
            onDelete = onDelete,
            titleStyle = MaterialTheme.typography.bodyMedium,
        )

        if (node.isExpanded) {
            node.visibleChildren.forEach { child ->
                BranchNodeItem(
                    node = child,
                    onToggle = onToggle,
                    onOpenDetail = onOpenDetail,
                    onOpenBranch = onOpenBranch,
                    onAddChild = onAddChild,
                    onMove = onMove,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun BranchNodeRow(
    node: BranchNodeUiModel,
    onToggle: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenBranch: (String) -> Unit,
    onAddChild: (NodeActionTarget) -> Unit,
    onMove: (NodeActionTarget) -> Unit,
    onRename: (NodeActionTarget) -> Unit,
    onDelete: (NodeActionTarget) -> Unit,
    titleStyle: TextStyle,
    titleFontWeight: FontWeight? = null,
) {
    val expandContentDescription = stringResource(
        if (node.showEnterBranch) {
            R.string.knowledge_tree_enter_branch
        } else {
            R.string.knowledge_tree_expand_node
        },
    )
    val countText = stringResource(R.string.knowledge_tree_count, node.count)

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
            contentDescription = expandContentDescription,
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
            style = titleStyle,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            fontWeight = titleFontWeight,
        )
        Text(
            text = countText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        NodeActionMenu(
            onEnterBranch = { onOpenBranch(node.nodeId) },
            onAddChild = { onAddChild(node.actionTarget) },
            onMove = { onMove(node.actionTarget) },
            onRename = { onRename(node.actionTarget) },
            onDelete = { onDelete(node.actionTarget) },
        )
    }
}
