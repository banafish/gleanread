package com.gleanread.android.feature.workspace.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.repository.WorkspaceRepository
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
    val parentNodeId: String? = null,
    val newNodeTitle: String = "",
)

class AiSummaryViewModel(
    private val repository: WorkspaceRepository,
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
            val generated = repository.generateOutline(selectedIds)
            _draft.update { state ->
                state.copy(
                    title = generated.title,
                    markdown = generated.markdown,
                    newNodeTitle = generated.title,
                    isGenerating = false,
                )
            }
        }
    }

    fun updateMarkdown(value: String) {
        _draft.update { it.copy(markdown = value) }
    }

    fun updateNewNodeTitle(value: String) {
        _draft.update { it.copy(newNodeTitle = value) }
    }

    fun selectTargetNode(nodeId: String?) {
        _draft.update { it.copy(targetNodeId = nodeId) }
    }

    fun selectParentNode(nodeId: String?) {
        _draft.update { it.copy(parentNodeId = nodeId) }
    }

    fun clear() {
        _draft.value = AiSummaryDraft()
    }

    fun save(onSaved: (String) -> Unit = {}) {
        val draft = _draft.value
        if (draft.selectedExcerptIds.isEmpty() || draft.markdown.isBlank()) return
        viewModelScope.launch {
            val nodeId = repository.saveAiSummary(
                selectedExcerptIds = draft.selectedExcerptIds,
                outlineMarkdown = draft.markdown,
                targetNodeId = draft.targetNodeId,
                newNodeTitle = draft.newNodeTitle.takeIf { draft.targetNodeId == null },
                parentNodeId = draft.parentNodeId,
            )
            _draft.value = AiSummaryDraft()
            onSaved(nodeId)
        }
    }
}
