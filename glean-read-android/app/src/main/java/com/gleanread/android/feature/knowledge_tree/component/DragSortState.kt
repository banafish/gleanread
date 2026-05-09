package com.gleanread.android.feature.knowledge_tree.component

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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gleanread.android.feature.knowledge_tree.model.DragListItemInfo
import com.gleanread.android.feature.knowledge_tree.model.DropTargetInfo
import com.gleanread.android.feature.knowledge_tree.model.calculateDropTarget
import com.gleanread.android.feature.knowledge_tree.model.calculateItemDisplacements
import kotlin.math.roundToInt

private val DragAutoScrollZone = 48.dp
private val DragAutoScrollSpeed = 2400.dp
private val NoOpNodeAction: (String) -> Unit = {}
private val DisabledDragNodeCallbacks = DragNodeCallbacks()

class DragSortState(
    private val draggedNodeIdProvider: () -> String?,
    private val draggedItemTopProvider: () -> Float?,
    private val dragOffsetYProvider: () -> Float,
    private val itemDisplacementsProvider: () -> Map<String, Float>,
    val onDragStartAtViewportOffset: (Offset) -> Unit,
    val onDragContainerPositioned: (Float) -> Unit,
    val onDragItemPositioned: (String, Float, Float) -> Unit,
    val onDragItemDisposed: (String) -> Unit,
    val onDragStart: (String, Offset) -> Unit,
    val onDragMove: (Offset) -> Unit,
    val onDragEnd: () -> Unit,
    val onDragCancel: () -> Unit,
) {
    val draggedNodeId: String? get() = draggedNodeIdProvider()
    val draggedItemTop: Float? get() = draggedItemTopProvider()
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
    var dragContainerRootY by remember { mutableFloatStateOf(0f) }
    val dragItemBounds = remember { mutableMapOf<String, DragItemBounds>() }

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
                val visibleItems = dragItemBounds.toDragListItems(
                    nodeIds = currentNodeIds,
                    containerRootY = dragContainerRootY,
                )
                val nextSession = session
                    .compensateScroll(consumed)
                    .withDropTarget(
                        visibleItems = visibleItems,
                        nodeIds = currentNodeIds,
                    )
                dragSession = nextSession
            }
        }
    }

    val itemDisplacements by remember {
        derivedStateOf {
            val session = dragSession ?: return@derivedStateOf emptyMap()
            calculateItemDisplacements(
                visibleItems = dragItemBounds.toDragListItems(
                    nodeIds = currentNodeIds,
                    containerRootY = dragContainerRootY,
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
            draggedItemTopProvider = { dragSession?.itemTop },
            dragOffsetYProvider = { dragSession?.dragOffsetY ?: 0f },
            itemDisplacementsProvider = { itemDisplacements },
            onDragStartAtViewportOffset = { startOffset ->
                val pointerRootY = dragContainerRootY + startOffset.y
                val nodeId = findDragNodeIdAt(pointerRootY, dragItemBounds)
                val bounds = nodeId?.let(dragItemBounds::get)
                if (nodeId != null && bounds != null) {
                    val touchY = (pointerRootY - bounds.top).coerceIn(
                        minimumValue = 0f,
                        maximumValue = bounds.height,
                    )
                    dragSession = DragSession(
                        nodeId = nodeId,
                        pointerViewportY = startOffset.y,
                        pointerOffsetInDraggedItemY = touchY,
                        itemSize = bounds.height,
                    )
                    currentOnNodeDragStart(nodeId)
                }
            },
            onDragContainerPositioned = { rootY ->
                dragContainerRootY = rootY
            },
            onDragItemPositioned = { nodeId, rootY, height ->
                dragItemBounds[nodeId] = DragItemBounds(top = rootY, height = height)
            },
            onDragItemDisposed = { nodeId ->
                dragItemBounds.remove(nodeId)
            },
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
                        visibleItems = dragItemBounds.toDragListItems(
                            nodeIds = currentNodeIds,
                            containerRootY = dragContainerRootY,
                        ),
                        nodeIds = currentNodeIds,
                    )
            },
            onDragEnd = {
                val session = dragSession
                if (session != null) {
                    currentOnNodeDragEnd(session.nodeId, session.dropTarget)
                }
                dragSession = null
            },
            onDragCancel = {
                if (dragSession != null) {
                    currentOnNodeDragCancel()
                }
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
    val itemTop: Float
        get() = pointerViewportY - pointerOffsetInDraggedItemY

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
        visibleItems: List<DragListItemInfo>,
        nodeIds: List<String>,
    ): DragSession {
        return copy(
            dropTarget = calculateDropTarget(
                visibleItems = visibleItems,
                draggedNodeId = nodeId,
                nodeIds = nodeIds,
                referenceY = referenceY,
            ),
        )
    }
}

internal data class DragItemBounds(
    val top: Float,
    val height: Float,
) {
    val bottom: Float
        get() = top + height
}

internal fun findDragNodeIdAt(
    pointerRootY: Float,
    itemBounds: Map<String, DragItemBounds>,
): String? {
    return itemBounds.entries
        .firstOrNull { (_, bounds) -> pointerRootY >= bounds.top && pointerRootY <= bounds.bottom }
        ?.key
}

internal fun calculateAutoScrollVelocity(
    pointerY: Float,
    viewportHeight: Float,
    zonePx: Float,
    maxSpeedPxPerSecond: Float,
): Float {
    if (viewportHeight <= 0f || zonePx <= 0f) return 0f
    return when {
        pointerY < zonePx ->
            -maxSpeedPxPerSecond * (1f - pointerY / zonePx).coerceIn(0f, 1f)
        pointerY > viewportHeight - zonePx ->
            maxSpeedPxPerSecond * (1f - (viewportHeight - pointerY) / zonePx).coerceIn(0f, 1f)
        else -> 0f
    }
}

private fun Map<String, DragItemBounds>.toDragListItems(
    nodeIds: List<String>,
    containerRootY: Float,
): List<DragListItemInfo> {
    return nodeIds.mapIndexedNotNull { nodeIndex, nodeId ->
        val bounds = get(nodeId) ?: return@mapIndexedNotNull null
        DragListItemInfo(
            nodeIndex = nodeIndex,
            nodeId = nodeId,
            offset = (bounds.top - containerRootY).roundToInt(),
            size = bounds.height.roundToInt(),
        )
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
