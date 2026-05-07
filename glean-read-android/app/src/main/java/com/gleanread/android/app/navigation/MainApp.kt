@file:OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)

package com.gleanread.android.app.navigation

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
import com.gleanread.android.core.ui.motion.fabLaunchDialogMotion
import com.gleanread.android.feature.excerpts.detail.ExcerptDetailRoute
import com.gleanread.android.feature.excerpts.detail.NewExcerptRoute
import com.gleanread.android.feature.excerpts.feed.FeedRoute
import com.gleanread.android.feature.excerpts.feed.FeedViewModel
import com.gleanread.android.feature.excerpts.summary.AiSummaryRoute
import com.gleanread.android.feature.excerpts.summary.AiSummaryViewModel
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeBranchRoute
import com.gleanread.android.feature.knowledge_tree.KnowledgeTreeHomeRoute
import com.gleanread.android.feature.knowledge_tree.graph.GraphRoute
import com.gleanread.android.feature.knowledge_tree.node_detail.NodeDetailRoute
import com.gleanread.android.feature.settings.SettingsRoute
import com.gleanread.android.feature.tags.TagsRoute
import com.gleanread.android.feature.tags.TagsViewModel
import com.gleanread.android.feature.tags.component.AddTagDialog
import com.gleanread.android.feature.settings.auth.AuthRoute

object MainRoutes {
    const val Feed = "feed"
    const val Tree = "tree"
    const val Tags = "tags"
    const val Settings = "settings"
    const val Auth = "auth"
    const val AiSummary = "ai-summary"
    const val NewExcerptPattern = "new-excerpt?archiveNodeId={archiveNodeId}"
    const val ExcerptPattern = "excerpt/{excerptId}"
    const val TreeBranchPattern = "tree/branch/{nodeId}"
    const val NodePattern = "node/{nodeId}"
    const val GraphPattern = "graph/{nodeId}"

    fun newExcerpt(archiveNodeId: String? = null): String {
        return archiveNodeId
            ?.let { "new-excerpt?archiveNodeId=${Uri.encode(it)}" }
            ?: "new-excerpt"
    }

    fun excerpt(excerptId: String) = "excerpt/$excerptId"
    fun treeBranch(nodeId: String) = "tree/branch/$nodeId"
    fun node(nodeId: String) = "node/$nodeId"
    fun graph(nodeId: String) = "graph/$nodeId"
}

