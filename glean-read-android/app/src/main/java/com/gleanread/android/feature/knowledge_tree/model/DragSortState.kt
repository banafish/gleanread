package com.gleanread.android.feature.knowledge_tree.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay

/**
 * 拖拽目标位置信息。
 * @param targetIndex 在同级节点列表中的插入位置
 */
data class DropTargetInfo(
    val targetIndex: Int,
)

class DragSortState(
    private val draggedNodeIdProvider: () -> String?,
    private val dragOffsetYProvider: () -> Float,
    private val itemDisplacementsProvider: () -> Map<String, Float>,
    val onDragStart: (String, Offset) -> Unit,
    val onDragMove: (Offset) -> Unit,
    val onDragEnd: () -> Unit,
    val onDragCancel: () -> Unit,
) {
    val draggedNodeId: String? get() = draggedNodeIdProvider()
    val dragOffsetY: Float get() = dragOffsetYProvider()
    val itemDisplacements: Map<String, Float> get() = itemDisplacementsProvider()

    fun isDragging(nodeId: String): Boolean = draggedNodeId == nodeId
    fun itemDisplacement(nodeId: String): Float = itemDisplacements[nodeId] ?: 0f
}

@Composable
fun rememberDragSortState(
    lazyListState: LazyListState,
    nodeIds: List<String>,
    onNodeDragStart: (String) -> Unit = {},
    onNodeDragEnd: (String?, DropTargetInfo?) -> Unit = { _, _ -> },
    onNodeDragCancel: () -> Unit = {},
    firstNodeItemIndex: Int = 0,
): DragSortState {
    val currentOnNodeDragStart by rememberUpdatedState(onNodeDragStart)
    val currentOnNodeDragEnd by rememberUpdatedState(onNodeDragEnd)
    val currentOnNodeDragCancel by rememberUpdatedState(onNodeDragCancel)
    val currentNodeIds by rememberUpdatedState(nodeIds)
    val currentFirstNodeItemIndex by rememberUpdatedState(firstNodeItemIndex)

    var draggedNodeId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dropTarget by remember { mutableStateOf<DropTargetInfo?>(null) }
    // 手指在视口中的 Y 坐标，用于判断自动滚动方向和速度
    var pointerViewportY by remember { mutableFloatStateOf(0f) }

    val currentDragOffsetY by rememberUpdatedState(dragOffsetY)
    val currentPointerViewportY by rememberUpdatedState(pointerViewportY)

    // 根据手指在视口中的位置自动滚动
    LaunchedEffect(draggedNodeId) {
        if (draggedNodeId == null) return@LaunchedEffect
        while (draggedNodeId != null) {
            val pointerY = currentPointerViewportY
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height

            val scrollAmount = when {
                pointerY in 0f..DRAG_AUTO_SCROLL_ZONE ->
                    -DRAG_AUTO_SCROLL_SPEED * (1f - pointerY / DRAG_AUTO_SCROLL_ZONE).coerceIn(0f, 1f)
                pointerY > viewportHeight - DRAG_AUTO_SCROLL_ZONE && pointerY <= viewportHeight ->
                    DRAG_AUTO_SCROLL_SPEED * (1f - (viewportHeight - pointerY) / DRAG_AUTO_SCROLL_ZONE).coerceIn(0f, 1f)
                else -> 0f
            }
            if (scrollAmount != 0f) {
                val consumed = lazyListState.dispatchRawDelta(scrollAmount / 60f)
                // 补偿 dragOffsetY，使被拖拽节点的视觉位置不随列表滚动而移动，保持跟随手指
                dragOffsetY -= consumed
            }
            delay(16)
        }
    }

    // 计算每个非拖拽节点的视觉位移量
    val itemDisplacements by remember(draggedNodeId, dragOffsetY) {
        derivedStateOf {
            if (draggedNodeId != null) {
                calculateItemDisplacements(
                    listState = lazyListState,
                    draggedNodeId = draggedNodeId,
                    nodeIds = currentNodeIds,
                    dragOffsetY = dragOffsetY,
                    firstNodeItemIndex = currentFirstNodeItemIndex,
                )
            } else {
                emptyMap()
            }
        }
    }

    return remember(lazyListState) {
        DragSortState(
            draggedNodeIdProvider = { draggedNodeId },
            dragOffsetYProvider = { dragOffsetY },
            itemDisplacementsProvider = { itemDisplacements },
            onDragStart = { nodeId, startOffset ->
                draggedNodeId = nodeId
                dragOffsetY = 0f
                dropTarget = null
                // 计算手指在视口中的初始 Y：节点在视口中的 offset + 触摸点在节点内的 Y 偏移
                val draggedNodeIndex = currentNodeIds.indexOf(nodeId)
                if (draggedNodeIndex >= 0) {
                    val draggedGlobalIndex = draggedNodeIndex + currentFirstNodeItemIndex
                    val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == draggedGlobalIndex }
                    if (draggedItemInfo != null) {
                        pointerViewportY = draggedItemInfo.offset + startOffset.y
                    }
                }
                currentOnNodeDragStart(nodeId)
            },
            onDragMove = { offset ->
                dragOffsetY += offset.y
                // 手指在视口中移动了，更新视口 Y
                pointerViewportY += offset.y
                dropTarget = calculateDropTarget(
                    listState = lazyListState,
                    draggedNodeId = draggedNodeId,
                    nodeIds = currentNodeIds,
                    dragOffsetY = dragOffsetY,
                    firstNodeItemIndex = currentFirstNodeItemIndex,
                )
            },
            onDragEnd = {
                currentOnNodeDragEnd(draggedNodeId, dropTarget)
                draggedNodeId = null
                dragOffsetY = 0f
                pointerViewportY = 0f
                dropTarget = null
            },
            onDragCancel = {
                currentOnNodeDragCancel()
                draggedNodeId = null
                dragOffsetY = 0f
                pointerViewportY = 0f
                dropTarget = null
            }
        )
    }
}
