package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 可复用的拖拽手势 Modifier，封装长按后拖拽检测。
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

/**
 * 拖拽目标位置的水平指示线。
 */
@Composable
fun DropIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
            ),
    )
}
