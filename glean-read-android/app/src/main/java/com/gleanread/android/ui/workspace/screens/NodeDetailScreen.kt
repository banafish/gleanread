@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.BacklinkUiModel
import com.gleanread.android.data.model.BacklinkType
import com.gleanread.android.data.model.ExcerptUiModel
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.data.model.WorkspaceSnapshot

@Composable
fun NodeDetailRoute(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onOpenGraph: () -> Unit,
    onUpdateOutline: (String, String) -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
    onAddExcerpt: () -> Unit,
) {
    val node = snapshot.flatNodes[nodeId] ?: return
    val nodeExcerpts = node.excerptIds.mapNotNull(snapshot.excerptsById::get)
    val backlinks = snapshot.backlinksByNodeId[nodeId].orEmpty()
    var editing by rememberSaveable(nodeId) { mutableStateOf(false) }
    var localOutline by rememberSaveable(nodeId) { mutableStateOf(node.outlineMarkdown) }

    LaunchedEffect(nodeId, node.outlineMarkdown, editing) {
        if (!editing && localOutline != node.outlineMarkdown) {
            localOutline = node.outlineMarkdown
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        Text("局部图谱")
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
                TextButton(
                    onClick = {
                        if (editing) onUpdateOutline(nodeId, localOutline)
                        editing = !editing
                    },
                ) {
                    Icon(
                        imageVector = if (editing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (editing) "保存大纲" else "编辑大纲")
                }
            }
        }

        item(key = "outline_body") {
            if (editing) {
                InlineLinkEditor(
                    rawText = localOutline,
                    placeholder = "输入节点总结内容",
                    onRawTextChange = { localOutline = it },
                    searchSuggestions = searchSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                )
            } else {
                LinkAwareText(
                    rawText = node.outlineMarkdown,
                    onLinkClick = { targetId ->
                        if (snapshot.flatNodes.containsKey(targetId)) {
                            onOpenNode(targetId)
                        } else if (snapshot.excerptsById.containsKey(targetId)) {
                            onPreviewExcerpt(targetId)
                        }
                    },
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
                        text = "节点摘录 (${nodeExcerpts.size})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                TextButton(onClick = onAddExcerpt) {
                    Text("添加摘录")
                }
            }
        }

        items(
            items = nodeExcerpts,
            key = { it.id },
        ) { excerpt ->
            NodeExcerptCard(
                excerpt = excerpt,
                snapshot = snapshot,
                onOpenNode = onOpenNode,
                onPreviewExcerpt = onPreviewExcerpt,
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
    snapshot: WorkspaceSnapshot,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
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
                    if (snapshot.excerptsById.containsKey(targetId)) {
                        onPreviewExcerpt(targetId)
                    } else if (snapshot.flatNodes.containsKey(targetId)) {
                        onOpenNode(targetId)
                    }
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
                    text = "被提及 (Backlinks)",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (backlinks.isEmpty()) {
                Text(
                    text = "暂无反向链接",
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
                            text = "• ${if (backlink.sourceType == BacklinkType.NODE) "在节点" else "在摘录"} ${backlink.title} 中被引用",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}
