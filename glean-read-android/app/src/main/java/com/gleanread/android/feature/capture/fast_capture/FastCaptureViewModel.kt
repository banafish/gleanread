package com.gleanread.android.feature.capture.fast_capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.repository.ExcerptRepository
import com.gleanread.android.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FastCaptureUiState(
    val availableTags: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

class FastCaptureViewModel(
    private val tagRepository: TagRepository,
    private val excerptRepository: ExcerptRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FastCaptureUiState())
    val uiState: StateFlow<FastCaptureUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeAvailableTagNames().collect { availableTags ->
                _uiState.update { it.copy(availableTags = availableTags) }
            }
        }
    }

    fun saveQuickExcerpt(
        content: String,
        thought: String,
        url: String,
        sourceTitle: String,
        tagNames: Set<String>,
    ) {
        if (_uiState.value.isSaving || _uiState.value.isSaved) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaved = false) }

            runCatching {
                excerptRepository.createExcerpt(
                    content = content,
                    thought = thought,
                    url = url,
                    sourceTitle = sourceTitle,
                    tagNames = tagNames,
                    archiveNodeId = null,
                    autoCreateTags = true,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
