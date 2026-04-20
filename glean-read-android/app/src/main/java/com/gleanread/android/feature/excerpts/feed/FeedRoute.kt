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
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onLoadSample: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
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

    FeedScreen(
        filteredExcerpts = filtered,
        searchQuery = searchQuery,
        showInboxOnly = showInboxOnly,
        isSelectionMode = uiState.isSelectionMode,
        selectedExcerptIds = uiState.selectedExcerptIds,
        onSearchQueryChange = { searchQuery = it },
        onClearSearch = { searchQuery = "" },
        onToggleInboxFilter = { showInboxOnly = !showInboxOnly },
        onOpenAiSummary = onOpenAiSummary,
        onLongPress = onLongPress,
        onToggleSelection = onToggleSelection,
        onOpenNode = onOpenNode,
        onPreviewExcerpt = onPreviewExcerpt,
    )
}
