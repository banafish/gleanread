package com.gleanread.android.feature.excerpts.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeNodePickerUiModel

@Composable
fun NewExcerptRoute(
    snapshot: WorkspaceSnapshot,
    initialArchiveNodeId: String?,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenExcerpt: (String) -> Unit,
    onCreatedExcerpt: (String) -> Unit,
    onCreateExcerpt: (
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
        onCreated: (String) -> Unit,
    ) -> Unit,
) {
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val resolvedInitialArchiveNodeId = remember(snapshot.flatNodes, initialArchiveNodeId) {
        initialArchiveNodeId?.takeIf(snapshot.flatNodes::containsKey)
    }
    var draft by rememberSaveable(
        resolvedInitialArchiveNodeId,
        stateSaver = ExcerptEditorDraft.Saver,
    ) {
        mutableStateOf(ExcerptEditorDraft.empty(resolvedInitialArchiveNodeId))
    }
    var isTagPickerOpen by rememberSaveable(resolvedInitialArchiveNodeId) { mutableStateOf(false) }
    var isMountPickerOpen by rememberSaveable(resolvedInitialArchiveNodeId) { mutableStateOf(false) }
    var mountPickerCurrentNodeId by rememberSaveable(resolvedInitialArchiveNodeId) {
        mutableStateOf(resolvedInitialArchiveNodeId)
    }
    val mountPickerUiModel = remember(snapshot, mountPickerCurrentNodeId, rootTitle) {
        buildKnowledgeTreeNodePickerUiModel(
            snapshot = snapshot,
            currentNodeId = mountPickerCurrentNodeId,
            rootTitle = rootTitle,
        )
    } ?: return
    val selectedArchiveNodeTitle = draft.archiveNodeId?.let { nodeId ->
        snapshot.flatNodes[nodeId]?.title
    }
    val placeholderExcerpt = remember(draft.archiveNodeId, selectedArchiveNodeTitle) {
        ExcerptUiModel(
            id = NewExcerptPlaceholderId,
            content = "",
            thought = "",
            url = null,
            sourceTitle = null,
            tags = emptyList(),
            archivedNodeId = draft.archiveNodeId,
            archivedNodeTitle = selectedArchiveNodeTitle,
            createTime = 0L,
        )
    }
    val canSave = draft.content.trim().isNotBlank()
    val onOpenLinkedTarget: (String) -> Unit = { targetId ->
        when {
            snapshot.excerptsById.containsKey(targetId) -> onOpenExcerpt(targetId)
            snapshot.flatNodes.containsKey(targetId) -> onOpenNode(targetId)
        }
    }

    ExcerptDetailScreen(
        excerpt = placeholderExcerpt,
        content = draft.content,
        thought = draft.thought,
        sourceTitle = draft.sourceTitle,
        url = draft.url,
        selectedTagNames = draft.selectedTagNames,
        archiveNodeId = draft.archiveNodeId,
        archiveNodeTitle = selectedArchiveNodeTitle,
        tagGroups = snapshot.tagGroups,
        breadcrumbs = mountPickerUiModel.breadcrumbs,
        destinations = mountPickerUiModel.destinations,
        isEditing = true,
        isCreateMode = true,
        canSave = canSave,
        isTagPickerOpen = isTagPickerOpen,
        isMountPickerOpen = isMountPickerOpen,
        mountPickerCurrentNodeId = mountPickerCurrentNodeId,
        searchSuggestions = searchSuggestions,
        onBack = onBack,
        onCloseEditing = onBack,
        onStartEditing = {},
        onSave = {
            onCreateExcerpt(
                draft.content.trim(),
                draft.thought.trim(),
                draft.sourceTitle.trim().takeIf { it.isNotBlank() },
                draft.url.trim().takeIf { it.isNotBlank() },
                draft.selectedTagNames.toSet(),
                draft.archiveNodeId,
            ) { createdExcerptId ->
                if (createdExcerptId.isNotBlank()) {
                    onCreatedExcerpt(createdExcerptId)
                }
            }
        },
        onContentChange = { draft = draft.copy(content = it) },
        onThoughtChange = { draft = draft.copy(thought = it) },
        onSourceTitleChange = { draft = draft.copy(sourceTitle = it) },
        onUrlChange = { draft = draft.copy(url = it) },
        onOpenArchiveNode = onOpenNode,
        onOpenLinkedTarget = onOpenLinkedTarget,
        onOpenTagPicker = { isTagPickerOpen = true },
        onDismissTagPicker = { isTagPickerOpen = false },
        onToggleTag = { tagName ->
            val updatedTags = draft.selectedTagNames.toMutableSet().apply {
                if (!add(tagName)) {
                    remove(tagName)
                }
            }
            draft = draft.copy(selectedTagNames = updatedTags.sorted())
        },
        onOpenMountPicker = {
            mountPickerCurrentNodeId = draft.archiveNodeId
            isMountPickerOpen = true
        },
        onDismissMountPicker = {
            mountPickerCurrentNodeId = draft.archiveNodeId
            isMountPickerOpen = false
        },
        onNavigateMountPicker = { mountPickerCurrentNodeId = it },
        onConfirmMountPicker = {
            draft = draft.copy(archiveNodeId = mountPickerCurrentNodeId)
            isMountPickerOpen = false
        },
    )
}

private const val NewExcerptPlaceholderId = "new-excerpt"
