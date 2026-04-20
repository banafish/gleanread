package com.gleanread.android.core.model

import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity

internal class ExcerptUiMapper {
    fun map(
        excerpts: List<ExcerptEntity>,
        nodeMap: Map<String, KnowledgeTreeNodeEntity>,
        tagNamesByExcerptId: Map<String, List<String>>,
    ): List<ExcerptUiModel> {
        return excerpts.map { excerpt ->
            val archivedNode = excerpt.treeNodeId?.let(nodeMap::get)
            ExcerptUiModel(
                id = excerpt.id,
                content = excerpt.content,
                thought = excerpt.userThought.orEmpty(),
                url = excerpt.url,
                sourceTitle = excerpt.sourceTitle,
                tags = tagNamesByExcerptId[excerpt.id].orEmpty(),
                archivedNodeId = archivedNode?.id,
                archivedNodeTitle = archivedNode?.nodeTitle,
                createTime = excerpt.createTime,
            )
        }
    }
}
