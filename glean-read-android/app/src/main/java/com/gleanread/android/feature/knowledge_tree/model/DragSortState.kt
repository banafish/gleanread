package com.gleanread.android.feature.knowledge_tree.model

/**
 * 拖拽目标位置信息。
 * @param targetIndex 在同级节点列表中的插入位置
 * @param isBeforeTarget true 表示插入到目标节点前面，false 表示后面
 */
data class DropTargetInfo(
    val targetIndex: Int,
    val isBeforeTarget: Boolean,
)
