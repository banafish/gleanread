package com.gleanread.android.core.model

import com.gleanread.android.data.local.TagEntity

internal data class TagProjection(
    val tagGroups: List<TagGroupUiModel>,
    val suggestedTags: List<SuggestedTagUiModel>,
)

internal class TagProjectionFactory {
    fun create(tags: List<TagEntity>): TagProjection {
        return TagProjection(
            tagGroups = buildTagGroups(tags),
            suggestedTags = tags.sortedByDescending { it.heatWeight }.take(6).map {
                SuggestedTagUiModel(
                    fullName = it.tagName,
                    label = normalizeTagLabel(it.tagName),
                )
            },
        )
    }

    private fun buildTagGroups(tags: List<TagEntity>): List<TagGroupUiModel> {
        return tags.groupBy { folderName(it.tagName) }
            .map { (folder, group) ->
                TagGroupUiModel(
                    folder = folder,
                    count = group.sumOf { it.heatWeight },
                    items = group.sortedByDescending { it.heatWeight }.map { tag ->
                        TagUiModel(
                            id = tag.id,
                            folder = folder,
                            displayName = displayTagName(tag.tagName),
                            fullName = tag.tagName,
                            heatWeight = tag.heatWeight,
                        )
                    },
                )
            }
            .sortedByDescending { it.count }
    }

    private fun folderName(tagName: String): String {
        return if (tagName.contains('/')) tagName.substringBefore('/') else "Uncategorized"
    }

    private fun displayTagName(tagName: String): String {
        return if (tagName.contains('/')) tagName.substringAfterLast('/') else tagName
    }

    private fun normalizeTagLabel(tagName: String): String = "#${displayTagName(tagName)}"
}
