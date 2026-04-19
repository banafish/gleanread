package com.gleanread.android.feature.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.data.repository.WorkspaceRepository
import com.gleanread.android.feature.workspace.data.WorkspaceSnapshotStore
import com.gleanread.android.feature.workspace.model.WorkspaceSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkspaceViewModel(
    private val repository: WorkspaceRepository,
    snapshotStore: WorkspaceSnapshotStore,
) : ViewModel() {
    private val _snapshot = MutableStateFlow(WorkspaceSnapshot.Empty)
    val snapshot: StateFlow<WorkspaceSnapshot> = _snapshot.asStateFlow()

    init {
        viewModelScope.launch {
            snapshotStore.snapshot.collect { latest ->
                _snapshot.value = latest
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
        }
    }

    fun createRootNode(title: String, onCreated: (String) -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            onCreated(repository.createRootNode(title))
        }
    }

    fun createChildNode(parentId: String, title: String, onCreated: (String) -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            onCreated(repository.createChildNode(parentId, title))
        }
    }

    fun renameNode(nodeId: String, title: String, onRenamed: () -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.renameNode(nodeId, title)
            onRenamed()
        }
    }

    fun deleteNodeSubtree(nodeId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNodeSubtree(nodeId)
            onDeleted()
        }
    }

    fun updateNodeOutline(nodeId: String, rawMarkdown: String) {
        viewModelScope.launch {
            repository.updateNodeOutline(nodeId, rawMarkdown)
        }
    }

    suspend fun searchSuggestions(query: String): List<LinkSuggestion> {
        return repository.searchSuggestions(query)
    }
}
