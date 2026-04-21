package com.gleanread.android.feature.tags

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TagsUiState(
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val selectedTagIds: Set<String> = emptySet(),
    val pendingDeleteTagIds: Set<String> = emptySet(),
) {
    val isSelectionMode: Boolean
        get() = selectedTagIds.isNotEmpty()
}

class TagsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    fun toggleSearch() {
        _uiState.update { state ->
            val nextVisible = !state.isSearchVisible
            state.copy(
                isSearchVisible = nextVisible,
                searchQuery = if (nextVisible) state.searchQuery else "",
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun enterSelectionMode(tagId: String) {
        _uiState.update { state ->
            state.copy(
                selectedTagIds = state.selectedTagIds + tagId,
                pendingDeleteTagIds = emptySet(),
            )
        }
    }

    fun toggleTagSelection(tagId: String) {
        _uiState.update { state ->
            val nextSelection = if (state.selectedTagIds.contains(tagId)) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(
                selectedTagIds = nextSelection,
                pendingDeleteTagIds = emptySet(),
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedTagIds = emptySet(),
                pendingDeleteTagIds = emptySet(),
            )
        }
    }

    fun promptDeleteSelected() {
        _uiState.update { state ->
            if (state.selectedTagIds.isEmpty()) {
                state
            } else {
                state.copy(pendingDeleteTagIds = state.selectedTagIds)
            }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(pendingDeleteTagIds = emptySet()) }
    }
}
