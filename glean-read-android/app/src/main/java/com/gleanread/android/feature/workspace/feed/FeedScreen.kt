@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.feature.workspace.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gleanread.android.core.ui.richtext.LinkAwareText
import com.gleanread.android.feature.workspace.model.ExcerptUiModel
import com.gleanread.android.feature.workspace.model.WorkspaceSnapshot

@Composable
fun FeedRoute(
    snapshot: WorkspaceSnapshot,
    uiState: FeedUiState,
    onOpenAiSummary: () -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onLoadSample: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    if (snapshot.isEmpty) {
        EmptyStateRoute(
            onLoadSample = onLoadSample,
            onStartRecording = onStartRecording,
        )
        return
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showInboxOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(snapshot.excerpts, searchQuery, showInboxOnly) {
        snapshot.excerpts.filter { excerpt ->
            val matchesQuery = searchQuery.isBlank() || excerpt.content.contains(
                searchQuery, ignoreCase = true
            ) || excerpt.thought.contains(
                searchQuery, ignoreCase = true
            ) || excerpt.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesFilter = !showInboxOnly || excerpt.archivedNodeId == null
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = {
                        Text(
                            "搜索摘录、标签或想法...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )
                FilledTonalIconButton(
                    onClick = { showInboxOnly = !showInboxOnly },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (showInboxOnly) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (showInboxOnly) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Icon(
                        painter = rememberVectorPainter(
                            ImageVector.Builder(
                                name = "Funnel",
                                defaultWidth = 24.dp,
                                defaultHeight = 24.dp,
                                viewportWidth = 24f,
                                viewportHeight = 24f
                            ).apply {
                                path(
                                    stroke = SolidColor(
                                        if (showInboxOnly) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    strokeLineWidth = 1.5f,
                                    strokeLineCap = StrokeCap.Round,
                                    strokeLineJoin = StrokeJoin.Round
                                ) {
                                    moveTo(4f, 6f)
                                    lineTo(20f, 6f)
                                    lineTo(14f, 13f)
                                    lineTo(14f, 19f)
                                    lineTo(10f, 21f)
                                    lineTo(10f, 13f)
                                    close()
                                }
                            }.build()
                        ),
                        contentDescription = "Filter inbox",
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "长按卡片进入多选并触发 AI 整理",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
        }

        items(items = filtered, key = { it.id }) { excerpt ->
            ExcerptCard(
                excerpt = excerpt,
                isSelectionMode = uiState.isSelectionMode,
                isSelected = uiState.selectedExcerptIds.contains(excerpt.id),
                onLongPress = { onLongPress(excerpt.id) },
                onClick = {
                    if (uiState.isSelectionMode) onToggleSelection(excerpt.id)
                },
                onOpenNode = onOpenNode,
                onPreviewExcerpt = onPreviewExcerpt,
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI推荐",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "AI推荐",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "你最近收集了 5 篇关于个人知识管理的摘录，是否需要 AI 为你生成总结并创建节点？",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {}) { Text("忽略") }
                        Button(onClick = onOpenAiSummary) { Text("一键整理") }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
fun EmptyStateRoute(
    onLoadSample: () -> Unit,
    onStartRecording: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "欢迎来到 GleanRead",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "先把摘录收进 Inbox，再用知识树和 AI 提炼把碎片整理成体系。",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "• 摘录流：收集、搜索与多选整理",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "• 知识树：分层组织你的主题节点",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "• FAB：随时闪记，本地保存并标记待同步",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onLoadSample) { Text("加载示例数据") }
                    TextButton(onClick = onStartRecording) { Text("开始记录") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExcerptCard(
    excerpt: ExcerptUiModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    onOpenNode: (String) -> Unit = {},
    onPreviewExcerpt: (String) -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (excerpt.archivedNodeTitle == null) {
                        StatusBadge(
                            text = "未归档",
                            bg = MaterialTheme.colorScheme.errorContainer,
                            fg = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        StatusBadge(
                            text = excerpt.archivedNodeTitle,
                            bg = MaterialTheme.colorScheme.secondaryContainer,
                            fg = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    excerpt.tags.take(3).forEach { tag ->
                        Text(
                            "#$tag",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinkAwareText(
                rawText = excerpt.content,
                onLinkClick = { targetId ->
                    if (targetId == excerpt.id) return@LinkAwareText
                    if (targetId.startsWith("excerpt-")) onPreviewExcerpt(targetId) else onOpenNode(
                        targetId
                    )
                },
                onClick = onClick,
                onLongClick = onLongPress,
            )
            if (excerpt.thought.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        LinkAwareText(
                            rawText = excerpt.thought,
                            onLinkClick = { targetId ->
                                if (targetId == excerpt.id) return@LinkAwareText
                                if (targetId.startsWith("excerpt-")) onPreviewExcerpt(targetId) else onOpenNode(
                                    targetId
                                )
                            },
                            onClick = onClick,
                            onLongClick = onLongPress,
                        )
                    }
                }
            }
            if (!excerpt.sourceTitle.isNullOrBlank() || !excerpt.url.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${excerpt.sourceTitle ?: excerpt.url}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}
