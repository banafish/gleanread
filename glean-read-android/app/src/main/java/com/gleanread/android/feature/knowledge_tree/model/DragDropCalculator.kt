package com.gleanread.android.feature.knowledge_tree.model

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

/** 拖拽自动滚动的边缘区域大小（px） */
const val DRAG_AUTO_SCROLL_ZONE = 48f

/** 拖拽自动滚动的最大速度（px/s） */
const val DRAG_AUTO_SCROLL_SPEED = 2400f

/**
 * 根据当前拖拽 Y 偏移量和列表状态，计算 drop 目标位置。
 * 使用中点比较算法：将被拖拽节点的中心 Y 与每个可见节点的中心 Y 比较，
 * 找到插入位置。仅用于同级排序，不支持跨层级移动。
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

    val draggedGlobalIndex = draggedNodeIndex + firstNodeItemIndex
    val draggedItemInfo = visibleItems.firstOrNull { it.index == draggedGlobalIndex }
        ?: return null

    // 被拖拽节点的当前视觉中心 Y（原位 + 偏移）
    val draggedCenterY = draggedItemInfo.offset + draggedItemInfo.size / 2f + dragOffsetY

    // 收集可见节点（排除被拖拽节点），按位置排序
    val sortedVisibleNodes = visibleItems
        .filter { it.index >= firstNodeItemIndex }
        .mapNotNull { itemInfo ->
            val nodeIndex = itemInfo.index - firstNodeItemIndex
            val nodeId = nodeIds.getOrNull(nodeIndex) ?: return@mapNotNull null
            if (nodeId == draggedNodeId) return@mapNotNull null
            DragVisibleNode(nodeIndex, nodeId, itemInfo.offset, itemInfo.size)
        }
        .sortedBy { it.offset }

    if (sortedVisibleNodes.isEmpty()) return null

    // 中点比较：找到被拖拽中心应该插入的位置
    for (node in sortedVisibleNodes) {
        val nodeCenterY = node.offset + node.size / 2f
        if (draggedCenterY < nodeCenterY) {
            // 被拖拽中心在此节点中心上方，插入到此节点前面
            val targetIndex = node.nodeIndex
            val adjustedIndex = if (draggedNodeIndex < targetIndex) targetIndex - 1 else targetIndex
            return DropTargetInfo(
                targetIndex = adjustedIndex.coerceIn(0, nodeIds.size - 1),
                isBeforeTarget = true,
            )
        }
    }

    // 被拖拽中心在所有可见节点中心下方，插入到末尾
    val lastNode = sortedVisibleNodes.last()
    val targetIndex = lastNode.nodeIndex + 1
    val adjustedIndex = if (draggedNodeIndex < targetIndex) targetIndex - 1 else targetIndex
    return DropTargetInfo(
        targetIndex = adjustedIndex.coerceIn(0, nodeIds.size - 1),
        isBeforeTarget = false,
    )
}

/**
 * 计算拖拽时每个非拖拽节点的视觉位移量。
 * 只有位于被拖拽节点原位与目标位之间的节点才需要位移，
 * 其他节点保持不动。当目标位置与原位相同时，不产生任何位移。
 */
fun calculateItemDisplacements(
    listState: LazyListState,
    draggedNodeId: String?,
    nodeIds: List<String>,
    dragOffsetY: Float,
    firstNodeItemIndex: Int = 0,
): Map<String, Float> {
    if (draggedNodeId == null || nodeIds.isEmpty()) return emptyMap()

    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return emptyMap()

    val draggedNodeIndex = nodeIds.indexOf(draggedNodeId)
    if (draggedNodeIndex < 0) return emptyMap()

    val draggedGlobalIndex = draggedNodeIndex + firstNodeItemIndex
    val draggedItemInfo = visibleItems.firstOrNull { it.index == draggedGlobalIndex }
        ?: return emptyMap()

    val draggedHeight = draggedItemInfo.size.toFloat()

    // 微小偏移时（不足卡片高度 1/4）不产生位移，避免刚长按时其他节点跳动
    if (abs(dragOffsetY) < draggedHeight / 4f) return emptyMap()

    val dropTarget = calculateDropTarget(
        listState, draggedNodeId, nodeIds, dragOffsetY, firstNodeItemIndex,
    ) ?: return emptyMap()

    val targetIndex = dropTarget.targetIndex

    // 目标位置与原位相同，不需要位移
    if (targetIndex == draggedNodeIndex) return emptyMap()

    val displacements = mutableMapOf<String, Float>()

    for (itemInfo in visibleItems) {
        if (itemInfo.index < firstNodeItemIndex) continue
        val nodeIndex = itemInfo.index - firstNodeItemIndex
        val nodeId = nodeIds.getOrNull(nodeIndex) ?: continue
        if (nodeId == draggedNodeId) continue

        // 此节点在"排除被拖拽节点"后的列表中的索引
        val adjustedIndex = if (nodeIndex > draggedNodeIndex) nodeIndex - 1 else nodeIndex

        if (draggedNodeIndex > targetIndex) {
            // 向上拖：原位与目标之间的节点向下移
            if (adjustedIndex in targetIndex until draggedNodeIndex) {
                displacements[nodeId] = draggedHeight
            }
        } else if (draggedNodeIndex < targetIndex) {
            // 向下拖：原位与目标之间的节点向上移
            if (adjustedIndex in draggedNodeIndex until targetIndex) {
                displacements[nodeId] = -draggedHeight
            }
        }
    }

    return displacements
}

private data class DragVisibleNode(
    val nodeIndex: Int,
    val nodeId: String,
    val offset: Int,
    val size: Int,
)
