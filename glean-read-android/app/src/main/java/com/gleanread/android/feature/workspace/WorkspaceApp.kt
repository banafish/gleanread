@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.feature.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gleanread.android.appContainer
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeBranchRoute
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeHomeRoute
import com.gleanread.android.feature.workspace.component.ExcerptPreviewDialog
import com.gleanread.android.feature.workspace.component.QuickCaptureOverlay
import com.gleanread.android.feature.workspace.feed.FeedRoute
import com.gleanread.android.feature.workspace.feed.FeedViewModel
import com.gleanread.android.feature.workspace.graph.GraphRoute
import com.gleanread.android.feature.workspace.node_detail.NodeDetailRoute
import com.gleanread.android.feature.workspace.summary.AiSummaryRoute
import com.gleanread.android.feature.workspace.summary.AiSummaryViewModel
import com.gleanread.android.feature.workspace.tags.TagsRoute

object WorkspaceRoutes {
    const val Feed = "feed"
    const val Tree = "tree"
    const val Tags = "tags"
    const val AiSummary = "ai-summary"
    const val TreeBranchPattern = "tree/branch/{nodeId}"
    const val NodePattern = "node/{nodeId}"
    const val GraphPattern = "graph/{nodeId}"

    fun treeBranch(nodeId: String) = "tree/branch/$nodeId"
    fun node(nodeId: String) = "node/$nodeId"
    fun graph(nodeId: String) = "graph/$nodeId"
}

