package com.gleanread.android.data.model

import com.gleanread.android.core.richtext.LinkSuggestionType

data class LocalSuggestionCandidate(
    val id: String,
    val title: String,
    val preview: String,
    val type: LinkSuggestionType,
)
