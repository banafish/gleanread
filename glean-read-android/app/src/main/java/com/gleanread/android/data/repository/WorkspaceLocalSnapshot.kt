package com.gleanread.android.data.repository

import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity

data class WorkspaceLocalSnapshot(
    val excerpts: List<ExcerptEntity>,
    val nodes: List<KnowledgeTreeNodeEntity>,
    val tags: List<TagEntity>,
    val relations: List<ExcerptTagEntity>,
)
