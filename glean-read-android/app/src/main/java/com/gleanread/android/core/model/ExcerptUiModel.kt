package com.gleanread.android.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class ExcerptUiModel(
    val id: String,
    val content: String,
    val thought: String,
    val url: String?,
    val sourceTitle: String?,
    val tags: List<String>,
    val archivedNodeId: String?,
    val archivedNodeTitle: String?,
    val createTime: Long,
)

fun excerptTitleFallback(excerpt: ExcerptUiModel): String {
    return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
        ?: excerpt.content.take(18).trim() + if (excerpt.content.length > 18) "..." else ""
}
