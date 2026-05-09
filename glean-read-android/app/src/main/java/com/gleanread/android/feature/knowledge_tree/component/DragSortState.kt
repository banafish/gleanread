package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gleanread.android.feature.knowledge_tree.model.DragListItemInfo
import com.gleanread.android.feature.knowledge_tree.model.DropTargetInfo
import com.gleanread.android.feature.knowledge_tree.model.calculateDropTarget
import com.gleanread.android.feature.knowledge_tree.model.calculateItemDisplacements

private val DragAutoScrollZone = 48.dp
private val DragAutoScrollSpeed = 2400.dp
private val NoOpNodeAction: (String) -> Unit = {}
private val DisabledDragNodeCallbacks = DragNodeCallbacks()

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
    val isDragInProgress: Boolean get() = draggedNodeId != null

    fun isDragging(nodeId: String): Boolean = draggedNodeId == nodeId
    fun itemDisplacement(nodeId: String): Float = itemDisplacements[nodeId] ?: 0f
}

data class DragNodeCallbacks(
    val onDragStart: ((Offset) -> Unit)? = null,
    val onDragMove: ((Offset) -> Unit)? = null,
    val onDragEnd: (() -> Unit)? = null,
    val onDragCancel: (() -> Unit)? = null,
)

data class DragNodeVisualState(
    val isDragging: Boolean,
    val itemDisplacement: Float,
    val dragOffsetY: Float,
    val zIndex: Float,
)

fun DragSortState.dragCallbacksFor(
    nodeId: String,
    enabled: Boolean = true,
): DragNodeCallbacks {
    if (!enabled) return DisabledDragNodeCallbacks
    return DragNodeCallbacks(
        onDragStart = { offset -> onDragStart(nodeId, offset) },
        onDragMove = onDragMove,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
    )
}

fun DragSortState.visualStateFor(nodeId: String): DragNodeVisualState {
    val isDragging = isDragging(nodeId)
    return DragNodeVisualState(
        isDragging = isDragging,
        itemDisplacement = itemDisplacement(nodeId),
        dragOffsetY = if (isDragging) dragOffsetY else 0f,
        zIndex = if (isDragging) 1f else 0f,
    )
}

fun DragSortState.ignoreDuringDrag(action: (String) -> Unit): (String) -> Unit {
    return if (isDragInProgress) NoOpNodeAction else action
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
    val density = LocalDensity.current
    val autoScrollZonePx = with(density) { DragAutoScrollZone.toPx() }
    val autoScrollSpeedPxPerSecond = with(density) { DragAutoScrollSpeed.toPx() }

    var dragSession by remember { mutableStateOf<DragSession?>(null) }

    LaunchedEffect(dragSession?.nodeId, autoScrollZonePx, autoScrollSpeedPxPerSecond) {
        if (dragSession == null) return@LaunchedEffect
        var lastFrameTimeNanos = withFrameNanos { it }
        while (dragSession != null) {
            val frameTimeNanos = withFrameNanos { it }
            val elapsedSeconds = (frameTimeNanos - lastFrameTimeNanos)
                .coerceAtLeast(0L)
                .toFloat() / 1_000_000_000f
            lastFrameTimeNanos = frameTimeNanos

            val session = dragSession ?: break
            val scrollVelocity = calculateAutoScrollVelocity(
                pointerY = session.pointerViewportY,
                viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat(),
                zonePx = autoScrollZonePx,
                maxSpeedPxPerSecond = autoScrollSpeedPxPerSecond,
            )
            if (scrollVelocity != 0f) {
                val consumed = lazyListState.dispatchRawDelta(scrollVelocity * elapsedSeconds)
                val nextSession = session
                    .compensateScroll(consumed)
                    .withDropTarget(
                        lazyListState = lazyListState,
                        nodeIds = currentNodeIds,
                        firstNodeItemIndex = currentFirstNodeItemIndex,
                    )
                dragSession = nextSession
            }
        }
    }

    val itemDisplacements by remember {
        derivedStateOf {
            val session = dragSession ?: return@derivedStateOf emptyMap()
            calculateItemDisplacements(
                visibleItems = lazyListState.toDragListItems(
                    nodeIds = currentNodeIds,
                    firstNodeItemIndex = currentFirstNodeItemIndex,
                ),
                draggedNodeId = session.nodeId,
                nodeIds = currentNodeIds,
                draggedItemSize = session.itemSize,
                dragOffsetY = session.dragOffsetY,
                referenceY = session.referenceY,
            )
        }
    }

    return remember(lazyListState) {
        DragSortState(
            draggedNodeIdProvider = { dragSession?.nodeId },
            dragOffsetYProvider = { dragSession?.dragOffsetY ?: 0f },
            itemDisplacementsProvider = { itemDisplacements },
            onDragStart = { nodeId, startOffset ->
                val draggedItemInfo = lazyListState
                    .toDragListItems(currentNodeIds, currentFirstNodeItemIndex)
                    .firstOrNull { it.nodeId == nodeId }
                if (draggedItemInfo != null) {
                    val itemSize = draggedItemInfo.size.toFloat()
                    val touchY = startOffset.y.coerceIn(0f, itemSize)

                    dragSession = DragSession(
                        nodeId = nodeId,
                        pointerViewportY = draggedItemInfo.offset + touchY,
                        pointerOffsetInDraggedItemY = touchY,
                        itemSize = itemSize,
                    )
                    currentOnNodeDragStart(nodeId)
                }
            },
            onDragMove = { offset ->
                dragSession = dragSession
                    ?.moveBy(offset.y)
                    ?.withDropTarget(
                        lazyListState = lazyListState,
                        nodeIds = currentNodeIds,
                        firstNodeItemIndex = currentFirstNodeItemIndex,
                    )
            },
            onDragEnd = {
                val session = dragSession
                currentOnNodeDragEnd(session?.nodeId, session?.dropTarget)
                dragSession = null
            },
            onDragCancel = {
                currentOnNodeDragCancel()
                dragSession = null
            },
        )
    }
}

