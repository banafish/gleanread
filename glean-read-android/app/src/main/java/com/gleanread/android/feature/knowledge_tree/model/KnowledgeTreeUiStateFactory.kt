package com.gleanread.android.feature.knowledge_tree.model

import com.gleanread.android.core.model.FlatNodeUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot

const val KNOWLEDGE_TREE_HOME_PREVIEW_DEPTH = 2
const val KNOWLEDGE_TREE_BRANCH_PREVIEW_DEPTH = 3

fun buildKnowledgeTreeHomeUiState(
    snapshot: WorkspaceSnapshot,
    expandedIds: Set<String>,
): KnowledgeTreeHomeUiState {
    val rootCards = snapshot.treeRoots.mapNotNull { root ->
        buildRootNodeCard(snapshot = snapshot, nodeId = root.id, expandedIds = expandedIds)
    }
    return KnowledgeTreeHomeUiState(
        rootCards = rootCards,
        isEmpty = rootCards.isEmpty(),
    )
}

fun buildKnowledgeTreeBranchUiState(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    expandedIds: Set<String>,
    rootTitle: String,
): KnowledgeTreeBranchUiState? {
    val currentNode = snapshot.flatNodes[nodeId] ?: return null
    return KnowledgeTreeBranchUiState(
        currentNodeId = currentNode.id,
        title = currentNode.title,
        breadcrumbTitles = buildKnowledgeTreePathTitles(snapshot, nodeId, rootTitle),
        items = currentNode.childNodeIds.mapNotNull { childId ->
            buildBranchNode(
                snapshot = snapshot,
                nodeId = childId,
                expandedIds = expandedIds,
                remainingPreviewDepth = KNOWLEDGE_TREE_BRANCH_PREVIEW_DEPTH,
                depth = 1,
            )
        },
        isEmpty = currentNode.childNodeIds.isEmpty(),
        actionTarget = currentNode.toActionTarget(),
    )
}

fun buildKnowledgeTreePathTitles(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    rootTitle: String,
): List<String> {
    val titles = mutableListOf<String>()
    var currentId: String? = nodeId
    while (currentId != null) {
        val current = snapshot.flatNodes[currentId] ?: break
        titles += current.title
        currentId = current.parentId
    }
    return listOf(rootTitle) + titles.asReversed()
}

fun buildKnowledgeTreePathText(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    rootTitle: String,
): String {
    return buildKnowledgeTreePathTitles(snapshot, nodeId, rootTitle).joinToString(" / ")
}

private fun buildRootNodeCard(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    expandedIds: Set<String>,
): RootNodeCardUiModel? {
    val node = snapshot.flatNodes[nodeId] ?: return null
    val canExpand = node.childNodeIds.isNotEmpty()
    val isExpanded = canExpand && expandedIds.contains(nodeId)
    return RootNodeCardUiModel(
        nodeId = node.id,
        title = node.title,
        count = node.excerptCount,
        isExpanded = isExpanded,
        canExpand = canExpand,
        previewItems = if (isExpanded) {
            node.childNodeIds.mapNotNull { childId ->
                buildPreviewNode(
                    snapshot = snapshot,
                    nodeId = childId,
                    expandedIds = expandedIds,
                    remainingPreviewDepth = KNOWLEDGE_TREE_HOME_PREVIEW_DEPTH,
                    depth = 2,
                )
            }
        } else {
            emptyList()
        },
        detailDestination = NodeDestination.Detail(node.id),
        branchDestination = NodeDestination.Branch(node.id),
        actionTarget = node.toActionTarget(),
    )
}

private fun buildPreviewNode(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    expandedIds: Set<String>,
    remainingPreviewDepth: Int,
    depth: Int,
): PreviewNodeUiModel? {
    val node = snapshot.flatNodes[nodeId] ?: return null
    val canExpand = remainingPreviewDepth > 1 && node.childNodeIds.isNotEmpty()
    val isExpanded = canExpand && expandedIds.contains(node.id)
    return PreviewNodeUiModel(
        nodeId = node.id,
        title = node.title,
        count = node.excerptCount,
        depth = depth,
        isExpanded = isExpanded,
        canExpand = canExpand,
        visibleChildren = if (isExpanded) {
            node.childNodeIds.mapNotNull { childId ->
                buildPreviewNode(
                    snapshot = snapshot,
                    nodeId = childId,
                    expandedIds = expandedIds,
                    remainingPreviewDepth = remainingPreviewDepth - 1,
                    depth = depth + 1,
                )
            }
        } else {
            emptyList()
        },
        showEnterBranch = node.childNodeIds.isNotEmpty() && !canExpand,
        titleDestination = NodeDestination.Detail(node.id),
        detailDestination = NodeDestination.Detail(node.id),
        branchDestination = if (node.childNodeIds.isNotEmpty()) {
            NodeDestination.Branch(node.id)
        } else {
            NodeDestination.None
        },
        actionTarget = node.toActionTarget(),
    )
}

private fun buildBranchNode(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
    expandedIds: Set<String>,
    remainingPreviewDepth: Int,
    depth: Int,
): BranchNodeUiModel? {
    val node = snapshot.flatNodes[nodeId] ?: return null
    val canExpand = remainingPreviewDepth > 1 && node.childNodeIds.isNotEmpty()
    val isExpanded = canExpand && expandedIds.contains(node.id)
    return BranchNodeUiModel(
        nodeId = node.id,
        title = node.title,
        count = node.excerptCount,
        depth = depth,
        isExpanded = isExpanded,
        canExpand = canExpand,
        visibleChildren = if (isExpanded) {
            node.childNodeIds.mapNotNull { childId ->
                buildBranchNode(
                    snapshot = snapshot,
                    nodeId = childId,
                    expandedIds = expandedIds,
                    remainingPreviewDepth = remainingPreviewDepth - 1,
                    depth = depth + 1,
                )
            }
        } else {
            emptyList()
        },
        showEnterBranch = node.childNodeIds.isNotEmpty() && !canExpand,
        titleDestination = NodeDestination.Detail(node.id),
        detailDestination = NodeDestination.Detail(node.id),
        branchDestination = if (node.childNodeIds.isNotEmpty()) {
            NodeDestination.Branch(node.id)
        } else {
            NodeDestination.None
        },
        actionTarget = node.toActionTarget(),
    )
}

private fun FlatNodeUiModel.toActionTarget(): NodeActionTarget {
    return NodeActionTarget(
        nodeId = id,
        title = title,
        childCount = childNodeIds.size,
    )
}