@Composable
fun MainApp() {
    val appContainer = LocalContext.current.appContainer
    val mainViewModel: MainAppViewModel = viewModel(factory = appContainer.mainAppViewModelFactory)
    val feedViewModel: FeedViewModel = viewModel(factory = appContainer.feedViewModelFactory)
    val aiSummaryViewModel: AiSummaryViewModel = viewModel(factory = appContainer.aiSummaryViewModelFactory)
    val tagsViewModel: TagsViewModel = viewModel(factory = appContainer.tagsViewModelFactory)

    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route ?: MainRoutes.Feed
    val snapshot by mainViewModel.snapshot.collectAsStateWithLifecycle()
    val syncState by mainViewModel.syncState.collectAsStateWithLifecycle()
    val localDataOwnershipUiState by mainViewModel.localDataOwnershipUiState.collectAsStateWithLifecycle()
    val feedUiState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val aiSummaryDraft by aiSummaryViewModel.draft.collectAsStateWithLifecycle()
    val tagsUiState by tagsViewModel.uiState.collectAsStateWithLifecycle()
    var isAddTagDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addTagInput by rememberSaveable { mutableStateOf("") }

    val isMainRoute = route == MainRoutes.Feed ||
        route == MainRoutes.Tree ||
        route == MainRoutes.Tags ||
        route == MainRoutes.Settings
    val showTagsFab = route == MainRoutes.Tags &&
        !feedUiState.isSelectionMode &&
        !tagsUiState.isSelectionMode &&
        !tagsUiState.isSearchVisible
    val showBottomNav = isMainRoute &&
        !feedUiState.isSelectionMode &&
        !(route == MainRoutes.Tags && tagsUiState.isSelectionMode)
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
            if (showTagsFab) {
                FloatingActionButton(
                    onClick = openAddTagDialog,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.tags_add_content_description),
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
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
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
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
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
                            isRefreshing = syncState.isSyncing,
                            onRefresh = mainViewModel::syncNow,
                            onStartRecording = { navController.navigate(MainRoutes.newExcerpt()) },
                            onAddExcerpt = { navController.navigate(MainRoutes.newExcerpt()) },
                            onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                            onOpenExcerpt = { navController.navigate(MainRoutes.excerpt(it)) },
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
                            onMoveNodeToPosition = mainViewModel::moveNodeToPosition,
                            onRenameNode = mainViewModel::renameNode,
                            onDeleteNode = mainViewModel::deleteNodeSubtree,
                            isRefreshing = syncState.isSyncing,
                            onRefresh = mainViewModel::syncNow,
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
                            onMoveNodeToPosition = mainViewModel::moveNodeToPosition,
                            onRenameNode = mainViewModel::renameNode,
                            onDeleteNode = mainViewModel::deleteNodeSubtree,
                            isRefreshing = syncState.isSyncing,
                            onRefresh = mainViewModel::syncNow,
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
                            isRefreshing = syncState.isSyncing,
                            onRefresh = mainViewModel::syncNow,
                            onAddTag = openAddTagDialog,
                            onConfirmDeleteDialog = {
                                mainViewModel.deleteTags(tagsUiState.pendingDeleteTagIds) {
                                    tagsViewModel.dismissDeleteDialog()
                                    tagsViewModel.clearSelection()
                                }
                            },
                        )
                    }
                    composable(MainRoutes.Settings) {
                        SettingsRoute(
                            onNavigateToAuth = { navController.navigate(MainRoutes.Auth) }
                        )
                    }
                    composable(MainRoutes.Auth) {
                        AuthRoute(
                            onNavigateBack = { navController.popBackStack() }
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
                            onCreateRootNode = mainViewModel::createRootNode,
                            onCreateChildNode = mainViewModel::createChildNode,
                            onSelectTargetNode = aiSummaryViewModel::selectTargetNode,
                            onMarkdownChange = aiSummaryViewModel::updateMarkdown,
                        )
                    }
                    composable(
                        route = MainRoutes.NewExcerptPattern,
                        arguments = listOf(
                            navArgument("archiveNodeId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { backStackEntry ->
                        val archiveNodeId = backStackEntry.arguments?.getString("archiveNodeId")
                        NewExcerptRoute(
                            snapshot = snapshot,
                            initialArchiveNodeId = archiveNodeId,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            searchSuggestions = mainViewModel::searchSuggestions,
                            onBack = { navController.popBackStack() },
                            onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                            onOpenExcerpt = { navController.navigate(MainRoutes.excerpt(it)) },
                            onCreatedExcerpt = { excerptId ->
                                navController.popBackStack()
                                navController.navigate(MainRoutes.excerpt(excerptId))
                            },
                            onCreateExcerpt = mainViewModel::createExcerpt,
                        )
                    }
                    composable(
                        route = MainRoutes.ExcerptPattern,
                        arguments = listOf(navArgument("excerptId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val excerptId = backStackEntry.arguments?.getString("excerptId").orEmpty()
                        ExcerptDetailRoute(
                            snapshot = snapshot,
                            excerptId = excerptId,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            searchSuggestions = mainViewModel::searchSuggestions,
                            onBack = { navController.popBackStack() },
                            onOpenNode = { navController.navigate(MainRoutes.node(it)) },
                            onOpenExcerpt = { navController.navigate(MainRoutes.excerpt(it)) },
                            onSaveExcerpt = mainViewModel::updateExcerpt,
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
                            onOpenExcerpt = { navController.navigate(MainRoutes.excerpt(it)) },
                            onAddExcerpt = { navController.navigate(MainRoutes.newExcerpt(nodeId)) },
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
                            onOpenExcerpt = { navController.navigate(MainRoutes.excerpt(it)) },
                        )
                    }
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
                    modifier = Modifier.fabLaunchDialogMotion(),
                )
            }

        }
    }

    if (localDataOwnershipUiState.isDialogVisible) {
        PendingLocalDataOwnershipDialog(
            isSubmitting = localDataOwnershipUiState.isSubmitting,
            onMergeLocalData = {
                mainViewModel.choosePendingLocalDataOwnership(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) {
                    if (route == MainRoutes.Auth) {
                        navController.popBackStack()
                    }
                }
            },
            onKeepLocalData = {
                mainViewModel.choosePendingLocalDataOwnership(LocalDataOwnershipChoice.KEEP_LOCAL) {
                    if (route == MainRoutes.Auth) {
                        navController.popBackStack()
                    }
                }
            },
        )
    }
}

@Composable
private fun PendingLocalDataOwnershipDialog(
    isSubmitting: Boolean,
    onMergeLocalData: () -> Unit,
    onKeepLocalData: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.auth_local_data_title)) },
        text = { Text(stringResource(R.string.auth_local_data_body)) },
        confirmButton = {
            TextButton(
                onClick = onMergeLocalData,
                enabled = !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(stringResource(R.string.auth_local_data_merge))
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(
                    onClick = onKeepLocalData,
                ) {
                    Text(stringResource(R.string.auth_local_data_keep_local))
                }
            }
        },
    )
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
        NavigationBarItem(
            selected = currentRoute == MainRoutes.Settings,
            onClick = { onNavigate(MainRoutes.Settings) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.main_nav_settings),
                )
            },
            label = { Text(stringResource(R.string.main_nav_settings)) },
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
