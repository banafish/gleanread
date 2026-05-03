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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.RootNodeCardUiModel

@Composable
fun RootNodeCard(
    card: RootNodeCardUiModel,
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
    val expandContentDescription = stringResource(R.string.knowledge_tree_expand_node)
    val countText = stringResource(R.string.knowledge_tree_count, card.count)
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "dragScale",
    )
    // 非拖拽节点：被其他节点的拖拽挤开时的位移量
    val animatedDisplacement by animateFloatAsState(
        targetValue = itemDisplacement,
        label = "itemDisplacement",
    )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                KnowledgeTreeLeadingIndicator(
                    showActionIcon = card.canExpand,
                    isExpanded = card.isExpanded,
                    contentDescription = expandContentDescription,
                    onClick = if (card.canExpand) {
                        { onToggle(card.nodeId) }
                    } else {
                        null
                    },
                )
                KnowledgeTreeNodeTitle(
                    title = card.title,
                    onClick = { onOpenDetail(card.nodeId) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = countText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                NodeActionMenu(
                    onEnterBranch = { onOpenBranch(card.nodeId) },
                    onAddChild = { onAddChild(card.actionTarget) },
                    onMove = { onMove(card.actionTarget) },
                    onRename = { onRename(card.actionTarget) },
                    onDelete = { onDelete(card.actionTarget) },
                )
            }

            if (card.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    // 卡片内子节点不可单独拖拽，不传拖拽回调
                    card.previewItems.forEach { node ->
                        PreviewNodeItem(
                            node = node,
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
}
