@file:OptIn(ExperimentalMaterial3Api::class)

package com.gleanread.android.feature.knowledge_tree.node_detail

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.core.model.BacklinkType
import com.gleanread.android.core.model.BacklinkUiModel
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.model.FlatNodeUiModel
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.core.ui.richtext.InlineLinkEditor
import com.gleanread.android.core.ui.richtext.LinkAwareText
import com.gleanread.android.core.ui.theme.GleanReadTheme
import kotlin.math.roundToInt

@Composable
fun NodeDetailScreen(
    node: FlatNodeUiModel,
    nodeExcerpts: List<ExcerptUiModel>,
    backlinks: List<BacklinkUiModel>,
    editing: Boolean,
    localOutline: String,
    revealedExcerptId: String?,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onOpenGraph: () -> Unit,
    onToggleEditing: () -> Unit,
    onOutlineChange: (String) -> Unit,
    onOpenLinkedTarget: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
    onRevealExcerptActions: (String) -> Unit,
    onDismissExcerptActions: (String) -> Unit,
    onRemoveExcerptFromNode: (String) -> Unit,
    onAddExcerpt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "top_bar") {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = node.title,
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
                actions = {
                    IconButton(onClick = onOpenGraph) {
                        Icon(
                            imageVector = Icons.Default.DeviceHub,
                            contentDescription = stringResource(R.string.node_detail_open_graph),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
                windowInsets = WindowInsets(0),
            )
        }

        item(key = "outline_header") {
            NodeDetailSectionHeader(
                title = stringResource(R.string.node_detail_outline_title),
                icon = Icons.Default.Description,
                action = {
                    IconButton(onClick = onToggleEditing) {
                        Icon(
                            imageVector = if (editing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = stringResource(
                                if (editing) R.string.node_detail_save_outline
                                else R.string.node_detail_edit_outline,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }

        item(key = "outline_body") {
            if (editing) {
                InlineLinkEditor(
                    rawText = localOutline,
                    placeholder = stringResource(R.string.node_detail_outline_placeholder),
                    onRawTextChange = onOutlineChange,
                    searchSuggestions = searchSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                )
            } else {
                LinkAwareText(
                    rawText = node.outlineMarkdown,
                    onLinkClick = onOpenLinkedTarget,
                )
            }
        }

        item(key = "outline_divider") {
            Column {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }
        }

        item(key = "excerpt_header") {
            NodeDetailSectionHeader(
                title = stringResource(R.string.node_detail_excerpt_header, nodeExcerpts.size),
                icon = Icons.Default.AttachFile,
                action = {
                    IconButton(onClick = onAddExcerpt) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.node_detail_add_excerpt),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }

        items(nodeExcerpts, key = { it.id }) { excerpt ->
            NodeExcerptCard(
                excerpt = excerpt,
                isDeleteRevealed = revealedExcerptId == excerpt.id,
                onRevealDelete = { onRevealExcerptActions(excerpt.id) },
                onDismissDelete = { onDismissExcerptActions(excerpt.id) },
                onRemoveExcerptFromNode = { onRemoveExcerptFromNode(excerpt.id) },
                onOpenLinkedTarget = onOpenLinkedTarget,
            )
        }

        item(key = "backlinks") {
            BacklinksCard(
                backlinks = backlinks,
                onOpenNode = onOpenNode,
                onPreviewExcerpt = onPreviewExcerpt,
            )
        }
    }
}

private enum class NodeExcerptSwipeValue {
    Closed,
    DeleteRevealed,
}

@Composable
private fun NodeDetailSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (action != null) {
            action()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeExcerptCard(
    excerpt: ExcerptUiModel,
    isDeleteRevealed: Boolean,
    onRevealDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onRemoveExcerptFromNode: () -> Unit,
    onOpenLinkedTarget: (String) -> Unit,
) {
    val density = LocalDensity.current
    val actionAreaWidthPx = with(density) { NODE_DETAIL_EXCERPT_ACTION_AREA_WIDTH.toPx() }
    val swipeState = remember(actionAreaWidthPx, density) {
        AnchoredDraggableState(
            initialValue = NodeExcerptSwipeValue.Closed,
            anchors = DraggableAnchors {
                NodeExcerptSwipeValue.Closed at 0f
                NodeExcerptSwipeValue.DeleteRevealed at -actionAreaWidthPx
            },
            positionalThreshold = { distance -> distance * 0.35f },
            velocityThreshold = { with(density) { 120.dp.toPx() } },
            animationSpec = tween(durationMillis = 220),
        )
    }
    val offsetX = swipeState.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0

    LaunchedEffect(isDeleteRevealed) {
        val targetValue = if (isDeleteRevealed) {
            NodeExcerptSwipeValue.DeleteRevealed
        } else {
            NodeExcerptSwipeValue.Closed
        }
        if (swipeState.currentValue != targetValue) {
            swipeState.animateTo(targetValue)
        }
    }

    LaunchedEffect(swipeState) {
        snapshotFlow { swipeState.currentValue }.collect { value ->
            if (value == NodeExcerptSwipeValue.DeleteRevealed) {
                onRevealDelete()
            } else {
                onDismissDelete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NodeExcerptCardShape),
    ) {
        NodeExcerptDeleteAction(
            onRemoveExcerptFromNode = onRemoveExcerptFromNode,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, 0) }
                .anchoredDraggable(
                    state = swipeState,
                    orientation = Orientation.Horizontal,
                ),
        ) {
            NodeExcerptCardSurface(
                excerpt = excerpt,
                onOpenLinkedTarget = onOpenLinkedTarget,
            )
        }
    }
}

@Composable
private fun NodeExcerptDeleteAction(
    onRemoveExcerptFromNode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(NODE_DETAIL_EXCERPT_ACTION_BUTTON_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onRemoveExcerptFromNode,
                modifier = Modifier
                    .size(NODE_DETAIL_EXCERPT_ACTION_BUTTON_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.node_detail_remove_excerpt_from_node),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(NODE_DETAIL_EXCERPT_ACTION_ICON_SIZE),
                )
            }
        }
    }
}

@Composable
private fun NodeExcerptCardSurface(
    excerpt: ExcerptUiModel,
    onOpenLinkedTarget: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NodeExcerptCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            LinkAwareText(
                rawText = excerpt.content,
                onLinkClick = { targetId ->
                    if (targetId == excerpt.id) return@LinkAwareText
                    onOpenLinkedTarget(targetId)
                },
            )
            if (excerpt.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    excerpt.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BacklinksCard(
    backlinks: List<BacklinkUiModel>,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.node_detail_backlinks_title),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (backlinks.isEmpty()) {
                Text(
                    text = stringResource(R.string.node_detail_backlinks_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                backlinks.forEach { backlink ->
                    TextButton(
                        onClick = {
                            if (backlink.sourceType == BacklinkType.NODE) {
                                onOpenNode(backlink.sourceId)
                            } else {
                                onPreviewExcerpt(backlink.sourceId)
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(
                                R.string.node_detail_backlink_item,
                                stringResource(
                                    if (backlink.sourceType == BacklinkType.NODE) {
                                        R.string.node_detail_backlink_source_node
                                    } else {
                                        R.string.node_detail_backlink_source_excerpt
                                    },
                                ),
                                backlink.title,
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeDetailScreenPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    GleanReadTheme {
        NodeDetailScreen(
            node = snapshot.flatNodes.getValue("node-1"),
            nodeExcerpts = snapshot.excerpts.filter { it.archivedNodeId == "node-1" },
            backlinks = snapshot.backlinksByNodeId["node-1"].orEmpty(),
            editing = false,
            localOutline = snapshot.flatNodes.getValue("node-1").outlineMarkdown,
            revealedExcerptId = null,
            searchSuggestions = { emptyList() },
            onBack = {},
            onOpenGraph = {},
            onToggleEditing = {},
            onOutlineChange = {},
            onOpenLinkedTarget = {},
            onOpenNode = {},
            onPreviewExcerpt = {},
            onRevealExcerptActions = {},
            onDismissExcerptActions = {},
            onRemoveExcerptFromNode = {},
            onAddExcerpt = {},
        )
    }
}

private val NodeExcerptCardShape = RoundedCornerShape(18.dp)
private val NODE_DETAIL_EXCERPT_ACTION_BUTTON_SIZE = 50.dp
private val NODE_DETAIL_EXCERPT_ACTION_ICON_SIZE = 26.dp
private val NODE_DETAIL_EXCERPT_ACTION_AREA_WIDTH = 72.dp
