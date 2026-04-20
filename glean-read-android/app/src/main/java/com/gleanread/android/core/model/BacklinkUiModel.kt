package com.gleanread.android.core.model

enum class BacklinkType {
    NODE,
    EXCERPT,
}

data class BacklinkUiModel(
    val sourceId: String,
    val title: String,
    val sourceType: BacklinkType,
    val snippet: String,
)
