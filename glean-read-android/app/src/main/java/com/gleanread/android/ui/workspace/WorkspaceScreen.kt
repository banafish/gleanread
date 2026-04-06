@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gleanread.android.data.model.BacklinkType
import com.gleanread.android.data.model.BacklinkUiModel
import com.gleanread.android.data.model.ExcerptUiModel
import com.gleanread.android.data.model.FlatNodeUiModel
import com.gleanread.android.data.model.GraphNodeKind
import com.gleanread.android.data.model.GraphUiModel
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.data.model.TagGroupUiModel
import com.gleanread.android.data.model.TreeNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.data.model.buildInlineAnnotatedString
import com.gleanread.android.data.model.currentInlineQuery
import com.gleanread.android.data.model.insertStructuredLink
import com.gleanread.android.ui.CaptureBottomSheet
import com.gleanread.android.ui.CaptureUI
import com.gleanread.android.ui.TagPill
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private object WorkspaceRoutes {
    const val Feed = "feed"
    const val Tree = "tree"
    const val Tags = "tags"
    const val AiSummary = "ai-summary"
    const val NodePattern = "node/{nodeId}"
    const val GraphPattern = "graph/{nodeId}"

    fun node(nodeId: String) = "node/$nodeId"
    fun graph(nodeId: String) = "graph/$nodeId"
}

