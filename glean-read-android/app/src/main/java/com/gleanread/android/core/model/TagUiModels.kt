package com.gleanread.android.core.model

data class TagUiModel(
    val id: String,
    val folder: String,
    val displayName: String,
    val fullName: String,
    val heatWeight: Int,
)

data class TagGroupUiModel(
    val folder: String,
    val count: Int,
    val items: List<TagUiModel>,
)

data class SuggestedTagUiModel(
    val fullName: String,
    val label: String,
)
