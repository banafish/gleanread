@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.gleanread.android.R
import com.gleanread.android.app.appContainer
import com.gleanread.android.feature.capture.quick_capture.QuickCaptureViewModel
import com.gleanread.android.feature.capture.quick_capture.component.QuickCaptureOverlay
import com.gleanread.android.feature.excerpts.component.ExcerptPreviewDialog
import com.gleanread.android.feature.excerpts.feed.FeedRoute
import com.gleanread.android.feature.excerpts.feed.FeedViewModel
import com.gleanread.android.feature.excerpts.summary.AiSummaryRoute
import com.gleanread.android.feature.excerpts.summary.AiSummaryViewModel
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeBranchRoute
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeHomeRoute
import com.gleanread.android.feature.knowledge_tree.graph.GraphRoute
import com.gleanread.android.feature.knowledge_tree.node_detail.NodeDetailRoute
import com.gleanread.android.feature.tags.TagsRoute
import com.gleanread.android.feature.tags.TagsViewModel
import com.gleanread.android.feature.tags.component.AddTagDialog

object MainRoutes {
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
fun MainApp() {
    val appContainer = LocalContext.current.appContainer
    val mainViewModel: MainAppViewModel = viewModel(factory = appContainer.mainAppViewModelFactory)
    val feedViewModel: FeedViewModel = viewModel(factory = appContainer.feedViewModelFactory)
    val quickCaptureViewModel: QuickCaptureViewModel =
        viewModel(factory = appContainer.quickCaptureViewModelFactory)
    val aiSummaryViewModel: AiSummaryViewModel = viewModel(factory = appContainer.aiSummaryViewModelFactory)
    val tagsViewModel: TagsViewModel = viewModel(factory = appContainer.tagsViewModelFactory)

    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: MainRoutes.Feed
    val snapshot by mainViewModel.snapshot.collectAsStateWithLifecycle()
    val feedUiState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val quickCaptureUiState by quickCaptureViewModel.uiState.collectAsStateWithLifecycle()
    val aiSummaryDraft by aiSummaryViewModel.draft.collectAsStateWithLifecycle()
    val tagsUiState by tagsViewModel.uiState.collectAsStateWithLifecycle()
    var previewExcerptId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAddTagDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addTagInput by rememberSaveable { mutableStateOf("") }

    val isMainRoute = route == MainRoutes.Feed || route == MainRoutes.Tree || route == MainRoutes.Tags
    val showEmptyGuide = route == MainRoutes.Feed && snapshot.isEmpty
    val showFab = (route == MainRoutes.Feed || route == MainRoutes.Tags) &&
        !feedUiState.isSelectionMode &&
        !(route == MainRoutes.Tags && (tagsUiState.isSelectionMode || tagsUiState.isSearchVisible)) &&
        !showEmptyGuide
    val showBottomNav = isMainRoute &&
        !feedUiState.isSelectionMode &&
        !(route == MainRoutes.Tags && tagsUiState.isSelectionMode) &&
        !showEmptyGuide
    val showFeedSelectionBar = route == MainRoutes.Feed && feedUiState.isSelectionMode
    val showTagsSelectionBar = route == MainRoutes.Tags && tagsUiState.isSelectionMode
    val openAddTagDialog = {
        isAddTagDialogOpen = true
    }
    val dismissAddTagDialog = {
        isAddTagDialogOpen = false
        addTagInput = ""
    }

    LaunchedEffect(route) {
        if (route != MainRoutes.Tags && isAddTagDialogOpen) {
            dismissAddTagDialog()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        if (route == MainRoutes.Tags) {
                            openAddTagDialog()
                        } else {
                            quickCaptureViewModel.open()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(
                            if (route == MainRoutes.Tags) {
                                R.string.tags_add_content_description
                            } else {
                                R.string.main_add_content_description
                            },
                        ),
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
                                popUpTo(MainRoutes.Feed)
                                launchSingleTop = true
                            }
                        }
                    },
                )
            } else if (showFeedSelectionBar) {
                SelectionActionBar(
                    selectedCount = feedUiState.selectedExcerptIds.size,
                    onCancel = feedViewModel::clearSelection,
                    onOpenAiSummary = {
                        aiSummaryViewModel.prepare(feedUiState.selectedExcerptIds.toList())
                        navController.navigate(MainRoutes.AiSummary)
                    },
                )
            } else if (showTagsSelectionBar) {
                TagsSelectionActionBar(
                    selectedCount = tagsUiState.selectedTagIds.size,
                    onCancel = tagsViewModel::clearSelection,
                    onDelete = tagsViewModel::promptDeleteSelected,
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
                startDestination = MainRoutes.Feed,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
            ) {
                composable(MainRoutes.Feed) {
                    FeedRoute(
                        snapshot = snapshot,
                        uiState = feedUiState,
                        onOpenAiSummary = {
                            aiSummaryViewModel.prepare(feedUiState.selectedExcerptIds.toList())
                            navController.navigate(MainRoutes.AiSummary)
                        },
                        onOpenExcerptAiSummary = { excerptId ->
                            feedViewModel.clearSelection()
                            aiSummaryViewModel.prepare(listOf(excerptId))
                            navController.navigate(MainRoutes.AiSummary)
                        },
                        onDeleteExcerpt = mainViewModel::deleteExcerpt,
                        onLongPress = feedViewModel::enterSelectionMode,
                        onToggleSelection = feedViewModel::toggleExcerptSelection,
                        onLoadSample = mainViewModel::loadSampleData,
                        onStartRecording = quickCaptureViewModel::open,
                        onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                    )
                }
                composable(MainRoutes.Tree) {
                    KnowledgeTreeHomeRoute(
                        snapshot = snapshot,
                        onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                        onOpenBranch = { navController.navigate(MainRoutes.treeBranch(it)) },
                        onCreateRootNode = mainViewModel::createRootNode,
                        onCreateChildNode = mainViewModel::createChildNode,
                        onMoveNode = mainViewModel::moveNode,
                        onRenameNode = mainViewModel::renameNode,
                        onDeleteNode = mainViewModel::deleteNodeSubtree,
                    )
                }
                composable(
                    route = MainRoutes.TreeBranchPattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    KnowledgeTreeBranchRoute(
                        snapshot = snapshot,
                        nodeId = nodeId,
                        onBack = { navController.popBackStack() },
                        onOpenRoot = {
                            if (!navController.popBackStack(MainRoutes.Tree, false)) {
                                navController.navigate(MainRoutes.Tree) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                        onOpenBranch = { navController.navigate(MainRoutes.treeBranch(it)) },
                        onCreateRootNode = mainViewModel::createRootNode,
                        onCreateChildNode = mainViewModel::createChildNode,
                        onMoveNode = mainViewModel::moveNode,
                        onRenameNode = mainViewModel::renameNode,
                        onDeleteNode = mainViewModel::deleteNodeSubtree,
                    )
                }
                composable(MainRoutes.Tags) {
                    TagsRoute(
                        snapshot = snapshot,
                        uiState = tagsUiState,
                        onToggleSearch = tagsViewModel::toggleSearch,
                        onSearchQueryChange = tagsViewModel::updateSearchQuery,
                        onLongPressTag = tagsViewModel::enterSelectionMode,
                        onToggleTagSelection = tagsViewModel::toggleTagSelection,
                        onDismissDeleteDialog = tagsViewModel::dismissDeleteDialog,
                        onConfirmDeleteDialog = {
                            mainViewModel.deleteTags(tagsUiState.pendingDeleteTagIds) {
                                tagsViewModel.dismissDeleteDialog()
                                tagsViewModel.clearSelection()
                            }
                        },
                    )
                }
                composable(MainRoutes.AiSummary) {
                    AiSummaryRoute(
                        snapshot = snapshot,
                        draft = aiSummaryDraft,
                        searchSuggestions = mainViewModel::searchSuggestions,
                        onClose = {
                            aiSummaryViewModel.clear()
                            navController.popBackStack()
                        },
                        onSave = {
                            aiSummaryViewModel.save {
                                feedViewModel.clearSelection()
                                navController.popBackStack(MainRoutes.Feed, false)
                            }
                        },
                        onSelectTargetNode = aiSummaryViewModel::selectTargetNode,
                        onSelectParentNode = aiSummaryViewModel::selectParentNode,
                        onMarkdownChange = aiSummaryViewModel::updateMarkdown,
                        onNewNodeTitleChange = aiSummaryViewModel::updateNewNodeTitle,
                    )
                }
                composable(
                    route = MainRoutes.NodePattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    NodeDetailRoute(
                        snapshot = snapshot,
                        nodeId = nodeId,
                        searchSuggestions = mainViewModel::searchSuggestions,
                        onBack = { navController.popBackStack() },
                        onOpenGraph = { navController.navigate(MainRoutes.graph(nodeId)) },
                        onUpdateOutline = mainViewModel::updateNodeOutline,
                        onMoveExcerptToInbox = mainViewModel::moveExcerptToInbox,
                        onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                        onAddExcerpt = { quickCaptureViewModel.openForNode(nodeId) },
                    )
                }
                composable(
                    route = MainRoutes.GraphPattern,
                    arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val nodeId = backStackEntry.arguments?.getString("nodeId").orEmpty()
                    GraphRoute(
                        snapshot = snapshot,
                        nodeId = nodeId,
                        onBack = { navController.popBackStack() },
                        onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                        onPreviewExcerpt = { previewExcerptId = it },
                    )
                }
            }

            if (route == MainRoutes.Tags && isAddTagDialogOpen) {
                AddTagDialog(
                    tagName = addTagInput,
                    onValueChange = { addTagInput = it },
                    onDismiss = dismissAddTagDialog,
                    onConfirm = {
                        mainViewModel.createTag(addTagInput) {
                            dismissAddTagDialog()
                        }
                    },
                )
            }

            if (quickCaptureUiState.isOpen) {
                QuickCaptureOverlay(
                    snapshot = snapshot,
                    draft = quickCaptureUiState.draft,
                    searchSuggestions = mainViewModel::searchSuggestions,
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
                                navController.navigate(MainRoutes.node(nodeId))
                            }
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
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentRoute == MainRoutes.Feed,
            onClick = { onNavigate(MainRoutes.Feed) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.main_nav_feed),
                )
            },
            label = { Text(stringResource(R.string.main_nav_feed)) },
        )
        NavigationBarItem(
            selected = currentRoute == MainRoutes.Tree,
            onClick = { onNavigate(MainRoutes.Tree) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = stringResource(R.string.main_nav_tree),
                )
            },
            label = { Text(stringResource(R.string.main_nav_tree)) },
        )
        NavigationBarItem(
            selected = currentRoute == MainRoutes.Tags,
            onClick = { onNavigate(MainRoutes.Tags) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = stringResource(R.string.main_nav_tags),
                )
            },
            label = { Text(stringResource(R.string.main_nav_tags)) },
        )
    }
}

@Composable
private fun TagsSelectionActionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
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
            text = stringResource(R.string.main_selected_count, selectedCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }
            Button(onClick = onDelete,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
            )) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.tags_delete_selected),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onError,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.tags_delete_selected))
            }
        }
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
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.main_selected_count, selectedCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }
            Button(onClick = onOpenAiSummary, enabled = selectedCount > 0) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.main_ai_archive_action),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.main_ai_archive_action))
            }
        }
    }
}
