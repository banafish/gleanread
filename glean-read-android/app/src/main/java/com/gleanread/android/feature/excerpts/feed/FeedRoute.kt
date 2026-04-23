package com.gleanread.android.feature.excerpts.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.gleanread.android.core.model.WorkspaceSnapshot

@Composable
fun FeedRoute(
    snapshot: WorkspaceSnapshot,
    uiState: FeedUiState,
    onOpenAiSummary: () -> Unit,
    onOpenExcerptAiSummary: (String) -> Unit,
    onDeleteExcerpt: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onLoadSample: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenExcerpt: (String) -> Unit,
) {
    if (snapshot.isEmpty) {
        FeedEmptyState(
            onLoadSample = onLoadSample,
            onStartRecording = onStartRecording,
        )
        return
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showInboxOnly by rememberSaveable { mutableStateOf(false) }
    var revealedExcerptId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteExcerptId by rememberSaveable { mutableStateOf<String?>(null) }
    val filtered = remember(snapshot.excerpts, searchQuery, showInboxOnly) {
        snapshot.excerpts.filter { excerpt ->
            val matchesQuery = searchQuery.isBlank() ||
                excerpt.content.contains(searchQuery, ignoreCase = true) ||
                excerpt.thought.contains(searchQuery, ignoreCase = true) ||
                excerpt.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesFilter = !showInboxOnly || excerpt.archivedNodeId == null
            matchesQuery && matchesFilter
        }
    }
    val pendingDeleteExcerpt = remember(snapshot.excerpts, pendingDeleteExcerptId) {
        snapshot.excerpts.firstOrNull { it.id == pendingDeleteExcerptId }
    }

    FeedScreen(
        filteredExcerpts = filtered,
        searchQuery = searchQuery,
        showInboxOnly = showInboxOnly,
        isSelectionMode = uiState.isSelectionMode,
        selectedExcerptIds = uiState.selectedExcerptIds,
        revealedExcerptId = revealedExcerptId,
        pendingDeleteExcerpt = pendingDeleteExcerpt,
        onSearchQueryChange = {
            revealedExcerptId = null
            searchQuery = it
        },
        onClearSearch = {
            revealedExcerptId = null
            searchQuery = ""
        },
        onToggleInboxFilter = {
            revealedExcerptId = null
            showInboxOnly = !showInboxOnly
        },
        onOpenAiSummary = onOpenAiSummary,
        onOpenExcerptAiSummary = { excerptId ->
            revealedExcerptId = null
            onOpenExcerptAiSummary(excerptId)
        },
        onDeleteExcerpt = { excerptId ->
            revealedExcerptId = null
            pendingDeleteExcerptId = excerptId
        },
        onLongPress = { excerptId ->
            revealedExcerptId = null
            onLongPress(excerptId)
        },
        onToggleSelection = onToggleSelection,
        onOpenNode = onOpenNode,
        onOpenExcerpt = onOpenExcerpt,
        onRevealExcerptActions = { excerptId -> revealedExcerptId = excerptId },
        onDismissExcerptActions = { excerptId ->
            if (revealedExcerptId == excerptId) {
                revealedExcerptId = null
            }
        },
        onDismissDeleteDialog = { pendingDeleteExcerptId = null },
        onConfirmDeleteDialog = {
            pendingDeleteExcerptId?.let(onDeleteExcerpt)
            pendingDeleteExcerptId = null
        },
    )
}
