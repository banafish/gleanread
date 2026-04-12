@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.ExcerptUiModel
import com.gleanread.android.ui.CaptureUI
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun FeedRoute(
    uiState: WorkspaceUiState,
    onOpenAiSummary: () -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onLoadSample: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    if (uiState.snapshot.isEmpty) {
        EmptyStateRoute(
            onLoadSample = onLoadSample,
            onStartRecording = onStartRecording,
        )
        return
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showInboxOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(uiState.snapshot.excerpts, searchQuery, showInboxOnly) {
        uiState.snapshot.excerpts.filter { excerpt ->
            val matchesQuery = searchQuery.isBlank() || excerpt.content.contains(
                searchQuery,
                ignoreCase = true
            ) || excerpt.thought.contains(
                searchQuery,
                ignoreCase = true
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).height(56.dp),
                    placeholder = { Text("搜索摘录、标签或想法...", color = CaptureUI.Slate400) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = CaptureUI.Slate400
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(percent = 50),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = CaptureUI.Slate100,
                        focusedBorderColor = CaptureUI.Indigo200,
                    )
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (showInboxOnly) CaptureUI.Indigo50 else Color.White)
                        .border(
                            1.dp,
                            if (showInboxOnly) CaptureUI.Indigo200 else CaptureUI.Slate100,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showInboxOnly = !showInboxOnly },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = rememberVectorPainter(
                            ImageVector.Builder(
                                name = "Funnel", defaultWidth = 24.dp, defaultHeight = 24.dp,
                                viewportWidth = 24f, viewportHeight = 24f
                            ).apply {
                                path(
                                    stroke = SolidColor(if (showInboxOnly) CaptureUI.Indigo600 else CaptureUI.Slate500),
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
                        contentDescription = "Filter",
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "长按卡片进入多选并触发 AI 整理",
                fontSize = 12.sp,
                color = CaptureUI.Slate400,
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F1FF)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "✨ AI推荐",
                        color = CaptureUI.Purple500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "你最近收集了 5 篇关于个人知识管理的摘录，是否需要 AI 为你生成总结并创建节点？",
                        color = CaptureUI.Slate700,
                        fontSize = 14.sp,
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📚", fontSize = 42.sp)
                Spacer(Modifier.height(12.dp))
                Text("欢迎来到 GleanRead", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "先把摘录收进 Inbox，再用知识树和 AI 提炼把碎片整理成体系。",
                    textAlign = TextAlign.Center,
                    color = CaptureUI.Slate600,
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 摘录流：收集、搜索与多选整理", color = CaptureUI.Slate700)
                    Text("• 知识树：分层组织你的主题节点", color = CaptureUI.Slate700)
                    Text("• FAB：随时闪记，本地保存并标记待同步", color = CaptureUI.Slate700)
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
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF6F7FF) else Color.White,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CaptureUI.Indigo200 else CaptureUI.Slate100,
        ),
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
                        StatusBadge(text = "未归档", bg = Color(0xFFFFF3D8), fg = Color(0xFFD97706))
                    } else {
                        StatusBadge(
                            text = excerpt.archivedNodeTitle,
                            bg = CaptureUI.Slate100,
                            fg = CaptureUI.Slate500
                        )
                    }
                    excerpt.tags.take(3).forEach { tag ->
                        Text("#$tag", color = CaptureUI.Slate400, fontSize = 12.sp)
                    }
                }
                if (isSelectionMode) {
                    Text(if (isSelected) "✅" else "⚪", fontSize = 18.sp)
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
            )
            if (excerpt.thought.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF9DB)) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        LinkAwareText(
                            rawText = excerpt.thought,
                            onLinkClick = { targetId ->
                                if (targetId == excerpt.id) return@LinkAwareText
                                if (targetId.startsWith("excerpt-")) onPreviewExcerpt(targetId) else onOpenNode(
                                    targetId
                                )
                            },
                        )
                    }
                }
            }
            if (!excerpt.sourceTitle.isNullOrBlank() || !excerpt.url.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "🔗 ${excerpt.sourceTitle ?: excerpt.url}",
                    color = CaptureUI.Slate400,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
