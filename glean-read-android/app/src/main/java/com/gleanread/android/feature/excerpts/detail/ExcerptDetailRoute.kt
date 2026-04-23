package com.gleanread.android.feature.excerpts.detail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreeNodePickerUiModel

@Composable
fun ExcerptDetailRoute(
    snapshot: WorkspaceSnapshot,
    excerptId: String,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenExcerpt: (String) -> Unit,
    onSaveExcerpt: (
        excerptId: String,
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
        onUpdated: (Boolean) -> Unit,
    ) -> Unit,
) {
    val excerpt = snapshot.excerptsById[excerptId] ?: return
    val rootTitle = stringResource(R.string.knowledge_tree_root_title)
    val baselineDraft = remember(
        excerpt.content,
        excerpt.thought,
        excerpt.sourceTitle,
        excerpt.url,
        excerpt.tags,
        excerpt.archivedNodeId,
    ) {
        ExcerptDetailDraft.from(excerpt)
    }
    var isEditing by rememberSaveable(excerptId) { mutableStateOf(false) }
    var draft by rememberSaveable(excerptId, stateSaver = ExcerptDetailDraft.Saver) {
        mutableStateOf(baselineDraft)
    }
    var isTagPickerOpen by rememberSaveable(excerptId) { mutableStateOf(false) }
    var isMountPickerOpen by rememberSaveable(excerptId) { mutableStateOf(false) }
    var mountPickerCurrentNodeId by rememberSaveable(excerptId) {
        mutableStateOf(excerpt.archivedNodeId)
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
    val resetEditor = {
        draft = baselineDraft
        isEditing = false
        isTagPickerOpen = false
        isMountPickerOpen = false
        mountPickerCurrentNodeId = excerpt.archivedNodeId
    }
    val canSave = draft.content.trim().isNotBlank() && draft != baselineDraft
    val onOpenLinkedTarget: (String) -> Unit = { targetId ->
        when {
            targetId == excerpt.id -> Unit
            snapshot.excerptsById.containsKey(targetId) -> onOpenExcerpt(targetId)
            snapshot.flatNodes.containsKey(targetId) -> onOpenNode(targetId)
        }
    }

    LaunchedEffect(baselineDraft, isEditing) {
        if (!isEditing && draft != baselineDraft) {
            draft = baselineDraft
        }
    }
    LaunchedEffect(excerpt.archivedNodeId, isEditing, isMountPickerOpen) {
        if (!isEditing && !isMountPickerOpen && mountPickerCurrentNodeId != excerpt.archivedNodeId) {
            mountPickerCurrentNodeId = excerpt.archivedNodeId
        }
    }

    BackHandler(enabled = isEditing) {
        resetEditor()
    }

    ExcerptDetailScreen(
        excerpt = excerpt,
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
        isEditing = isEditing,
        canSave = canSave,
        isTagPickerOpen = isTagPickerOpen,
        isMountPickerOpen = isMountPickerOpen,
        mountPickerCurrentNodeId = mountPickerCurrentNodeId,
        searchSuggestions = searchSuggestions,
        onBack = onBack,
        onCloseEditing = resetEditor,
        onStartEditing = {
            draft = baselineDraft
            mountPickerCurrentNodeId = excerpt.archivedNodeId
            isEditing = true
        },
        onSave = {
            onSaveExcerpt(
                excerptId,
                draft.content.trim(),
                draft.thought.trim(),
                draft.sourceTitle.trim().takeIf { it.isNotBlank() },
                draft.url.trim().takeIf { it.isNotBlank() },
                draft.selectedTagNames.toSet(),
                draft.archiveNodeId,
            ) { updated ->
                if (updated) {
                    isEditing = false
                    isTagPickerOpen = false
                    isMountPickerOpen = false
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

private data class ExcerptDetailDraft(
    val content: String,
    val thought: String,
    val sourceTitle: String,
    val url: String,
    val selectedTagNames: List<String>,
    val archiveNodeId: String?,
) {
    companion object {
        val Saver: Saver<ExcerptDetailDraft, Any> = listSaver(
            save = { draft ->
                listOf(
                    draft.content,
                    draft.thought,
                    draft.sourceTitle,
                    draft.url,
                    draft.selectedTagNames,
                    draft.archiveNodeId.orEmpty(),
                )
            },
            restore = { restored ->
                ExcerptDetailDraft(
                    content = restored[0] as String,
                    thought = restored[1] as String,
                    sourceTitle = restored[2] as String,
                    url = restored[3] as String,
                    selectedTagNames = (restored[4] as List<*>).mapNotNull { it as? String },
                    archiveNodeId = (restored[5] as String).takeIf { it.isNotBlank() },
                )
            },
        )

        fun from(excerpt: ExcerptUiModel): ExcerptDetailDraft {
            return ExcerptDetailDraft(
                content = excerpt.content,
                thought = excerpt.thought,
                sourceTitle = excerpt.sourceTitle.orEmpty(),
                url = excerpt.url.orEmpty(),
                selectedTagNames = excerpt.tags.sorted(),
                archiveNodeId = excerpt.archivedNodeId,
            )
        }
    }
}
