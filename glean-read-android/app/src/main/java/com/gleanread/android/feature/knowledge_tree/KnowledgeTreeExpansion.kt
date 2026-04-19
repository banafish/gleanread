package com.gleanread.android.feature.knowledge_tree

import com.gleanread.android.feature.workspace.model.WorkspaceSnapshot

internal fun collectExpandablePreviewIds(
    snapshot: WorkspaceSnapshot,
    nodeIds: List<String>,
    remainingPreviewDepth: Int,
): Set<String> {
    if (remainingPreviewDepth <= 1) {
        return emptySet()
    }

    return buildSet {
        nodeIds.forEach { nodeId ->
            val node = snapshot.flatNodes[nodeId] ?: return@forEach
            if (node.childNodeIds.isEmpty()) {
                return@forEach
            }

            add(node.id)
            addAll(
                collectExpandablePreviewIds(
                    snapshot = snapshot,
                    nodeIds = node.childNodeIds,
                    remainingPreviewDepth = remainingPreviewDepth - 1,
                ),
            )
        }
    }
}
