package com.gleanread.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.core.data.AppSnapshotStore
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.data.repository.ExcerptRepository
import com.gleanread.android.data.repository.KnowledgeTreeRepository
import com.gleanread.android.data.repository.SeedDataInitializer
import com.gleanread.android.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainAppViewModel(
    private val excerptRepository: ExcerptRepository,
    private val tagRepository: TagRepository,
    private val knowledgeTreeRepository: KnowledgeTreeRepository,
    private val seedDataInitializer: SeedDataInitializer,
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
            seedDataInitializer.seedSampleData()
        }
    }

    fun deleteExcerpt(excerptId: String) {
        viewModelScope.launch {
            excerptRepository.deleteExcerpt(excerptId)
        }
    }

    fun createExcerpt(
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
        onCreated: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            onCreated(
                excerptRepository.createExcerpt(
                    content = content,
                    thought = thought,
                    sourceTitle = sourceTitle,
                    url = url,
                    tagNames = tagNames,
                    archiveNodeId = archiveNodeId,
                ),
            )
        }
    }

    fun updateExcerpt(
        excerptId: String,
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
        onUpdated: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            onUpdated(
                excerptRepository.updateExcerpt(
                    excerptId = excerptId,
                    content = content,
                    thought = thought,
                    sourceTitle = sourceTitle,
                    url = url,
                    tagNames = tagNames,
                    archiveNodeId = archiveNodeId,
                ),
            )
        }
    }

    fun createTag(tagName: String, onCreated: () -> Unit = {}) {
        if (tagName.isBlank()) return
        viewModelScope.launch {
            tagRepository.createTag(tagName)
            onCreated()
        }
    }

    fun deleteTags(tagIds: Set<String>, onDeleted: () -> Unit = {}) {
        if (tagIds.isEmpty()) return
        viewModelScope.launch {
            tagRepository.deleteTags(tagIds)
            onDeleted()
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

    fun moveNode(nodeId: String, parentNodeId: String?, onMoved: () -> Unit = {}) {
        viewModelScope.launch {
            knowledgeTreeRepository.moveNode(nodeId, parentNodeId)
            onMoved()
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

    fun moveExcerptToInbox(excerptId: String) {
        viewModelScope.launch {
            knowledgeTreeRepository.moveExcerptToInbox(excerptId)
        }
    }

    suspend fun searchSuggestions(query: String): List<LinkSuggestion> {
        return knowledgeTreeRepository.searchSuggestions(query)
    }
}