@Composable
fun WorkspaceApp() {
    val appContainer = LocalContext.current.appContainer
    val workspaceViewModel: WorkspaceViewModel = viewModel(factory = appContainer.workspaceViewModelFactory)
    val feedViewModel: FeedViewModel = viewModel(factory = appContainer.feedViewModelFactory)
    val quickCaptureViewModel: com.gleanread.android.feature.workspace.capture.QuickCaptureViewModel =
        viewModel(factory = appContainer.quickCaptureViewModelFactory)
    val aiSummaryViewModel: AiSummaryViewModel = viewModel(factory = appContainer.aiSummaryViewModelFactory)

    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: WorkspaceRoutes.Feed
    val snapshot by workspaceViewModel.snapshot.collectAsStateWithLifecycle()
    val feedUiState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val quickCaptureUiState by quickCaptureViewModel.uiState.collectAsStateWithLifecycle()
    val aiSummaryDraft by aiSummaryViewModel.draft.collectAsStateWithLifecycle()
    var previewExcerptId by rememberSaveable { mutableStateOf<String?>(null) }

    val isMainRoute =
        route == WorkspaceRoutes.Feed || route == WorkspaceRoutes.Tree || route == WorkspaceRoutes.Tags
    val showEmptyGuide = route == WorkspaceRoutes.Feed && snapshot.isEmpty
    val showFab =
        (route == WorkspaceRoutes.Feed || route == WorkspaceRoutes.Tags) &&
            !feedUiState.isSelectionMode &&
            !showEmptyGuide
    val showBottomNav = isMainRoute && !feedUiState.isSelectionMode && !showEmptyGuide
    val showSelectionBar = route == WorkspaceRoutes.Feed && feedUiState.isSelectionMode

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = quickCaptureViewModel::open,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                    )
                }
            }
        },
        bottomBar = {
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
                )
            } else if (showSelectionBar) {
                SelectionActionBar(
                    selectedCount = feedUiState.selectedExcerptIds.size,
                    onCancel = feedViewModel::clearSelection,
                    onOpenAiSummary = {
                        aiSummaryViewModel.prepare(feedUiState.selectedExcerptIds.toList())
                        navController.navigate(WorkspaceRoutes.AiSummary)
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = WorkspaceRoutes.Feed,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(300),
                    )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(300),
                    )
                },
            ) {
                composable(WorkspaceRoutes.Feed) {
                    FeedRoute(
                        snapshot = snapshot,
                        uiState = feedUiState,
                        onOpenAiSummary = {
                            aiSummaryViewModel.prepare(feedUiState.selectedExcerptIds.toList())
                            navController.navigate(WorkspaceRoutes.AiSummary)
                        },
                        onLongPress = feedViewModel::enterSelectionMode,
                        onToggleSelection = feedViewModel::toggleExcerptSelection,
                        onLoadSample = workspaceViewModel::loadSampleData,
                        onStartRecording = quickCaptureViewModel::open,
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                    )
                }
                composable(WorkspaceRoutes.Tree) {
                    KnowledgeTreeHomeRoute(
                        snapshot = snapshot,
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onOpenBranch = { navController.navigate(WorkspaceRoutes.treeBranch(it)) },
                        onCreateRootNode = workspaceViewModel::createRootNode,
                        onCreateChildNode = workspaceViewModel::createChildNode,
                        onRenameNode = workspaceViewModel::renameNode,
                        onDeleteNode = workspaceViewModel::deleteNodeSubtree,
                    )
                }
                composable(
                    route = WorkspaceRoutes.TreeBranchPattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    KnowledgeTreeBranchRoute(
                        snapshot = snapshot,
                        nodeId = nodeId,
                        onBack = { navController.popBackStack() },
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onOpenBranch = { navController.navigate(WorkspaceRoutes.treeBranch(it)) },
                        onCreateChildNode = workspaceViewModel::createChildNode,
                        onRenameNode = workspaceViewModel::renameNode,
                        onDeleteNode = workspaceViewModel::deleteNodeSubtree,
                    )
                }
                composable(WorkspaceRoutes.Tags) {
                    TagsRoute(tagGroups = snapshot.tagGroups)
                }
                composable(WorkspaceRoutes.AiSummary) {
                    AiSummaryRoute(
                        snapshot = snapshot,
                        draft = aiSummaryDraft,
                        searchSuggestions = workspaceViewModel::searchSuggestions,
                        onClose = {
                            aiSummaryViewModel.clear()
                            navController.popBackStack()
                        },
                        onSave = {
                            aiSummaryViewModel.save {
                                feedViewModel.clearSelection()
                                navController.popBackStack(WorkspaceRoutes.Feed, false)
                            }
                        },
                        onSelectTargetNode = aiSummaryViewModel::selectTargetNode,
                        onSelectParentNode = aiSummaryViewModel::selectParentNode,
                        onMarkdownChange = aiSummaryViewModel::updateMarkdown,
                        onNewNodeTitleChange = aiSummaryViewModel::updateNewNodeTitle,
                    )
                }
                composable(
                    route = WorkspaceRoutes.NodePattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    NodeDetailRoute(
                        snapshot = snapshot,
                        nodeId = nodeId,
                        searchSuggestions = workspaceViewModel::searchSuggestions,
                        onBack = { navController.popBackStack() },
                        onOpenGraph = { navController.navigate(WorkspaceRoutes.graph(nodeId)) },
                        onUpdateOutline = workspaceViewModel::updateNodeOutline,
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                        onAddExcerpt = { quickCaptureViewModel.openForNode(nodeId) },
                    )
                }
                composable(
                    route = WorkspaceRoutes.GraphPattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    GraphRoute(
                        snapshot = snapshot,
                        nodeId = nodeId,
                        onBack = { navController.popBackStack() },
                        onOpenNode = { navController.navigate(WorkspaceRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                    )
                }
            }

            if (quickCaptureUiState.isOpen) {
                QuickCaptureOverlay(
                    snapshot = snapshot,
                    draft = quickCaptureUiState.draft,
                    searchSuggestions = workspaceViewModel::searchSuggestions,
                    onDismiss = quickCaptureViewModel::close,
                    onContentChange = quickCaptureViewModel::updateContent,
                    onThoughtChange = quickCaptureViewModel::updateThought,
                    onUrlChange = quickCaptureViewModel::updateUrl,
                    onTagToggle = quickCaptureViewModel::toggleTag,
                    onArchiveNodeSelect = quickCaptureViewModel::setArchiveNode,
                    onSave = { quickCaptureViewModel.save() },
                )
            }

            previewExcerptId?.let { excerptId ->
                snapshot.excerptsById[excerptId]?.let { excerpt ->
                    ExcerptPreviewDialog(
                        excerpt = excerpt,
                        onDismiss = { previewExcerptId = null },
                        onOpenNode = { nodeId ->
                            previewExcerptId = null
                            if (nodeId != null) {
                                navController.navigate(WorkspaceRoutes.node(nodeId))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentRoute == WorkspaceRoutes.Feed,
            onClick = { onNavigate(WorkspaceRoutes.Feed) },
            icon = { Icon(Icons.Default.Home, contentDescription = "摘录") },
            label = { Text("摘录") },
        )
        NavigationBarItem(
            selected = currentRoute == WorkspaceRoutes.Tree,
            onClick = { onNavigate(WorkspaceRoutes.Tree) },
            icon = { Icon(Icons.Default.AccountTree, contentDescription = "知识树") },
            label = { Text("知识树") },
        )
        NavigationBarItem(
            selected = currentRoute == WorkspaceRoutes.Tags,
            onClick = { onNavigate(WorkspaceRoutes.Tags) },
            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "标签") },
            label = { Text("标签") },
        )
    }
}

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onOpenAiSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "已选 $selectedCount 项",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onOpenAiSummary, enabled = selectedCount > 0) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI 提炼并归档",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text("AI 提炼并归档")
            }
        }
    }
}
