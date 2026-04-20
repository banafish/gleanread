package com.gleanread.android.core.model

import com.gleanread.android.core.richtext.extractStructuredLinks

internal class GraphBuilder {
    fun build(
        flatNodes: Map<String, FlatNodeUiModel>,
        excerptsById: Map<String, ExcerptUiModel>,
        backlinksByNodeId: Map<String, List<BacklinkUiModel>>,
    ): Map<String, GraphUiModel> {
        return flatNodes.values.associate { node ->
            val nodes = mutableListOf(
                GraphUiNode(node.id, node.title, GraphNodeKind.CURRENT_NODE),
            )
            val edges = mutableListOf<GraphUiEdge>()

            node.excerptIds.forEach { excerptId ->
                excerptsById[excerptId]?.let { excerpt ->
                    nodes.add(GraphUiNode(excerpt.id, excerptTitleFallback(excerpt), GraphNodeKind.EXCERPT))
                    edges.add(GraphUiEdge(node.id, excerpt.id))
                }
            }

            extractStructuredLinks(node.outlineMarkdown).forEach { link ->
                val linkedNode = flatNodes[link.targetId]
                if (linkedNode != null) {
                    nodes.add(GraphUiNode(linkedNode.id, linkedNode.title, GraphNodeKind.LINKED_NODE))
                    edges.add(GraphUiEdge(node.id, linkedNode.id))
                }
            }

            backlinksByNodeId[node.id].orEmpty().forEach { backlink ->
                val kind = if (backlink.sourceType == BacklinkType.NODE) {
                    GraphNodeKind.BACKLINK_NODE
                } else {
                    GraphNodeKind.EXCERPT
                }
                nodes.add(GraphUiNode(backlink.sourceId, backlink.title, kind))
                edges.add(GraphUiEdge(backlink.sourceId, node.id))
            }

            node.id to GraphUiModel(
                nodes = nodes.distinctBy { it.id },
                edges = edges.distinct(),
            )
        }
    }
}
