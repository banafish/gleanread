@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.gleanread.android.feature.knowledge_tree.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.GraphNodeKind
import com.gleanread.android.core.model.GraphUiModel
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.ui.theme.GleanReadTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val CurrentGraphNodeWidth = 96.dp
private val CurrentGraphNodeMinHeight = 56.dp
private val SupportingGraphNodeWidth = 120.dp
private val SupportingGraphNodeMinHeight = 72.dp
private val GraphNodeContentPadding = 24.dp
private val GraphSectorAngles = listOf(
    (-PI / 2f).toFloat(),
    (-5f * PI / 6f).toFloat(),
    (-PI / 6f).toFloat(),
    (5f * PI / 6f).toFloat(),
    (PI / 6f).toFloat(),
    (PI / 2f).toFloat(),
)

@Composable
fun GraphScreen(
    graph: GraphUiModel,
    title: String,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
            ),
            windowInsets = WindowInsets(0),
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            GraphCanvas(
                graph = graph,
                modifier = Modifier.fillMaxSize(),
                onOpenNode = onOpenNode,
                onPreviewExcerpt = onPreviewExcerpt,
            )
        }
    }
}

@Composable
private fun GraphCanvas(
    graph: GraphUiModel,
    modifier: Modifier = Modifier,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val currentNodeSize = remember(density) {
            GraphNodeSizePx(
                width = with(density) { CurrentGraphNodeWidth.toPx() },
                height = with(density) { CurrentGraphNodeMinHeight.toPx() },
            )
        }
        val supportingNodeSize = remember(density) {
            GraphNodeSizePx(
                width = with(density) { SupportingGraphNodeWidth.toPx() },
                height = with(density) { SupportingGraphNodeMinHeight.toPx() },
            )
        }
        val graphPaddingPx = with(density) { GraphNodeContentPadding.toPx() }
        val currentNodeRadiusPx = with(density) { 28.dp.toPx() }
        val supportingNodeRadiusPx = with(density) { 22.dp.toPx() }
        val edgeStrokeWidthPx = with(density) { 2.dp.toPx() }
        val nodePositions = remember(
            graph,
            viewportWidthPx,
            viewportHeightPx,
            currentNodeSize,
            supportingNodeSize,
            graphPaddingPx,
        ) {
            calculateNodePositions(
                graph = graph,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                currentNodeSize = currentNodeSize,
                supportingNodeSize = supportingNodeSize,
                contentPaddingPx = graphPaddingPx,
            )
        }
        val graphBounds = remember(graph, nodePositions, currentNodeSize, supportingNodeSize) {
            calculateGraphBounds(
                graph = graph,
                nodePositions = nodePositions,
                currentNodeSize = currentNodeSize,
                supportingNodeSize = supportingNodeSize,
            )
        }
        val initialViewport = remember(graphBounds, viewportWidthPx, viewportHeightPx, graphPaddingPx) {
            calculateInitialViewportTransform(
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                graphBounds = graphBounds,
                contentPaddingPx = graphPaddingPx,
            )
        }
        var scale by remember(graph, viewportWidthPx, viewportHeightPx, graphBounds) {
            mutableFloatStateOf(initialViewport.scale)
        }
        var panX by remember(graph, viewportWidthPx, viewportHeightPx, graphBounds) {
            mutableFloatStateOf(initialViewport.panX)
        }
        var panY by remember(graph, viewportWidthPx, viewportHeightPx, graphBounds) {
            mutableFloatStateOf(initialViewport.panY)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(graph) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.65f, 2.2f)
                        panX += pan.x
                        panY += pan.y
                    }
                },
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = panX,
                        translationY = panY,
                        transformOrigin = TransformOrigin(0f, 0f),
                    ),
            ) {
                graph.edges.forEach { edge ->
                    val from = nodePositions[edge.fromId] ?: return@forEach
                    val to = nodePositions[edge.toId] ?: return@forEach
                    drawLine(
                        color = colors.outlineVariant,
                        start = from,
                        end = to,
                        strokeWidth = edgeStrokeWidthPx,
                    )
                }
                graph.nodes.forEach { node ->
                    val position = nodePositions[node.id] ?: return@forEach
                    drawCircle(
                        color = graphColor(node.kind, colors),
                        radius = if (node.kind == GraphNodeKind.CURRENT_NODE) {
                            currentNodeRadiusPx
                        } else {
                            supportingNodeRadiusPx
                        },
                        center = position,
                    )
                }
            }

            graph.nodes.forEach { node ->
                val base = nodePositions[node.id] ?: return@forEach
                val chipWidth = graphNodeWidth(node.kind)
                val chipMinHeight = graphNodeMinHeight(node.kind)
                val chipWidthPx = with(density) { chipWidth.toPx() }
                val chipMinHeightPx = with(density) { chipMinHeight.toPx() }
                val x = base.x * scale + panX - chipWidthPx / 2f
                val y = base.y * scale + panY - chipMinHeightPx / 2f
                Box(
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = x,
                            translationY = y,
                            scaleX = scale,
                            scaleY = scale,
                            transformOrigin = TransformOrigin(0.5f, 0.5f),
                        )
                        .width(chipWidth)
                        .defaultMinSize(minHeight = chipMinHeight)
                        .clip(RoundedCornerShape(18.dp))
                        .background(graphColor(node.kind, colors))
                        .combinedClickable {
                            when (node.kind) {
                                GraphNodeKind.EXCERPT -> onPreviewExcerpt(node.id)
                                GraphNodeKind.CURRENT_NODE -> Unit
                                else -> onOpenNode(node.id)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = node.title,
                        color = graphLabelColor(node.kind, colors),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun graphNodeWidth(kind: GraphNodeKind): Dp {
    return if (kind == GraphNodeKind.CURRENT_NODE) {
        CurrentGraphNodeWidth
    } else {
        SupportingGraphNodeWidth
    }
}

private fun graphNodeMinHeight(kind: GraphNodeKind): Dp {
    return if (kind == GraphNodeKind.CURRENT_NODE) {
        CurrentGraphNodeMinHeight
    } else {
        SupportingGraphNodeMinHeight
    }
}

private fun graphColor(
    kind: GraphNodeKind,
    colors: ColorScheme,
): Color {
    return when (kind) {
        GraphNodeKind.CURRENT_NODE -> colors.primary
        GraphNodeKind.LINKED_NODE -> colors.tertiary
        GraphNodeKind.BACKLINK_NODE -> colors.secondary
        GraphNodeKind.EXCERPT -> colors.primaryContainer
    }
}

private fun graphLabelColor(
    kind: GraphNodeKind,
    colors: ColorScheme,
): Color {
    return when (kind) {
        GraphNodeKind.CURRENT_NODE -> colors.onPrimary
        GraphNodeKind.LINKED_NODE -> colors.onTertiary
        GraphNodeKind.BACKLINK_NODE -> colors.onSecondary
        GraphNodeKind.EXCERPT -> colors.onPrimaryContainer
    }
}

private fun calculateNodePositions(
    graph: GraphUiModel,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    currentNodeSize: GraphNodeSizePx,
    supportingNodeSize: GraphNodeSizePx,
    contentPaddingPx: Float,
): Map<String, Offset> {
    val currentNode = graph.nodes.firstOrNull { it.kind == GraphNodeKind.CURRENT_NODE } ?: return emptyMap()
    val center = Offset(x = viewportWidthPx / 2f, y = viewportHeightPx / 2f)
    val others = graph.nodes.filterNot { it.kind == GraphNodeKind.CURRENT_NODE }
    val nodePositions = mutableMapOf(currentNode.id to center)
    if (others.isEmpty()) {
        return nodePositions
    }

    val occupiedRects = mutableListOf(center.toRect(currentNodeSize).inflate(contentPaddingPx * 0.4f))
    val baseRadius = currentNodeSize.diagonal / 2f + supportingNodeSize.diagonal / 2f + contentPaddingPx * 1.8f
    val ringSpacing = supportingNodeSize.height + contentPaddingPx * 1.6f

    others.forEachIndexed { index, node ->
        val position = findAvailableNodePosition(
            preferredSectorIndex = index % GraphSectorAngles.size,
            center = center,
            occupiedRects = occupiedRects,
            nodeSize = supportingNodeSize,
            baseRadius = baseRadius,
            ringSpacing = ringSpacing,
            contentPaddingPx = contentPaddingPx,
        )
        nodePositions[node.id] = position
        occupiedRects += position.toRect(supportingNodeSize).inflate(contentPaddingPx * 0.4f)
    }

    return nodePositions
}

private fun findAvailableNodePosition(
    preferredSectorIndex: Int,
    center: Offset,
    occupiedRects: List<Rect>,
    nodeSize: GraphNodeSizePx,
    baseRadius: Float,
    ringSpacing: Float,
    contentPaddingPx: Float,
): Offset {
    repeat(12) { layer ->
        val radius = baseRadius + ringSpacing * layer
        repeat(GraphSectorAngles.size) { sectorOffset ->
            val sectorIndex = (preferredSectorIndex + sectorOffset) % GraphSectorAngles.size
            val angle = GraphSectorAngles[sectorIndex]
            val candidate = Offset(
                x = center.x + radius * cos(angle),
                y = center.y + radius * sin(angle),
            )
            val candidateRect = candidate.toRect(nodeSize).inflate(contentPaddingPx * 0.4f)
            if (occupiedRects.none { overlapArea(it, candidateRect) > 0f }) {
                return candidate
            }
        }
    }

    val fallbackRadius = baseRadius + ringSpacing * 12
    val fallbackAngle = GraphSectorAngles[preferredSectorIndex] + preferredSectorIndex * 0.18f
    return Offset(
        x = center.x + fallbackRadius * cos(fallbackAngle),
        y = center.y + fallbackRadius * sin(fallbackAngle),
    )
}

private fun calculateGraphBounds(
    graph: GraphUiModel,
    nodePositions: Map<String, Offset>,
    currentNodeSize: GraphNodeSizePx,
    supportingNodeSize: GraphNodeSizePx,
): Rect {
    val bounds = graph.nodes.mapNotNull { node ->
        nodePositions[node.id]?.toRect(
            if (node.kind == GraphNodeKind.CURRENT_NODE) currentNodeSize else supportingNodeSize,
        )
    }
    return bounds.reduce { acc, rect -> acc.union(rect) }
}

private fun calculateInitialViewportTransform(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    graphBounds: Rect,
    contentPaddingPx: Float,
): GraphViewportTransform {
    val paddedBounds = graphBounds.inflate(contentPaddingPx)
    val scale = min(
        1f,
        min(
            viewportWidthPx / paddedBounds.width.coerceAtLeast(1f),
            viewportHeightPx / paddedBounds.height.coerceAtLeast(1f),
        ),
    ) * 0.96f
    return GraphViewportTransform(
        scale = scale,
        panX = (viewportWidthPx - paddedBounds.width * scale) / 2f - paddedBounds.left * scale,
        panY = (viewportHeightPx - paddedBounds.height * scale) / 2f - paddedBounds.top * scale,
    )
}

private fun Rect.union(other: Rect): Rect {
    return Rect(
        left = min(left, other.left),
        top = min(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom),
    )
}

private fun Offset.toRect(size: GraphNodeSizePx): Rect {
    return Rect(
        left = x - size.width / 2f,
        top = y - size.height / 2f,
        right = x + size.width / 2f,
        bottom = y + size.height / 2f,
    )
}

private fun overlapArea(first: Rect, second: Rect): Float {
    val overlapWidth = min(first.right, second.right) - max(first.left, second.left)
    val overlapHeight = min(first.bottom, second.bottom) - max(first.top, second.top)
    return overlapWidth.coerceAtLeast(0f) * overlapHeight.coerceAtLeast(0f)
}

private val GraphNodeSizePx.diagonal: Float
    get() = sqrt(width * width + height * height)

private data class GraphViewportTransform(
    val scale: Float,
    val panX: Float,
    val panY: Float,
)
private data class GraphNodeSizePx(
    val width: Float,
    val height: Float,
)

@Preview(showBackground = true)
@Composable
private fun GraphScreenPreview() {
    GleanReadTheme {
        GraphScreen(
            graph = WorkspacePreviewData.snapshot().graphByNodeId.getValue("node-1"),
            title = "Compose Architecture",
            onBack = {},
            onOpenNode = {},
            onPreviewExcerpt = {},
        )
    }
}