@Composable
fun WorkspaceApp(
    workspaceViewModel: WorkspaceViewModel = viewModel(
        factory = WorkspaceViewModel.factory(LocalContext.current.applicationContext as Context)
    ),
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: WorkspaceRoutes.Feed
    val uiState by workspaceViewModel.uiState.collectAsState()
    var previewExcerptId by rememberSaveable { mutableStateOf<String?>(null) }

    val isMainRoute = route == WorkspaceRoutes.Feed || route == WorkspaceRoutes.Tree || route == WorkspaceRoutes.Tags
    val showEmptyGuide = route == WorkspaceRoutes.Feed && uiState.snapshot.isEmpty
    val showFab = isMainRoute && !uiState.isSelectionMode && !showEmptyGuide
    val showBottomNav = isMainRoute && !uiState.isSelectionMode && !showEmptyGuide
    val showSelectionBar = route == WorkspaceRoutes.Feed && uiState.isSelectionMode

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CaptureUI.Slate50,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = WorkspaceRoutes.Feed,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBottomNav || showSelectionBar) 92.dp else 0.dp),
            ) {
                composable(WorkspaceRoutes.Feed) {
                    FeedRoute(
                        uiState = uiState,
                        onOpenAiSummary = {
                            workspaceViewModel.prepareAiSummary()
                            navController.navigate(WorkspaceRoutes.AiSummary)
                        },
                        onLongPress = workspaceViewModel::enterSelectionMode,
                        onToggleSelection = workspaceViewModel::toggleExcerptSelection,
                        onLoadSample = workspaceViewModel::loadSampleData,
                        onStartRecording = workspaceViewModel::openQuickCapture,
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                    )
                }
                composable(WorkspaceRoutes.Tree) {
                    TreeRoute(
                        snapshot = uiState.snapshot,
                        onNodeClick = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onCreateRootNode = workspaceViewModel::createRootNode,
                    )
                }
                composable(WorkspaceRoutes.Tags) {
                    TagsRoute(tagGroups = uiState.snapshot.tagGroups)
                }
                composable(WorkspaceRoutes.AiSummary) {
                    AiSummaryRoute(
                        uiState = uiState,
                        searchSuggestions = workspaceViewModel::searchSuggestions,
                        onClose = {
                            workspaceViewModel.clearAiSummary()
                            navController.popBackStack()
                        },
                        onSave = {
                            workspaceViewModel.saveAiSummary {
                                navController.popBackStack(WorkspaceRoutes.Feed, false)
                            }
                        },
                        onSelectTargetNode = workspaceViewModel::selectAiTargetNode,
                        onSelectParentNode = workspaceViewModel::selectAiParentNode,
                        onMarkdownChange = workspaceViewModel::updateAiMarkdown,
                        onNewNodeTitleChange = workspaceViewModel::updateAiNewNodeTitle,
                    )
                }
                composable(
                    route = WorkspaceRoutes.NodePattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    NodeDetailRoute(
                        snapshot = uiState.snapshot,
                        nodeId = nodeId,
                        searchSuggestions = workspaceViewModel::searchSuggestions,
                        onBack = { navController.popBackStack() },
                        onOpenGraph = { navController.navigate(WorkspaceRoutes.graph(nodeId)) },
                        onUpdateOutline = workspaceViewModel::updateNodeOutline,
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                        onAddExcerpt = {
                            workspaceViewModel.setQuickCaptureArchiveNode(nodeId)
                            workspaceViewModel.openQuickCapture()
                        },
                    )
                }
                composable(
                    route = WorkspaceRoutes.GraphPattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    GraphRoute(
                        snapshot = uiState.snapshot,
                        nodeId = nodeId,
                        onBack = { navController.popBackStack() },
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                    )
                }
            }

            if (showFab) {
                Button(
                    onClick = workspaceViewModel::openQuickCapture,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CaptureUI.Indigo600),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 86.dp)
                        .size(58.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("＋", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (showBottomNav) {
                BottomNavigationBar(
                    currentRoute = route,
                    onNavigate = { destination ->
                        if (destination != route) {
                            navController.navigate(destination) {
                                popUpTo(WorkspaceRoutes.Feed)
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (showSelectionBar) {
                SelectionActionBar(
                    selectedCount = uiState.selectedExcerptIds.size,
                    onCancel = workspaceViewModel::clearSelection,
                    onOpenAiSummary = {
                        workspaceViewModel.prepareAiSummary()
                        navController.navigate(WorkspaceRoutes.AiSummary)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (uiState.isQuickCaptureOpen) {
                QuickCaptureOverlay(
                    snapshot = uiState.snapshot,
                    draft = uiState.quickCaptureDraft,
                    searchSuggestions = workspaceViewModel::searchSuggestions,
                    onDismiss = workspaceViewModel::closeQuickCapture,
                    onContentChange = workspaceViewModel::updateQuickCaptureContent,
                    onThoughtChange = workspaceViewModel::updateQuickCaptureThought,
                    onUrlChange = workspaceViewModel::updateQuickCaptureUrl,
                    onTagToggle = workspaceViewModel::toggleQuickCaptureTag,
                    onArchiveNodeSelect = workspaceViewModel::setQuickCaptureArchiveNode,
                    onSave = { workspaceViewModel.saveQuickCapture() },
                )
            }

            previewExcerptId?.let { excerptId ->
                uiState.snapshot.excerptsById[excerptId]?.let { excerpt ->
                    ExcerptPreviewDialog(
                        excerpt = excerpt,
                        onDismiss = { previewExcerptId = null },
                        onOpenNode = { nodeId ->
                            previewExcerptId = null
                            if (nodeId != null) navController.navigate(WorkspaceRoutes.node(nodeId))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomNavItem("🏠", "摘录", currentRoute == WorkspaceRoutes.Feed) { onNavigate(WorkspaceRoutes.Feed) }
        BottomNavItem("🌲", "知识树", currentRoute == WorkspaceRoutes.Tree) { onNavigate(WorkspaceRoutes.Tree) }
        BottomNavItem("🏷️", "标签", currentRoute == WorkspaceRoutes.Tags) { onNavigate(WorkspaceRoutes.Tags) }
    }
}

@Composable
private fun BottomNavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    val color = if (active) CaptureUI.Indigo600 else CaptureUI.Slate400
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) CaptureUI.Indigo50 else Color.Transparent)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 20.sp)
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onOpenAiSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("已选 $selectedCount 项", color = CaptureUI.Slate600, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onOpenAiSummary, enabled = selectedCount > 0) {
                Text("✨ AI提炼并归档")
            }
        }
    }
}

@Composable
private fun FeedRoute(
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
    val filtered = uiState.snapshot.excerpts.filter { excerpt ->
        val matchesQuery = searchQuery.isBlank() ||
            excerpt.content.contains(searchQuery, ignoreCase = true) ||
            excerpt.thought.contains(searchQuery, ignoreCase = true) ||
            excerpt.tags.any { it.contains(searchQuery, ignoreCase = true) }
        val matchesFilter = !showInboxOnly || excerpt.archivedNodeId == null
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("🔍 搜索摘录、标签或想法...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            Button(
                onClick = { showInboxOnly = !showInboxOnly },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showInboxOnly) CaptureUI.Indigo50 else Color.White,
                    contentColor = if (showInboxOnly) CaptureUI.Indigo600 else CaptureUI.Slate600,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("漏斗")
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

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            filtered.forEach { excerpt ->
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
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F1FF)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✨ AI推荐", color = CaptureUI.Purple500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun EmptyStateRoute(
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
private fun ExcerptCard(
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (excerpt.archivedNodeTitle == null) {
                        StatusBadge(text = "未归档", bg = Color(0xFFFFF3D8), fg = Color(0xFFD97706))
                    } else {
                        StatusBadge(text = excerpt.archivedNodeTitle, bg = CaptureUI.Slate100, fg = CaptureUI.Slate500)
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
                    if (targetId.startsWith("excerpt-")) onPreviewExcerpt(targetId) else onOpenNode(targetId)
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
                                if (targetId.startsWith("excerpt-")) onPreviewExcerpt(targetId) else onOpenNode(targetId)
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
private fun StatusBadge(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TreeRoute(
    snapshot: WorkspaceSnapshot,
    onNodeClick: (String) -> Unit,
    onCreateRootNode: (String, (String) -> Unit) -> Unit,
) {
    var expandedIds by remember(snapshot.treeRoots) {
        mutableStateOf(snapshot.treeRoots.map { it.id }.toSet())
    }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newRootTitle by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🌲 知识体系", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Button(onClick = { showAddDialog = true }) { Text("＋ 根节点") }
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(8.dp)) {
                snapshot.treeRoots.forEach { node ->
                    TreeNodeRow(
                        node = node,
                        expandedIds = expandedIds,
                        onToggle = { id ->
                            expandedIds = if (expandedIds.contains(id)) expandedIds - id else expandedIds + id
                        },
                        onOpen = onNodeClick,
                    )
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新建根节点") },
            text = {
                OutlinedTextField(
                    value = newRootTitle,
                    onValueChange = { newRootTitle = it },
                    placeholder = { Text("输入根节点名称") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCreateRootNode(newRootTitle) {}
                    newRootTitle = ""
                    showAddDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TreeNodeRow(
    node: TreeNodeUiModel,
    expandedIds: Set<String>,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    level: Int = 0,
) {
    val isExpanded = expandedIds.contains(node.id)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 18).dp, top = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (node.children.isNotEmpty()) if (isExpanded) "▼" else "▶" else "•",
                modifier = Modifier.combinedClickable { if (node.children.isNotEmpty()) onToggle(node.id) },
                color = CaptureUI.Slate400,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                node.title,
                modifier = Modifier.weight(1f).combinedClickable { onOpen(node.id) },
                color = CaptureUI.Slate800,
                fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Medium,
            )
            Text("${node.count} 条", color = CaptureUI.Slate400, fontSize = 12.sp)
        }
        if (isExpanded) {
            node.children.forEach { child ->
                TreeNodeRow(
                    node = child,
                    expandedIds = expandedIds,
                    onToggle = onToggle,
                    onOpen = onOpen,
                    level = level + 1,
                )
            }
        }
    }
}

@Composable
private fun TagsRoute(tagGroups: List<TagGroupUiModel>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🏷️ 标签库", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Button(onClick = {}) { Text("＋ 新建") }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tagGroups.forEach { group ->
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📂 ${group.folder} (${group.count})", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.items.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = CaptureUI.Indigo50,
                                ) {
                                    Text(
                                        "#${tag.displayName} ${tag.heatWeight}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = CaptureUI.Indigo600,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun AiSummaryRoute(
    uiState: WorkspaceUiState,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onSelectTargetNode: (String?) -> Unit,
    onSelectParentNode: (String?) -> Unit,
    onMarkdownChange: (String) -> Unit,
    onNewNodeTitleChange: (String) -> Unit,
) {
    var showNodePicker by rememberSaveable { mutableStateOf(false) }
    var createNewNode by rememberSaveable { mutableStateOf(false) }
    val draft = uiState.aiSummaryDraft
    val selectedExcerpts = draft.selectedExcerptIds.mapNotNull(uiState.snapshot.excerptsById::get)
    val selectedNodeTitle = draft.targetNodeId?.let { uiState.snapshot.flatNodes[it]?.title }

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
            TextButton(onClick = onClose) { Text("✕ 取消") }
            Text("AI 整理助手", fontWeight = FontWeight.Bold)
            Button(
                onClick = onSave,
                enabled = draft.markdown.isNotBlank() && (draft.targetNodeId != null || draft.newNodeTitle.isNotBlank()),
            ) { Text("保存") }
        }
        Spacer(Modifier.height(16.dp))
        Text("✨ 总结大纲", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (draft.isGenerating) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            InlineLinkEditor(
                rawText = draft.markdown,
                placeholder = "输入 AI 大纲内容",
                onRawTextChange = onMarkdownChange,
                searchSuggestions = searchSuggestions,
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("🎯 挂载到知识树", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { showNodePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8FAFC), contentColor = CaptureUI.Slate700),
        ) {
            Text(
                selectedNodeTitle ?: if (draft.newNodeTitle.isNotBlank()) "新建节点：${draft.newNodeTitle}" else "＋ 选择或创建目标节点 (必填)",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("📎 关联的知识摘录 (${selectedExcerpts.size}项)", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CaptureUI.Slate50)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedExcerpts.forEach { excerpt ->
                    Text("- ${excerpt.content}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = CaptureUI.Slate600, fontSize = 13.sp)
                }
            }
        }
    }

    if (showNodePicker) {
        NodePickerOverlay(
            snapshot = uiState.snapshot,
            createNewNode = createNewNode,
            draftTitle = draft.newNodeTitle,
            selectedTargetNodeId = draft.targetNodeId,
            selectedParentNodeId = draft.parentNodeId,
            onDismiss = { showNodePicker = false },
            onToggleCreate = { createNewNode = !createNewNode },
            onSelectTarget = {
                createNewNode = false
                onSelectTargetNode(it)
                showNodePicker = false
            },
            onSelectParent = {
                createNewNode = true
                onSelectTargetNode(null)
                onSelectParentNode(it)
            },
            onTitleChange = onNewNodeTitleChange,
        )
    }
}

@Composable
private fun NodePickerOverlay(
    snapshot: WorkspaceSnapshot,
    createNewNode: Boolean,
    draftTitle: String,
    selectedTargetNodeId: String?,
    selectedParentNodeId: String?,
    onDismiss: () -> Unit,
    onToggleCreate: () -> Unit,
    onSelectTarget: (String) -> Unit,
    onSelectParent: (String) -> Unit,
    onTitleChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .combinedClickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CaptureBottomSheet(onDismiss = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).imePadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("选择目标节点", fontWeight = FontWeight.Bold)
                    TextButton(onClick = onToggleCreate) { Text(if (createNewNode) "选择现有节点" else "新建子节点") }
                }
                if (createNewNode) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = onTitleChange,
                        placeholder = { Text("输入新子节点标题") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selectedParentNodeId == null) "从下方选择父节点" else "父节点：${snapshot.flatNodes[selectedParentNodeId]?.title}",
                        color = CaptureUI.Slate500,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.treeRoots.forEach { node ->
                        NodePickerTreeRow(
                            node = node,
                            createNewNode = createNewNode,
                            selectedTargetNodeId = selectedTargetNodeId,
                            selectedParentNodeId = selectedParentNodeId,
                            onSelectTarget = onSelectTarget,
                            onSelectParent = onSelectParent,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun NodePickerTreeRow(
    node: TreeNodeUiModel,
    createNewNode: Boolean,
    selectedTargetNodeId: String?,
    selectedParentNodeId: String?,
    onSelectTarget: (String) -> Unit,
    onSelectParent: (String) -> Unit,
    level: Int = 0,
) {
    Column {
        val selected = if (createNewNode) node.id == selectedParentNodeId else node.id == selectedTargetNodeId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) CaptureUI.Indigo50 else Color.Transparent)
                .combinedClickable {
                    if (createNewNode) onSelectParent(node.id) else onSelectTarget(node.id)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (selected) "✅" else "🌿")
            Spacer(Modifier.width(8.dp))
            Text(node.title, modifier = Modifier.weight(1f), color = CaptureUI.Slate700)
        }
        node.children.forEach {
            NodePickerTreeRow(
                node = it,
                createNewNode = createNewNode,
                selectedTargetNodeId = selectedTargetNodeId,
                selectedParentNodeId = selectedParentNodeId,
                onSelectTarget = onSelectTarget,
                onSelectParent = onSelectParent,
                level = level + 1,
            )
        }
    }
}

@Composable
private fun NodeDetailRoute(
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
    var localOutline by remember(nodeId, node.outlineMarkdown) { mutableStateOf(node.outlineMarkdown) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text(node.title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 1)
            TextButton(onClick = onOpenGraph) { Text("🕸️局部图谱") }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                if (editing) onUpdateOutline(nodeId, localOutline)
                editing = !editing
            }) {
                Text(if (editing) "保存大纲" else "📝 编辑大纲")
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
        HorizontalDivider(color = CaptureUI.Slate100)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📎 节点摘录 (${nodeExcerpts.size})", fontWeight = FontWeight.Bold)
            TextButton(onClick = onAddExcerpt) { Text("添加摘录") }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            nodeExcerpts.forEach { excerpt ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CaptureUI.Slate50)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        LinkAwareText(
                            rawText = excerpt.content,
                            onLinkClick = { targetId ->
                                if (targetId == excerpt.id) return@LinkAwareText
                                if (snapshot.excerptsById.containsKey(targetId)) onPreviewExcerpt(targetId)
                                else if (snapshot.flatNodes.containsKey(targetId)) onOpenNode(targetId)
                            },
                        )
                        if (excerpt.tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                excerpt.tags.forEach { tag ->
                                    Surface(shape = RoundedCornerShape(12.dp), color = CaptureUI.Slate200) {
                                        Text("#$tag", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔄 被提及 (Backlinks)", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (backlinks.isEmpty()) {
                    Text("暂无反向链接", color = CaptureUI.Slate500)
                } else {
                    backlinks.forEach { backlink ->
                        TextButton(onClick = {
                            if (backlink.sourceType == BacklinkType.NODE) onOpenNode(backlink.sourceId) else onPreviewExcerpt(backlink.sourceId)
                        }) {
                            Text(
                                "• ${if (backlink.sourceType == BacklinkType.NODE) "在节点" else "在摘录"} ${backlink.title} 中被引用",
                                color = Color(0xFF1E3A8A),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LinkAwareText(rawText: String, onLinkClick: (String) -> Unit) {
    val annotated = remember(rawText) { buildInlineAnnotatedString(rawText) }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(color = CaptureUI.Slate800, lineHeight = 22.sp),
        onClick = { offset ->
            annotated.getStringAnnotations("inline-link", offset, offset).firstOrNull()?.let {
                onLinkClick(it.item)
            }
        },
    )
}

@Composable
private fun GraphRoute(
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
private fun GraphCanvas(
    graph: GraphUiModel,
    modifier: Modifier = Modifier,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(graph) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.7f, 2.2f)
                    panX += pan.x
                    panY += pan.y
                }
            }
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

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = panX, translationY = panY)) {
            graph.edges.forEach { edge ->
                val from = nodePositions[edge.fromId] ?: return@forEach
                val to = nodePositions[edge.toId] ?: return@forEach
                drawLine(color = CaptureUI.Slate300(), start = from, end = to, strokeWidth = 4f)
            }
            graph.nodes.forEach { node ->
                val position = nodePositions[node.id] ?: return@forEach
                drawCircle(color = graphColor(node.kind), radius = if (node.kind == GraphNodeKind.CURRENT_NODE) 44f else 34f, center = position)
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
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
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

private fun CaptureUI.Slate300(): Color = Color(0xFFCBD5E1)

private fun graphColor(kind: GraphNodeKind): Color {
    return when (kind) {
        GraphNodeKind.CURRENT_NODE -> CaptureUI.Indigo600
        GraphNodeKind.LINKED_NODE -> CaptureUI.Purple500
        GraphNodeKind.BACKLINK_NODE -> Color(0xFF2563EB)
        GraphNodeKind.EXCERPT -> Color(0xFF0EA5E9)
    }
}

@Composable
private fun QuickCaptureOverlay(
    snapshot: WorkspaceSnapshot,
    draft: QuickCaptureDraft,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onThoughtChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onArchiveNodeSelect: (String?) -> Unit,
    onSave: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showArchivePicker by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .combinedClickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CaptureBottomSheet(onDismiss = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("极速记录", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                InlineLinkEditor(
                    rawText = draft.content,
                    placeholder = "在此输入摘录、想法或粘贴链接...",
                    onRawTextChange = onContentChange,
                    searchSuggestions = searchSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    autoFocus = true,
                )
                Spacer(Modifier.height(8.dp))
                Text("💡 提示：输入 [[ 触发双向链接", color = CaptureUI.Slate400, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                InlineLinkEditor(
                    rawText = draft.thought,
                    placeholder = "补充你的想法（选填）",
                    onRawTextChange = onThoughtChange,
                    searchSuggestions = searchSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft.url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("🔗 添加来源 URL（选填）") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                Text("推荐标签", color = CaptureUI.Slate600, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.suggestedTags.forEach { tag ->
                        val selected = draft.selectedTags.contains(tag.fullName)
                        TagPill(label = tag.label, isSelected = selected) { onTagToggle(tag.fullName) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { showArchivePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CaptureUI.Slate50, contentColor = CaptureUI.Slate700),
                ) {
                    Text(
                        draft.archiveNodeId?.let { "📍 ${snapshot.flatNodes[it]?.title ?: "未归档 (Inbox)"}" } ?: "📍 未归档 (Inbox)",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSave()
                    },
                    enabled = draft.content.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("发送")
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        if (showArchivePicker) {
            ArchivePickerDialog(
                snapshot = snapshot,
                selectedNodeId = draft.archiveNodeId,
                onDismiss = { showArchivePicker = false },
                onSelect = {
                    onArchiveNodeSelect(it)
                    showArchivePicker = false
                }
            )
        }
    }
}

@Composable
private fun ArchivePickerDialog(
    snapshot: WorkspaceSnapshot,
    selectedNodeId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择归档位置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = { onSelect(null) }) {
                    Text(if (selectedNodeId == null) "✅ 未归档 (Inbox)" else "未归档 (Inbox)")
                }
                snapshot.treeRoots.forEach { node ->
                    ArchiveNodeRow(node, selectedNodeId, onSelect)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun ArchiveNodeRow(node: TreeNodeUiModel, selectedNodeId: String?, onSelect: (String?) -> Unit, level: Int = 0) {
    Column {
        TextButton(onClick = { onSelect(node.id) }, modifier = Modifier.padding(start = (level * 16).dp)) {
            Text(if (selectedNodeId == node.id) "✅ ${node.title}" else node.title)
        }
        node.children.forEach { ArchiveNodeRow(it, selectedNodeId, onSelect, level + 1) }
    }
}

@Composable
private fun ExcerptPreviewDialog(
    excerpt: ExcerptUiModel,
    onDismiss: () -> Unit,
    onOpenNode: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(excerpt.sourceTitle ?: "摘录预览") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkAwareText(rawText = excerpt.content, onLinkClick = {})
                if (excerpt.thought.isNotBlank()) {
                    LinkAwareText(rawText = excerpt.thought, onLinkClick = {})
                }
                if (excerpt.archivedNodeTitle != null) {
                    Text("已归档到 ${excerpt.archivedNodeTitle}", color = CaptureUI.Indigo600)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpenNode(excerpt.archivedNodeId) }) { Text("打开节点") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun InlineLinkEditor(
    rawText: String,
    placeholder: String,
    onRawTextChange: (String) -> Unit,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    modifier: Modifier = Modifier,
    minLines: Int = 4,
    autoFocus: Boolean = false,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(rawText, selection = TextRange(rawText.length))) }
    var suggestions by remember { mutableStateOf<List<LinkSuggestion>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(rawText) {
        if (rawText != fieldValue.text) {
            fieldValue = fieldValue.copy(text = rawText, selection = TextRange(rawText.length))
        }
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { next ->
                fieldValue = next
                onRawTextChange(next.text)
                val query = currentInlineQuery(next.text, next.selection.start)
                searchJob?.cancel()
                if (query != null) {
                    searchJob = scope.launch {
                        suggestions = searchSuggestions(query)
                    }
                } else {
                    suggestions = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (!state.isFocused) suggestions = emptyList()
                },
            minLines = minLines,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.clickable(enabled = false) {},
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "联想结果",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        color = CaptureUI.Slate400,
                        fontSize = 12.sp,
                    )
                    suggestions.forEach { suggestion ->
                        TextButton(onClick = {
                            val (raw, cursor) = insertStructuredLink(fieldValue.text, fieldValue.selection.start, suggestion)
                            fieldValue = TextFieldValue(raw, selection = TextRange(cursor))
                            onRawTextChange(raw)
                            suggestions = emptyList()
                        }) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (suggestion.type.name == "NODE") "🌲 ${suggestion.title}" else "📎 ${suggestion.title}",
                                    color = CaptureUI.Slate800,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    suggestion.preview,
                                    color = CaptureUI.Slate400,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
