@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object WorkspaceRoutes {
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

    val isMainRoute =
        route == WorkspaceRoutes.Feed || route == WorkspaceRoutes.Tree || route == WorkspaceRoutes.Tags
    val showEmptyGuide = route == WorkspaceRoutes.Feed && uiState.snapshot.isEmpty
    val showFab = isMainRoute && !uiState.isSelectionMode && !showEmptyGuide
    val showBottomNav = isMainRoute && !uiState.isSelectionMode && !showEmptyGuide
    val showSelectionBar = route == WorkspaceRoutes.Feed && uiState.isSelectionMode

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 86.dp)
                        .size(58.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomNavItem(Icons.Default.Home, "摘录", currentRoute == WorkspaceRoutes.Feed) {
            onNavigate(
                WorkspaceRoutes.Feed
            )
        }
        BottomNavItem(Icons.Default.AccountTree, "知识树", currentRoute == WorkspaceRoutes.Tree) {
            onNavigate(
                WorkspaceRoutes.Tree
            )
        }
        BottomNavItem(Icons.Default.Label, "标签", currentRoute == WorkspaceRoutes.Tags) {
            onNavigate(
                WorkspaceRoutes.Tags
            )
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
        Text("已选 $selectedCount 项", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onOpenAiSummary, enabled = selectedCount > 0) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI提炼并归档", modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("AI提炼并归档")
            }
        }
    }
}
