package com.gleanread.android.feature.excerpts.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.repository.AiSummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiSummaryDraft(
    val selectedExcerptIds: List<String> = emptyList(),
    val title: String = "",
    val markdown: String = "",
    val isGenerating: Boolean = false,
    val targetNodeId: String? = null,
)

class AiSummaryViewModel(
    private val aiSummaryRepository: AiSummaryRepository,
) : ViewModel() {
    private val _draft = MutableStateFlow(AiSummaryDraft())
    val draft: StateFlow<AiSummaryDraft> = _draft.asStateFlow()

    fun prepare(selectedIds: List<String>) {
        if (selectedIds.isEmpty()) return
        _draft.value = AiSummaryDraft(
            selectedExcerptIds = selectedIds,
            isGenerating = true,
        )
        viewModelScope.launch {
            val generated = aiSummaryRepository.generateOutline(selectedIds)
            _draft.update { state ->
                state.copy(
                    title = generated.title,
                    markdown = generated.markdown,
                    isGenerating = false,
                )
            }
        }
    }

    fun updateMarkdown(value: String) {
        _draft.update { it.copy(markdown = value) }
    }

    fun selectTargetNode(nodeId: String?) {
        _draft.update { it.copy(targetNodeId = nodeId) }
    }

    fun clear() {
        _draft.value = AiSummaryDraft()
    }

    fun save(onSaved: (String) -> Unit = {}) {
        val draft = _draft.value
        val targetNodeId = draft.targetNodeId ?: return
        if (draft.selectedExcerptIds.isEmpty() || draft.markdown.isBlank()) return
        viewModelScope.launch {
            val nodeId = aiSummaryRepository.saveAiSummary(
                selectedExcerptIds = draft.selectedExcerptIds,
                outlineMarkdown = draft.markdown,
                targetNodeId = targetNodeId,
            )
            if (nodeId.isNotBlank()) {
                _draft.value = AiSummaryDraft()
                onSaved(nodeId)
            }
        }
    }
}
