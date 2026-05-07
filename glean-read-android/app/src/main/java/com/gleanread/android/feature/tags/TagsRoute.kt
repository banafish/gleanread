package com.gleanread.android.feature.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gleanread.android.core.model.TagGroupUiModel
import com.gleanread.android.core.model.TagUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot

@Composable
fun TagsRoute(
    snapshot: WorkspaceSnapshot,
    uiState: TagsUiState,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLongPressTag: (String) -> Unit,
    onToggleTagSelection: (String) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onConfirmDeleteDialog: () -> Unit,
    onAddTag: () -> Unit,
) {
    val filteredTagGroups = remember(snapshot.tagGroups, uiState.searchQuery) {
        filterTagGroups(
            tagGroups = snapshot.tagGroups,
            query = uiState.searchQuery,
        )
    }
    val pendingDeleteTags = remember(snapshot.tagGroups, uiState.pendingDeleteTagIds) {
        resolveTagsByIds(
            tagGroups = snapshot.tagGroups,
            tagIds = uiState.pendingDeleteTagIds,
        )
    }

    TagsScreen(
        tagGroups = filteredTagGroups,
        isSearchVisible = uiState.isSearchVisible,
        searchQuery = uiState.searchQuery,
        selectedTagIds = uiState.selectedTagIds,
        pendingDeleteTags = pendingDeleteTags,
        onToggleSearch = onToggleSearch,
        onSearchQueryChange = onSearchQueryChange,
        onLongPressTag = onLongPressTag,
        onToggleTagSelection = onToggleTagSelection,
        onDismissDeleteDialog = onDismissDeleteDialog,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onConfirmDeleteDialog = onConfirmDeleteDialog,
        onAddTag = onAddTag,
    )
}

private fun filterTagGroups(
    tagGroups: List<TagGroupUiModel>,
    query: String,
): List<TagGroupUiModel> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return tagGroups

    return tagGroups.mapNotNull { group ->
        val groupMatches = group.folder.contains(trimmedQuery, ignoreCase = true)
        val filteredItems = if (groupMatches) {
            group.items
        } else {
            group.items.filter { tag ->
                tag.displayName.contains(trimmedQuery, ignoreCase = true) ||
                    tag.fullName.contains(trimmedQuery, ignoreCase = true)
            }
        }

        if (filteredItems.isEmpty()) {
            null
        } else {
            group.copy(
                count = filteredItems.sumOf { it.heatWeight },
                items = filteredItems,
            )
        }
    }
}

private fun resolveTagsByIds(
    tagGroups: List<TagGroupUiModel>,
    tagIds: Set<String>,
): List<TagUiModel> {
    if (tagIds.isEmpty()) return emptyList()

    return tagGroups.asSequence()
        .flatMap { it.items.asSequence() }
        .filter { tagIds.contains(it.id) }
        .sortedBy(TagUiModel::fullName)
        .toList()
}
