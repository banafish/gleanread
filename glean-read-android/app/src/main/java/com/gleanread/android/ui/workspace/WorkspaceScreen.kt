@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import android.content.Context
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeBranchRoute
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeHomeRoute

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

    val isMainRoute =
        route == WorkspaceRoutes.Feed || route == WorkspaceRoutes.Tree || route == WorkspaceRoutes.Tags
    val showEmptyGuide = route == WorkspaceRoutes.Feed && uiState.snapshot.isEmpty
    val showFab =
        (route == WorkspaceRoutes.Feed || route == WorkspaceRoutes.Tags) && !uiState.isSelectionMode && !showEmptyGuide
    val showBottomNav = isMainRoute && !uiState.isSelectionMode && !showEmptyGuide
    val showSelectionBar = route == WorkspaceRoutes.Feed && uiState.isSelectionMode

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = workspaceViewModel::openQuickCapture,
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
                    selectedCount = uiState.selectedExcerptIds.size,
                    onCancel = workspaceViewModel::clearSelection,
                    onOpenAiSummary = {
                        workspaceViewModel.prepareAiSummary()
                        navController.navigate(WorkspaceRoutes.AiSummary)
                    },
                )
            }
        }) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = WorkspaceRoutes.Feed,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        androidx.compose.animation.core.tween(300)
                    )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        androidx.compose.animation.core.tween(300)
                    )
                },
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
                    KnowledgeTreeHomeRoute(
                        snapshot = uiState.snapshot,
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
                        snapshot = uiState.snapshot,
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

            // FAB and BottomNav are now handled by Scaffold

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
            label = { Text("摘录") })
        NavigationBarItem(
            selected = currentRoute == WorkspaceRoutes.Tree,
            onClick = { onNavigate(WorkspaceRoutes.Tree) },
            icon = { Icon(Icons.Default.AccountTree, contentDescription = "知识树") },
            label = { Text("知识树") })
        NavigationBarItem(
            selected = currentRoute == WorkspaceRoutes.Tags,
            onClick = { onNavigate(WorkspaceRoutes.Tags) },
            icon = { Icon(Icons.Default.Label, contentDescription = "标签") },
            label = { Text("标签") })
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
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onOpenAiSummary, enabled = selectedCount > 0) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI提炼并归档",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("AI提炼并归档")
            }
        }
    }
}
