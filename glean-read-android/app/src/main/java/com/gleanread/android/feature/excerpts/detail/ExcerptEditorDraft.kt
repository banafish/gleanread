package com.gleanread.android.feature.excerpts.detail

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.gleanread.android.core.model.ExcerptUiModel

internal data class ExcerptEditorDraft(
    val content: String,
    val thought: String,
    val sourceTitle: String,
    val url: String,
    val selectedTagNames: List<String>,
    val archiveNodeId: String?,
) {
    companion object {
        val Saver: Saver<ExcerptEditorDraft, Any> = listSaver(
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
                ExcerptEditorDraft(
                    content = restored[0] as String,
                    thought = restored[1] as String,
                    sourceTitle = restored[2] as String,
                    url = restored[3] as String,
                    selectedTagNames = (restored[4] as List<*>).mapNotNull { it as? String },
                    archiveNodeId = (restored[5] as String).takeIf { it.isNotBlank() },
                )
            },
        )

        fun empty(archiveNodeId: String?): ExcerptEditorDraft {
            return ExcerptEditorDraft(
                content = "",
                thought = "",
                sourceTitle = "",
                url = "",
                selectedTagNames = emptyList(),
                archiveNodeId = archiveNodeId,
            )
        }

        fun from(excerpt: ExcerptUiModel): ExcerptEditorDraft {
            return ExcerptEditorDraft(
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
