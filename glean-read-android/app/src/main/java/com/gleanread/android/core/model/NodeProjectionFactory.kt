package com.gleanread.android.core.model

import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity

internal data class NodeProjection(
    val flatNodes: Map<String, FlatNodeUiModel>,
    val treeRoots: List<TreeNodeUiModel>,
)

internal class NodeProjectionFactory {
    fun create(
        nodes: List<KnowledgeTreeNodeEntity>,
        excerpts: List<ExcerptEntity>,
    ): NodeProjection {
        val excerptIdsByNodeId = excerpts.groupBy { it.treeNodeId }
            .mapValues { (_, value) -> value.map { it.id } }
        val childrenByParent = nodes.groupBy { it.parentId }

        val flatNodes = nodes.associate { node ->
            node.id to FlatNodeUiModel(
                id = node.id,
                parentId = node.parentId,
                title = node.nodeTitle,
                outlineMarkdown = node.outlineMarkdown.orEmpty(),
                excerptIds = excerptIdsByNodeId[node.id].orEmpty(),
                excerptCount = excerptIdsByNodeId[node.id].orEmpty().size,
                childNodeIds = childrenByParent[node.id].orEmpty().map(KnowledgeTreeNodeEntity::id),
            )
        }

        fun toTree(parentId: String?): List<TreeNodeUiModel> {
            return childrenByParent[parentId].orEmpty().map { node ->
                val children = toTree(node.id)
                TreeNodeUiModel(
                    id = node.id,
                    title = node.nodeTitle,
                    count = excerptIdsByNodeId[node.id].orEmpty().size,
                    children = children,
                )
            }
        }

        return NodeProjection(
            flatNodes = flatNodes,
            treeRoots = toTree(parentId = null),
        )
    }
}
