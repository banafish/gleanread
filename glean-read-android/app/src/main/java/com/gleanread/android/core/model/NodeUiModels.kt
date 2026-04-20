package com.gleanread.android.core.model

data class FlatNodeUiModel(
    val id: String,
    val parentId: String?,
    val title: String,
    val outlineMarkdown: String,
    val excerptIds: List<String>,
    val excerptCount: Int,
    val childNodeIds: List<String>,
)

data class TreeNodeUiModel(
    val id: String,
    val title: String,
    val count: Int,
    val children: List<TreeNodeUiModel>,
)
