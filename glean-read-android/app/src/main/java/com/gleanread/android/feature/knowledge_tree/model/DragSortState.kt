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
    val onDragStart: (String) -> Unit,
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

    val currentDragOffsetY by rememberUpdatedState(dragOffsetY)

    // 根据被拖拽节点的视觉位置自动滚动
    LaunchedEffect(draggedNodeId) {
        if (draggedNodeId == null) return@LaunchedEffect
        while (draggedNodeId != null) {
            val dragNodeId = draggedNodeId ?: break
            val currentOffsetY = currentDragOffsetY
            val currentIds = currentNodeIds
            val currentFirstIndex = currentFirstNodeItemIndex

            val draggedNodeIndex = currentIds.indexOf(dragNodeId)
            if (draggedNodeIndex < 0) break

            val draggedGlobalIndex = draggedNodeIndex + currentFirstIndex
            val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == draggedGlobalIndex }

            if (draggedItemInfo != null) {
                val visualTop = draggedItemInfo.offset + currentOffsetY
                val visualBottom = visualTop + draggedItemInfo.size
                val viewportHeight = lazyListState.layoutInfo.viewportSize.height

                val scrollAmount = when {
                    visualTop < DRAG_AUTO_SCROLL_ZONE ->
                        -DRAG_AUTO_SCROLL_SPEED * (1f - visualTop / DRAG_AUTO_SCROLL_ZONE).coerceIn(0f, 1f)
                    visualBottom > viewportHeight - DRAG_AUTO_SCROLL_ZONE ->
                        DRAG_AUTO_SCROLL_SPEED * (1f - (viewportHeight - visualBottom) / DRAG_AUTO_SCROLL_ZONE).coerceIn(0f, 1f)
                    else -> 0f
                }
                if (scrollAmount != 0f) {
                    lazyListState.dispatchRawDelta(scrollAmount / 60f)
                }
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
            onDragStart = { nodeId ->
                draggedNodeId = nodeId
                dragOffsetY = 0f
                dropTarget = null
                currentOnNodeDragStart(nodeId)
            },
            onDragMove = { offset ->
                dragOffsetY += offset.y
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
                dropTarget = null
            },
            onDragCancel = {
                currentOnNodeDragCancel()
                draggedNodeId = null
                dragOffsetY = 0f
                dropTarget = null
            }
        )
    }
}
