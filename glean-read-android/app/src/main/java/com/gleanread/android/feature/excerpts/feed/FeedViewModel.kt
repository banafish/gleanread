package com.gleanread.android.feature.excerpts.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class FeedUiState(
    val isSelectionMode: Boolean = false,
    val selectedExcerptIds: Set<String> = emptySet(),
)

class FeedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    fun enterSelectionMode(excerptId: String) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedExcerptIds = it.selectedExcerptIds + excerptId,
            )
        }
    }

    fun toggleExcerptSelection(excerptId: String) {
        _uiState.update { state ->
            val next = if (state.selectedExcerptIds.contains(excerptId)) {
                state.selectedExcerptIds - excerptId
            } else {
                state.selectedExcerptIds + excerptId
            }
            state.copy(
                selectedExcerptIds = next,
                isSelectionMode = next.isNotEmpty(),
            )
        }
    }

    fun clearSelection() {
        _uiState.value = FeedUiState()
    }
}
