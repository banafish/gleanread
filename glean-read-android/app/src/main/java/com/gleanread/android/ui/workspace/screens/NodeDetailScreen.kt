@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.BacklinkType
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
    var localOutline by remember(
        nodeId,
        node.outlineMarkdown
    ) { mutableStateOf(node.outlineMarkdown) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("返回")
            }
            Text(
                node.title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onOpenGraph) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("局部图谱")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                if (editing) onUpdateOutline(nodeId, localOutline)
                editing = !editing
            }) {
                Icon(if (editing) Icons.Default.Save else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (editing) "保存大纲" else "编辑大纲")
            }
        }

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
                    if (snapshot.flatNodes.containsKey(targetId)) onOpenNode(targetId)
                    else if (snapshot.excerptsById.containsKey(targetId)) onPreviewExcerpt(targetId)
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(6.dp))
                Text("节点摘录 (${nodeExcerpts.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            TextButton(onClick = onAddExcerpt) { Text("添加摘录") }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            nodeExcerpts.forEach { excerpt ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        LinkAwareText(
                            rawText = excerpt.content,
                            onLinkClick = { targetId ->
                                if (targetId == excerpt.id) return@LinkAwareText
                                if (snapshot.excerptsById.containsKey(targetId)) onPreviewExcerpt(
                                    targetId
                                )
                                else if (snapshot.flatNodes.containsKey(targetId)) onOpenNode(
                                    targetId
                                )
                            },
                        )
                        if (excerpt.tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                excerpt.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            "#$tag",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "被提及 (Backlinks)",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (backlinks.isEmpty()) {
                    Text("暂无反向链接", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    backlinks.forEach { backlink ->
                        TextButton(onClick = {
                            if (backlink.sourceType == BacklinkType.NODE) onOpenNode(backlink.sourceId) else onPreviewExcerpt(
                                backlink.sourceId
                            )
                        }) {
                            Text(
                                "• ${if (backlink.sourceType == BacklinkType.NODE) "在节点" else "在摘录"} ${backlink.title} 中被引用",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
