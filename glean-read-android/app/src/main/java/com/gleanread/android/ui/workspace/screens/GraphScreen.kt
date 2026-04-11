@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.GraphNodeKind
import com.gleanread.android.data.model.GraphUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.ui.CaptureUI
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GraphRoute(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    val graph = snapshot.graphByNodeId[nodeId] ?: return
    val currentNode = snapshot.flatNodes[nodeId] ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text(currentNode.title, fontWeight = FontWeight.Bold)
            Text("局部图谱")
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CaptureUI.Slate50),
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
fun GraphCanvas(
    graph: GraphUiModel,
    modifier: Modifier = Modifier,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier.pointerInput(graph) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.7f, 2.2f)
                panX += pan.x
                panY += pan.y
            }
        }) {
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

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale, scaleY = scale, translationX = panX, translationY = panY
                )
        ) {
            graph.edges.forEach { edge ->
                val from = nodePositions[edge.fromId] ?: return@forEach
                val to = nodePositions[edge.toId] ?: return@forEach
                drawLine(color = CaptureUI.Slate300(), start = from, end = to, strokeWidth = 4f)
            }
            graph.nodes.forEach { node ->
                val position = nodePositions[node.id] ?: return@forEach
                drawCircle(
                    color = graphColor(node.kind),
                    radius = if (node.kind == GraphNodeKind.CURRENT_NODE) 44f else 34f,
                    center = position
                )
            }
        }

        graph.nodes.forEach { node ->
            val base = nodePositions[node.id] ?: return@forEach
            val localDensity = LocalDensity.current
            val x = with(localDensity) { base.x * scale + panX - 48.dp.toPx() / 2f }
            val y = with(localDensity) { base.y * scale + panY - 36.dp.toPx() / 2f }
            Box(
                modifier = Modifier
                    .graphicsLayer(translationX = x, translationY = y)
                    .widthIn(min = 96.dp, max = 120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(graphColor(node.kind))
                    .combinedClickable {
                        when (node.kind) {
                            GraphNodeKind.EXCERPT -> onPreviewExcerpt(node.id)
                            GraphNodeKind.CURRENT_NODE -> Unit
                            else -> onOpenNode(node.id)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    node.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

fun CaptureUI.Slate300(): Color = Color(0xFFCBD5E1)

fun graphColor(kind: GraphNodeKind): Color {
    return when (kind) {
        GraphNodeKind.CURRENT_NODE -> CaptureUI.Indigo600
        GraphNodeKind.LINKED_NODE -> CaptureUI.Purple500
        GraphNodeKind.BACKLINK_NODE -> Color(0xFF2563EB)
        GraphNodeKind.EXCERPT -> Color(0xFF0EA5E9)
    }
}
