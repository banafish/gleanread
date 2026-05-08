package com.gleanread.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gleanread.android.core.data.AppSnapshotStore
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
import com.gleanread.android.data.auth.SupabaseAuthRepository
import com.gleanread.android.data.repository.ExcerptRepository
import com.gleanread.android.data.repository.KnowledgeTreeRepository
import com.gleanread.android.data.repository.SeedDataInitializer
import com.gleanread.android.data.repository.TagRepository
import com.gleanread.android.data.sync.WorkspaceSyncRepository
import com.gleanread.android.data.sync.WorkspaceSyncUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainAppViewModel(
    private val authRepository: SupabaseAuthRepository,
    private val excerptRepository: ExcerptRepository,
    private val tagRepository: TagRepository,
    private val knowledgeTreeRepository: KnowledgeTreeRepository,
    private val seedDataInitializer: SeedDataInitializer,
    private val syncRepository: WorkspaceSyncRepository,
    snapshotStore: AppSnapshotStore,
    private val backgroundSyncScope: CoroutineScope,
) : ViewModel() {
    private val _snapshot = MutableStateFlow(WorkspaceSnapshot.Empty)
    private val _localDataOwnershipUiState = MutableStateFlow(LocalDataOwnershipUiState())
    val snapshot: StateFlow<WorkspaceSnapshot> = _snapshot.asStateFlow()
    val syncState: StateFlow<WorkspaceSyncUiState> = syncRepository.syncState
    val localDataOwnershipUiState: StateFlow<LocalDataOwnershipUiState> = _localDataOwnershipUiState.asStateFlow()

    init {
        viewModelScope.launch {
            snapshotStore.snapshot.collect { latest ->
                _snapshot.value = latest
            }
        }
        viewModelScope.launch {
            authRepository.pendingLocalDataOwnership.collect { pending ->
                _localDataOwnershipUiState.update {
                    it.copy(isDialogVisible = pending, isSubmitting = false)
                }
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            seedDataInitializer.seedSampleData()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncRepository.syncNow()
        }
    }

    fun choosePendingLocalDataOwnership(
        choice: LocalDataOwnershipChoice,
        onFinished: () -> Unit = {},
    ) {
        if (_localDataOwnershipUiState.value.isSubmitting) return
        viewModelScope.launch {
            _localDataOwnershipUiState.update { it.copy(isSubmitting = true) }
            when (choice) {
                LocalDataOwnershipChoice.MERGE_TO_ACCOUNT -> {
                    authRepository.mergeLocalDataIntoCurrentAccount()
                    startBackgroundSync()
                }

                LocalDataOwnershipChoice.KEEP_LOCAL -> {
                    startBackgroundSync()
                }

                LocalDataOwnershipChoice.USE_CLOUD -> {
                    authRepository.clearLocalWorkspaceData()
                    startBackgroundSync()
                }
            }
            authRepository.clearLocalDataOwnershipRequest()
            _localDataOwnershipUiState.update {
                it.copy(isDialogVisible = false, isSubmitting = false)
            }
            onFinished()
        }
    }

    private fun startBackgroundSync() {
        syncRepository.setCloudSyncEnabled(true)
        backgroundSyncScope.launch {
            syncRepository.syncNow(repairMissingRemote = true)
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

    fun moveNodeToPosition(nodeId: String, targetIndex: Int) {
        viewModelScope.launch {
            knowledgeTreeRepository.moveNodeToPosition(nodeId, targetIndex)
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

data class LocalDataOwnershipUiState(
    val isDialogVisible: Boolean = false,
    val isSubmitting: Boolean = false,
)
