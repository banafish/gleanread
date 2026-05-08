package com.gleanread.android.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun LiquidGlassTopAppBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val statusBarOverlap = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassTopAppBarBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                topOverflow = statusBarOverlap,
            ),
    ) {
        content()
    }
}

private fun Modifier.liquidGlassTopAppBarBackground(
    backgroundColor: Color,
    topOverflow: Dp,
): Modifier = drawWithCache {
    val topOverflowPx = topOverflow.toPx()
    val top = -topOverflowPx
    val bottom = size.height
    val glassSize = Size(size.width, size.height + topOverflowPx)
    val baseBrush = Brush.verticalGradient(
        colors = listOf(
            backgroundColor.copy(alpha = 0.96f),
            backgroundColor.copy(alpha = 0.90f),
            backgroundColor.copy(alpha = 0.82f),
            backgroundColor.copy(alpha = 0.68f),
        ),
        startY = top,
        endY = bottom,
    )
    val glassSheenBrush = Brush.linearGradient(
        colors = listOf(
            backgroundColor.copy(alpha = 0.92f),
            Color.Transparent,
            backgroundColor.copy(alpha = 0.36f),
        ),
        start = Offset(0f, top),
        end = Offset(size.width, bottom),
    )
    val lowerDepthBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            backgroundColor.copy(alpha = 0.34f),
        ),
        startY = top + glassSize.height * 0.42f,
        endY = bottom,
    )

    onDrawBehind {
        drawRect(brush = baseBrush, topLeft = Offset(0f, top), size = glassSize)
        drawRect(brush = glassSheenBrush, topLeft = Offset(0f, top), size = glassSize)
        drawRect(brush = lowerDepthBrush, topLeft = Offset(0f, top), size = glassSize)
    }
}
