package com.gleanread.android.feature.knowledge_tree.model

import androidx.compose.foundation.lazy.LazyListState

/**
 * 根据当前拖拽 Y 偏移量和列表状态，计算 drop 目标位置。
 * @param listState LazyColumn 的状态
 * @param draggedNodeId 正在拖拽的节点 ID
 * @param nodeIds 列表中所有节点的 ID（按显示顺序，不含 header item）
 * @param dragOffsetY 累积的 Y 轴拖拽偏移量
 * @param firstNodeItemIndex LazyColumn 中第一个节点 item 的全局 index（如果有 header item 则 > 0）
 * @return DropTargetInfo 或 null（如果无有效目标）
 */
fun calculateDropTarget(
    listState: LazyListState,
    draggedNodeId: String?,
    nodeIds: List<String>,
    dragOffsetY: Float,
    firstNodeItemIndex: Int = 0,
): DropTargetInfo? {
    if (draggedNodeId == null || nodeIds.isEmpty()) return null

    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val draggedNodeIndex = nodeIds.indexOf(draggedNodeId)
    if (draggedNodeIndex < 0) return null

    // 找到拖拽节点对应的 LazyList item
    val draggedGlobalIndex = draggedNodeIndex + firstNodeItemIndex
    val draggedItemInfo = visibleItems.firstOrNull { it.index == draggedGlobalIndex }
        ?: return null

    // 计算拖拽节点当前中心 Y 坐标（原位 + 偏移）
    val draggedCenterY = draggedItemInfo.offset + draggedItemInfo.size / 2f + dragOffsetY

    // 收集可见节点 item 的信息（排除 header 和被拖拽节点本身）
    val visibleNodeItems = visibleItems
        .filter { it.index >= firstNodeItemIndex }
        .mapNotNull { itemInfo ->
            val nodeIndex = itemInfo.index - firstNodeItemIndex
            val nodeId = nodeIds.getOrNull(nodeIndex) ?: return@mapNotNull null
            if (nodeId == draggedNodeId) return@mapNotNull null
            DragVisibleNode(nodeIndex, nodeId, itemInfo.offset, itemInfo.size)
        }

    if (visibleNodeItems.isEmpty()) return null

    // 找到拖拽中心 Y 落在哪个节点的范围内
    for (node in visibleNodeItems) {
        val nodeTop = node.offset.toFloat()
        val nodeBottom = nodeTop + node.size.toFloat()

        if (draggedCenterY in nodeTop..nodeBottom) {
            val nodeCenterY = nodeTop + node.size / 2f
            val isBefore = draggedCenterY < nodeCenterY

            // 计算目标插入位置：移除被拖拽节点后的目标 index
            val targetIndex = if (isBefore) {
                node.nodeIndex
            } else {
                node.nodeIndex + 1
            }
            // 如果被拖拽节点在目标位置之前，需要调整 index
            val adjustedIndex = if (draggedNodeIndex < targetIndex) targetIndex - 1 else targetIndex

            return DropTargetInfo(
                targetIndex = adjustedIndex.coerceIn(0, nodeIds.size - 1),
                isBeforeTarget = isBefore,
            )
        }
    }

    // 如果拖到可见区域之外（上方或下方），取最近的一端
    val firstVisible = visibleNodeItems.minByOrNull { it.nodeIndex } ?: return null
    val lastVisible = visibleNodeItems.maxByOrNull { it.nodeIndex } ?: return null

    return if (draggedCenterY < firstVisible.offset.toFloat()) {
        DropTargetInfo(
            targetIndex = if (draggedNodeIndex < firstVisible.nodeIndex) firstVisible.nodeIndex - 1 else firstVisible.nodeIndex,
            isBeforeTarget = true,
        )
    } else {
        DropTargetInfo(
            targetIndex = if (draggedNodeIndex > lastVisible.nodeIndex) lastVisible.nodeIndex + 1 else lastVisible.nodeIndex,
            isBeforeTarget = false,
        )
    }
}

private data class DragVisibleNode(
    val nodeIndex: Int,
    val nodeId: String,
    val offset: Int,
    val size: Int,
)
