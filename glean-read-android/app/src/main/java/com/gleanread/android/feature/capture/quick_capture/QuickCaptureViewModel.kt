package com.gleanread.android.feature.capture.quick_capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.repository.ExcerptCaptureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickCaptureDraft(
    val content: String = "",
    val thought: String = "",
    val url: String = "",
    val selectedTags: Set<String> = emptySet(),
    val archiveNodeId: String? = null,
)

data class QuickCaptureUiState(
    val isOpen: Boolean = false,
    val draft: QuickCaptureDraft = QuickCaptureDraft(),
)

class QuickCaptureViewModel(
    private val captureRepository: ExcerptCaptureRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    fun open() {
        _uiState.update { it.copy(isOpen = true) }
    }

    fun close() {
        _uiState.value = QuickCaptureUiState()
    }

    fun updateContent(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(content = value)) }
    }

    fun updateThought(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(thought = value)) }
    }

    fun updateUrl(value: String) {
        _uiState.update { it.copy(draft = it.draft.copy(url = value)) }
    }

    fun toggleTag(value: String) {
        val normalized = value.removePrefix("#")
        _uiState.update { state ->
            val selected = if (state.draft.selectedTags.contains(normalized)) {
                state.draft.selectedTags - normalized
            } else {
                state.draft.selectedTags + normalized
            }
            state.copy(draft = state.draft.copy(selectedTags = selected))
        }
    }

    fun setArchiveNode(nodeId: String?) {
        _uiState.update { it.copy(draft = it.draft.copy(archiveNodeId = nodeId)) }
    }

    fun openForNode(nodeId: String?) {
        _uiState.update {
            it.copy(
                isOpen = true,
                draft = it.draft.copy(archiveNodeId = nodeId),
            )
        }
    }

    fun save(onSaved: (String) -> Unit = {}) {
        val draft = _uiState.value.draft
        if (draft.content.isBlank()) return
        viewModelScope.launch {
            val excerptId = captureRepository.saveQuickExcerpt(
                content = draft.content,
                thought = draft.thought,
                url = draft.url,
                sourceTitle = null,
                tagNames = draft.selectedTags.toList(),
                archiveNodeId = draft.archiveNodeId,
            )
            _uiState.value = QuickCaptureUiState()
            onSaved(excerptId)
        }
    }
}

