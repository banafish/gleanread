package com.gleanread.android.feature.knowledge_tree.model

import kotlin.math.abs

data class DragListItemInfo(
    val nodeIndex: Int,
    val nodeId: String,
    val offset: Int,
    val size: Int,
)

/**
 * 拖拽目标位置信息。
 * @param targetIndex 在同级节点列表中的插入位置
 */
data class DropTargetInfo(
    val targetIndex: Int,
)

/**
 * 根据当前拖拽 Y 偏移量和列表状态，计算 drop 目标位置。
 * 使用边界比较算法：向上拖时取被拖拽节点的上边界，向下拖时取下边界，
 * 与每个可见节点的中心 Y 比较，找到插入位置。
 * 相比中点比较，边界比较对大卡片更友好，无需拖过半个卡片高度即可触发让位。
 * 仅用于同级排序，不支持跨层级移动。
 */
fun calculateDropTarget(
    visibleItems: List<DragListItemInfo>,
    draggedNodeId: String?,
    nodeIds: List<String>,
    referenceY: Float?,
): DropTargetInfo? {
    if (draggedNodeId == null || nodeIds.isEmpty()) return null
    if (referenceY == null) return null
    if (visibleItems.isEmpty()) return null

    val draggedNodeIndex = nodeIds.indexOf(draggedNodeId)
    if (draggedNodeIndex < 0) return null

    // 收集可见节点（排除被拖拽节点），按位置排序
    val sortedVisibleNodes = visibleItems
        .filter { it.nodeId != draggedNodeId }
        .sortedBy { it.offset }

    if (sortedVisibleNodes.isEmpty()) return null

    // 边界比较：找到参考边应该插入的位置
    for (node in sortedVisibleNodes) {
        val nodeCenterY = node.offset + node.size / 2f
        if (referenceY < nodeCenterY) {
            // 参考边在此节点中心上方，插入到此节点前面
            val targetIndex = node.nodeIndex
            val adjustedIndex = if (draggedNodeIndex < targetIndex) targetIndex - 1 else targetIndex
            return DropTargetInfo(
                targetIndex = adjustedIndex.coerceIn(0, nodeIds.size - 1),
            )
        }
    }

    // 参考边在所有可见节点中心下方，插入到末尾
    val lastNode = sortedVisibleNodes.last()
    val targetIndex = lastNode.nodeIndex + 1
    val adjustedIndex = if (draggedNodeIndex < targetIndex) targetIndex - 1 else targetIndex
    return DropTargetInfo(
        targetIndex = adjustedIndex.coerceIn(0, nodeIds.size - 1),
    )
}

/**
 * 计算拖拽时每个非拖拽节点的视觉位移量。
 * 只有位于被拖拽节点原位与目标位之间的节点才需要位移，
 * 其他节点保持不动。当目标位置与原位相同时，不产生任何位移。
 */
fun calculateItemDisplacements(
    visibleItems: List<DragListItemInfo>,
    draggedNodeId: String?,
    nodeIds: List<String>,
    draggedItemSize: Float?,
    dragOffsetY: Float,
    referenceY: Float?,
): Map<String, Float> {
    if (draggedNodeId == null || nodeIds.isEmpty()) return emptyMap()
    if (draggedItemSize == null) return emptyMap()
    if (visibleItems.isEmpty()) return emptyMap()

    val draggedNodeIndex = nodeIds.indexOf(draggedNodeId)
    if (draggedNodeIndex < 0) return emptyMap()

    // 微小偏移时（不足卡片高度 1/4）不产生位移，避免刚长按时其他节点跳动
    if (abs(dragOffsetY) < draggedItemSize / 4f) return emptyMap()

    val dropTarget = calculateDropTarget(
        visibleItems = visibleItems,
        draggedNodeId = draggedNodeId,
        nodeIds = nodeIds,
        referenceY = referenceY,
    ) ?: return emptyMap()

    val targetIndex = dropTarget.targetIndex

    // 目标位置与原位相同，不需要位移
    if (targetIndex == draggedNodeIndex) return emptyMap()

    val displacements = mutableMapOf<String, Float>()

    for (itemInfo in visibleItems) {
        val nodeIndex = itemInfo.nodeIndex
        val nodeId = itemInfo.nodeId
        if (nodeId == draggedNodeId) continue

        // 此节点在"排除被拖拽节点"后的列表中的索引
        val adjustedIndex = if (nodeIndex > draggedNodeIndex) nodeIndex - 1 else nodeIndex

        if (draggedNodeIndex > targetIndex) {
            // 向上拖：原位与目标之间的节点向下移
            if (adjustedIndex in targetIndex until draggedNodeIndex) {
                displacements[nodeId] = draggedItemSize
            }
        } else if (draggedNodeIndex < targetIndex) {
            // 向下拖：原位与目标之间的节点向上移
            if (adjustedIndex in draggedNodeIndex until targetIndex) {
                displacements[nodeId] = -draggedItemSize
            }
        }
    }

    return displacements
}
