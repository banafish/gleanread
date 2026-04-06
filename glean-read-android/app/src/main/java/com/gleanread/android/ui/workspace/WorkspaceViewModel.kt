package com.gleanread.android.ui.workspace

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.data.model.OutlineDraft
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.data.repository.WorkspaceRepository
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

data class AiSummaryDraft(
    val selectedExcerptIds: List<String> = emptyList(),
    val title: String = "",
    val markdown: String = "",
    val isGenerating: Boolean = false,
    val targetNodeId: String? = null,
    val parentNodeId: String? = null,
    val newNodeTitle: String = "",
)

data class WorkspaceUiState(
    val snapshot: WorkspaceSnapshot = WorkspaceSnapshot.Empty,
    val isSelectionMode: Boolean = false,
    val selectedExcerptIds: Set<String> = emptySet(),
    val isQuickCaptureOpen: Boolean = false,
    val quickCaptureDraft: QuickCaptureDraft = QuickCaptureDraft(),
    val aiSummaryDraft: AiSummaryDraft = AiSummaryDraft(),
)

class WorkspaceViewModel(
    private val repository: WorkspaceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.snapshot.collect { snapshot ->
                _uiState.update { it.copy(snapshot = snapshot) }
            }
        }
    }

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
        _uiState.update {
            it.copy(isSelectionMode = false, selectedExcerptIds = emptySet())
        }
    }

    fun openQuickCapture() {
        _uiState.update { it.copy(isQuickCaptureOpen = true) }
    }

    fun closeQuickCapture() {
        _uiState.update {
            it.copy(isQuickCaptureOpen = false, quickCaptureDraft = QuickCaptureDraft())
        }
    }

    fun updateQuickCaptureContent(value: String) {
        _uiState.update { it.copy(quickCaptureDraft = it.quickCaptureDraft.copy(content = value)) }
    }

    fun updateQuickCaptureThought(value: String) {
        _uiState.update { it.copy(quickCaptureDraft = it.quickCaptureDraft.copy(thought = value)) }
    }

    fun updateQuickCaptureUrl(value: String) {
        _uiState.update { it.copy(quickCaptureDraft = it.quickCaptureDraft.copy(url = value)) }
    }

    fun toggleQuickCaptureTag(value: String) {
        val normalized = value.removePrefix("#")
        _uiState.update { state ->
            val selected = if (state.quickCaptureDraft.selectedTags.contains(normalized)) {
                state.quickCaptureDraft.selectedTags - normalized
            } else {
                state.quickCaptureDraft.selectedTags + normalized
            }
            state.copy(quickCaptureDraft = state.quickCaptureDraft.copy(selectedTags = selected))
        }
    }

    fun setQuickCaptureArchiveNode(nodeId: String?) {
        _uiState.update { it.copy(quickCaptureDraft = it.quickCaptureDraft.copy(archiveNodeId = nodeId)) }
    }

    fun saveQuickCapture(onSaved: (String) -> Unit = {}) {
        val draft = _uiState.value.quickCaptureDraft
        if (draft.content.isBlank()) return
        viewModelScope.launch {
            val excerptId = repository.saveQuickExcerpt(
                content = draft.content,
                thought = draft.thought,
                url = draft.url,
                tagNames = draft.selectedTags.toList(),
                archiveNodeId = draft.archiveNodeId,
            )
            _uiState.update {
                it.copy(isQuickCaptureOpen = false, quickCaptureDraft = QuickCaptureDraft())
            }
            onSaved(excerptId)
        }
    }

    fun loadSampleData() {
        viewModelScope.launch { repository.seedSampleData() }
    }

    fun prepareAiSummary(selectedIds: List<String>? = null) {
        val targetIds = selectedIds ?: _uiState.value.selectedExcerptIds.toList()
        if (targetIds.isEmpty()) return
        _uiState.update {
            it.copy(
                aiSummaryDraft = AiSummaryDraft(
                    selectedExcerptIds = targetIds,
                    isGenerating = true,
                )
            )
        }
        viewModelScope.launch {
            val draft = repository.generateOutline(targetIds)
            _uiState.update { state ->
                state.copy(
                    aiSummaryDraft = state.aiSummaryDraft.copy(
                        title = draft.title,
                        markdown = draft.markdown,
                        newNodeTitle = draft.title,
                        isGenerating = false,
                    )
                )
            }
        }
    }

    fun updateAiMarkdown(value: String) {
        _uiState.update { it.copy(aiSummaryDraft = it.aiSummaryDraft.copy(markdown = value)) }
    }

    fun updateAiNewNodeTitle(value: String) {
        _uiState.update { it.copy(aiSummaryDraft = it.aiSummaryDraft.copy(newNodeTitle = value)) }
    }

    fun selectAiTargetNode(nodeId: String?) {
        _uiState.update {
            it.copy(aiSummaryDraft = it.aiSummaryDraft.copy(targetNodeId = nodeId))
        }
    }

    fun selectAiParentNode(nodeId: String?) {
        _uiState.update {
            it.copy(aiSummaryDraft = it.aiSummaryDraft.copy(parentNodeId = nodeId))
        }
    }

    fun clearAiSummary() {
        _uiState.update { it.copy(aiSummaryDraft = AiSummaryDraft()) }
    }

    fun saveAiSummary(onSaved: (String) -> Unit = {}) {
        val draft = _uiState.value.aiSummaryDraft
        if (draft.selectedExcerptIds.isEmpty() || draft.markdown.isBlank()) return
        viewModelScope.launch {
            val nodeId = repository.saveAiSummary(
                selectedExcerptIds = draft.selectedExcerptIds,
                outlineMarkdown = draft.markdown,
                targetNodeId = draft.targetNodeId,
                newNodeTitle = draft.newNodeTitle.takeIf { draft.targetNodeId == null },
                parentNodeId = draft.parentNodeId,
            )
            _uiState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedExcerptIds = emptySet(),
                    aiSummaryDraft = AiSummaryDraft(),
                )
            }
            onSaved(nodeId)
        }
    }

    fun createRootNode(title: String, onCreated: (String) -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            onCreated(repository.createRootNode(title))
        }
    }

    fun updateNodeOutline(nodeId: String, rawMarkdown: String) {
        viewModelScope.launch { repository.updateNodeOutline(nodeId, rawMarkdown) }
    }

    suspend fun searchSuggestions(query: String): List<LinkSuggestion> {
        return repository.searchSuggestions(query)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val repository = WorkspaceRepository(WorkspaceDatabase.get(context))
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return WorkspaceViewModel(repository) as T
                }
            }
        }
    }
}
