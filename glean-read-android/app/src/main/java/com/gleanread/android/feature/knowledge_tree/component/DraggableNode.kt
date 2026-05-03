package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 可复用的拖拽手势 Modifier，封装长按整张卡片后拖拽检测。
 * 仅用于同级排序移动，不支持跨层级移动。
 */
fun Modifier.draggableNode(
    onDragStart: (Offset) -> Unit = {},
    onDragMove: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    enabled: Boolean = true,
): Modifier = if (enabled) {
    this.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart(it) },
            onDrag = { change, dragAmount ->
                change.consume()
                onDragMove(dragAmount)
            },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragCancel() },
        )
    }
} else {
    this
}
