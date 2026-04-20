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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.GraphNodeKind
import com.gleanread.android.core.model.GraphUiModel
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.ui.theme.GleanReadTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier.pointerInput(graph) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.7f, 2.2f)
                panX += pan.x
                panY += pan.y
            }
        },
    ) {
        val density = LocalDensity.current
        val center = with(density) { Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f) }
        val radius = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) * 0.28f }
        val nodePositions = remember(graph, maxWidth, maxHeight) {
            val others = graph.nodes.filterNot { it.kind == GraphNodeKind.CURRENT_NODE }
            val map = mutableMapOf<String, Offset>()
            graph.nodes.firstOrNull { it.kind == GraphNodeKind.CURRENT_NODE }?.let { current ->
                map[current.id] = center
            }
            others.forEachIndexed { index, node ->
                val angle = (2 * PI * index / maxOf(others.size, 1)).toFloat()
                map[node.id] = Offset(
                    x = center.x + radius * cos(angle),
                    y = center.y + radius * sin(angle),
                )
            }
            map
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = panX,
                    translationY = panY,
                ),
        ) {
            graph.edges.forEach { edge ->
                val from = nodePositions[edge.fromId] ?: return@forEach
                val to = nodePositions[edge.toId] ?: return@forEach
                drawLine(color = colors.outlineVariant, start = from, end = to, strokeWidth = 4f)
            }
            graph.nodes.forEach { node ->
                val position = nodePositions[node.id] ?: return@forEach
                drawCircle(
                    color = graphColor(node.kind, colors),
                    radius = if (node.kind == GraphNodeKind.CURRENT_NODE) 44f else 34f,
                    center = position,
                )
            }
        }

        graph.nodes.forEach { node ->
            val base = nodePositions[node.id] ?: return@forEach
            val x = with(density) { base.x * scale + panX - 48.dp.toPx() / 2f }
            val y = with(density) { base.y * scale + panY - 36.dp.toPx() / 2f }
            Box(
                modifier = Modifier
                    .graphicsLayer(translationX = x, translationY = y)
                    .widthIn(min = 96.dp, max = 120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(graphColor(node.kind, colors))
                    .combinedClickable {
                        when (node.kind) {
                            GraphNodeKind.EXCERPT -> onPreviewExcerpt(node.id)
                            GraphNodeKind.CURRENT_NODE -> Unit
                            else -> onOpenNode(node.id)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = node.title,
                    color = colors.surface,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
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
