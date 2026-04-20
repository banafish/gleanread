@file:OptIn(ExperimentalMaterial3Api::class)

package com.gleanread.android.feature.knowledge_tree.node_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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

@Composable
fun NodeDetailScreen(
    node: FlatNodeUiModel,
    nodeExcerpts: List<ExcerptUiModel>,
    backlinks: List<BacklinkUiModel>,
    editing: Boolean,
    localOutline: String,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onOpenGraph: () -> Unit,
    onToggleEditing: () -> Unit,
    onOutlineChange: (String) -> Unit,
    onOpenLinkedTarget: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
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
                    TextButton(onClick = onOpenGraph) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.node_detail_open_graph))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
                windowInsets = WindowInsets(0),
            )
        }

        item(key = "outline_action") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onToggleEditing) {
                    Icon(
                        imageVector = if (editing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            if (editing) R.string.node_detail_save_outline
                            else R.string.node_detail_edit_outline,
                        ),
                    )
                }
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.node_detail_excerpt_header, nodeExcerpts.size),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                TextButton(onClick = onAddExcerpt) {
                    Text(stringResource(R.string.node_detail_add_excerpt))
                }
            }
        }

        items(nodeExcerpts, key = { it.id }) { excerpt ->
            NodeExcerptCard(
                excerpt = excerpt,
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

@Composable
private fun NodeExcerptCard(
    excerpt: ExcerptUiModel,
    onOpenLinkedTarget: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
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
            searchSuggestions = { emptyList() },
            onBack = {},
            onOpenGraph = {},
            onToggleEditing = {},
            onOutlineChange = {},
            onOpenLinkedTarget = {},
            onOpenNode = {},
            onPreviewExcerpt = {},
            onAddExcerpt = {},
        )
    }
}

