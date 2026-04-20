package com.gleanread.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.core.data.AppSnapshotStore
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.data.repository.KnowledgeTreeRepository
import com.gleanread.android.data.repository.SnapshotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainAppViewModel(
    private val snapshotRepository: SnapshotRepository,
    private val knowledgeTreeRepository: KnowledgeTreeRepository,
    snapshotStore: AppSnapshotStore,
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
            snapshotRepository.seedSampleData()
        }
    }

    fun createRootNode(title: String, onCreated: (String) -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            onCreated(knowledgeTreeRepository.createRootNode(title))
        }
    }

    fun createChildNode(parentId: String, title: String, onCreated: (String) -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            onCreated(knowledgeTreeRepository.createChildNode(parentId, title))
        }
    }

    fun renameNode(nodeId: String, title: String, onRenamed: () -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            knowledgeTreeRepository.renameNode(nodeId, title)
            onRenamed()
        }
    }

    fun deleteNodeSubtree(nodeId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            knowledgeTreeRepository.deleteNodeSubtree(nodeId)
            onDeleted()
        }
    }

    fun updateNodeOutline(nodeId: String, rawMarkdown: String) {
        viewModelScope.launch {
            knowledgeTreeRepository.updateNodeOutline(nodeId, rawMarkdown)
        }
    }

    suspend fun searchSuggestions(query: String): List<LinkSuggestion> {
        return knowledgeTreeRepository.searchSuggestions(query)
    }
}