private data class DragSession(
    val nodeId: String,
    val dragOffsetY: Float = 0f,
    val pointerViewportY: Float,
    val pointerOffsetInDraggedItemY: Float,
    val itemSize: Float,
    val dropTarget: DropTargetInfo? = null,
) {
    val referenceY: Float
        get() = if (dragOffsetY < 0f) {
            pointerViewportY - pointerOffsetInDraggedItemY
        } else {
            pointerViewportY + (itemSize - pointerOffsetInDraggedItemY)
        }

    fun moveBy(deltaY: Float): DragSession {
        return copy(
            dragOffsetY = dragOffsetY + deltaY,
            pointerViewportY = pointerViewportY + deltaY,
        )
    }

    fun compensateScroll(consumedPx: Float): DragSession {
        return copy(dragOffsetY = dragOffsetY - consumedPx)
    }

    fun withDropTarget(
        lazyListState: LazyListState,
        nodeIds: List<String>,
        firstNodeItemIndex: Int,
    ): DragSession {
        return copy(
            dropTarget = calculateDropTarget(
                visibleItems = lazyListState.toDragListItems(nodeIds, firstNodeItemIndex),
                draggedNodeId = nodeId,
                nodeIds = nodeIds,
                referenceY = referenceY,
            ),
        )
    }
}

private fun calculateAutoScrollVelocity(
    pointerY: Float,
    viewportHeight: Float,
    zonePx: Float,
    maxSpeedPxPerSecond: Float,
): Float {
    if (viewportHeight <= 0f || zonePx <= 0f) return 0f
    return when {
        pointerY in 0f..zonePx ->
            -maxSpeedPxPerSecond * (1f - pointerY / zonePx).coerceIn(0f, 1f)
        pointerY > viewportHeight - zonePx && pointerY <= viewportHeight ->
            maxSpeedPxPerSecond * (1f - (viewportHeight - pointerY) / zonePx).coerceIn(0f, 1f)
        else -> 0f
    }
}

private fun LazyListState.toDragListItems(
    nodeIds: List<String>,
    firstNodeItemIndex: Int,
): List<DragListItemInfo> {
    return layoutInfo.visibleItemsInfo.mapNotNull { itemInfo ->
        val nodeIndex = itemInfo.index - firstNodeItemIndex
        if (nodeIndex < 0) return@mapNotNull null
        val nodeId = nodeIds.getOrNull(nodeIndex) ?: return@mapNotNull null
        DragListItemInfo(
            nodeIndex = nodeIndex,
            nodeId = nodeId,
            offset = itemInfo.offset,
            size = itemInfo.size,
        )
    }
}
