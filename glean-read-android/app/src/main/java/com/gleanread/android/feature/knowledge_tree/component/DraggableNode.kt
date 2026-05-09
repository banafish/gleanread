package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * 可复用的拖拽手势 Modifier，封装长按整张卡片后拖拽检测。
 * 仅用于同级排序移动，不支持跨层级移动。
 */
@Composable
fun Modifier.draggableNode(
    onDragStart: (Offset) -> Unit = {},
    onDragMove: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    enabled: Boolean = true,
): Modifier {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    return if (enabled) {
        this.pointerInput(enabled) {
            detectDragGesturesAfterLongPress(
                onDragStart = { currentOnDragStart(it) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    currentOnDragMove(dragAmount)
                },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragCancel() },
            )
        }
    } else {
        this
    }
}

@Composable
fun Modifier.draggableNodeList(
    dragSortState: DragSortState,
    enabled: Boolean = true,
): Modifier {
    val currentOnDragStart by rememberUpdatedState(dragSortState.onDragStartAtViewportOffset)
    val currentOnDragMove by rememberUpdatedState(dragSortState.onDragMove)
    val currentOnDragEnd by rememberUpdatedState(dragSortState.onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(dragSortState.onDragCancel)
    val currentIsDragInProgress by rememberUpdatedState { dragSortState.isDragInProgress }
    val currentOnDragContainerPositioned by rememberUpdatedState(
        dragSortState.onDragContainerPositioned,
    )

    return if (enabled) {
        this
            .onGloballyPositioned { coordinates ->
                currentOnDragContainerPositioned(coordinates.positionInRoot().y)
            }
            .pointerInput(enabled) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart(it) },
                    onDrag = { change, dragAmount ->
                        if (currentIsDragInProgress()) {
                            change.consume()
                            currentOnDragMove(dragAmount)
                        }
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragCancel() },
                )
            }
    } else {
        this
    }
}

@Composable
fun Modifier.dragSortItemBounds(
    dragSortState: DragSortState,
    nodeId: String,
): Modifier {
    val currentOnDragItemPositioned by rememberUpdatedState(dragSortState.onDragItemPositioned)
    val currentOnDragItemDisposed by rememberUpdatedState(dragSortState.onDragItemDisposed)

    DisposableEffect(nodeId) {
        onDispose { currentOnDragItemDisposed(nodeId) }
    }

    return this.onGloballyPositioned { coordinates ->
        currentOnDragItemPositioned(
            nodeId,
            coordinates.positionInRoot().y,
            coordinates.size.height.toFloat(),
        )
    }
}
