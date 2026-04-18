package com.gleanread.android.feature.knowledge_tree.model

import com.gleanread.android.data.model.FlatNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot

const val KNOWLEDGE_TREE_ROOT_TITLE = "知识体系"
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
): KnowledgeTreeBranchUiState? {
    val currentNode = snapshot.flatNodes[nodeId] ?: return null
    return KnowledgeTreeBranchUiState(
        currentNodeId = currentNode.id,
        title = currentNode.title,
        breadcrumbTitles = buildKnowledgeTreePathTitles(snapshot, nodeId),
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
): List<String> {
    val titles = mutableListOf<String>()
    var currentId: String? = nodeId
    while (currentId != null) {
        val current = snapshot.flatNodes[currentId] ?: break
        titles += current.title
        currentId = current.parentId
    }
    return listOf(KNOWLEDGE_TREE_ROOT_TITLE) + titles.asReversed()
}

fun buildKnowledgeTreePathText(
    snapshot: WorkspaceSnapshot,
    nodeId: String,
): String {
    return buildKnowledgeTreePathTitles(snapshot, nodeId).joinToString(" / ")
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
    val titleDestination = buildTitleDestination(
        snapshot = snapshot,
        node = node,
        remainingPreviewDepth = remainingPreviewDepth,
    )
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
        titleDestination = titleDestination,
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
    val titleDestination = buildTitleDestination(
        snapshot = snapshot,
        node = node,
        remainingPreviewDepth = remainingPreviewDepth,
    )
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
        titleDestination = titleDestination,
        detailDestination = NodeDestination.Detail(node.id),
        branchDestination = if (node.childNodeIds.isNotEmpty()) {
            NodeDestination.Branch(node.id)
        } else {
            NodeDestination.None
        },
        actionTarget = node.toActionTarget(),
    )
}

private fun buildTitleDestination(
    snapshot: WorkspaceSnapshot,
    node: FlatNodeUiModel,
    remainingPreviewDepth: Int,
): NodeDestination {
    return if (hasHiddenDescendants(snapshot, node, remainingPreviewDepth)) {
        NodeDestination.Branch(node.id)
    } else {
        NodeDestination.Detail(node.id)
    }
}

private fun hasHiddenDescendants(
    snapshot: WorkspaceSnapshot,
    node: FlatNodeUiModel,
    remainingPreviewDepth: Int,
): Boolean {
    if (node.childNodeIds.isEmpty()) {
        return false
    }
    if (remainingPreviewDepth <= 1) {
        return true
    }
    return node.childNodeIds.any { childId ->
        snapshot.flatNodes[childId]?.let { child ->
            hasHiddenDescendants(snapshot, child, remainingPreviewDepth - 1)
        } ?: false
    }
}

private fun FlatNodeUiModel.toActionTarget(): NodeActionTarget {
    return NodeActionTarget(
        nodeId = id,
        title = title,
        childCount = childNodeIds.size,
    )
}
