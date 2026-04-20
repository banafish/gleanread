package com.gleanread.android.feature.tags

import androidx.compose.runtime.Composable
import com.gleanread.android.core.model.TagGroupUiModel

@Composable
fun TagsRoute(
    tagGroups: List<TagGroupUiModel>,
) {
    TagsScreen(tagGroups = tagGroups)
}
