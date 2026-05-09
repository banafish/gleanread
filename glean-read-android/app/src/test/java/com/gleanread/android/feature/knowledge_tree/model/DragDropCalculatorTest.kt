package com.gleanread.android.feature.knowledge_tree.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DragDropCalculatorTest {
    private val nodeIds = listOf("a", "b", "c", "d")
    private val visibleItemsWithoutDragged = listOf(
        DragListItemInfo(nodeIndex = 1, nodeId = "b", offset = 0, size = 100),
        DragListItemInfo(nodeIndex = 2, nodeId = "c", offset = 100, size = 100),
        DragListItemInfo(nodeIndex = 3, nodeId = "d", offset = 200, size = 100),
    )

    @Test
    fun `calculateDropTarget uses visible targets when dragged item is offscreen`() {
        val target = calculateDropTarget(
            visibleItems = visibleItemsWithoutDragged,
            draggedNodeId = "a",
            nodeIds = nodeIds,
            referenceY = 260f,
        )

        assertEquals(3, target?.targetIndex)
    }

    @Test
    fun `calculateItemDisplacements uses cached dragged size when dragged item is offscreen`() {
        val displacements = calculateItemDisplacements(
            visibleItems = visibleItemsWithoutDragged,
            draggedNodeId = "a",
            nodeIds = nodeIds,
            draggedItemSize = 100f,
            dragOffsetY = 320f,
            referenceY = 260f,
        )

        assertEquals(
            mapOf("b" to -100f, "c" to -100f, "d" to -100f),
            displacements,
        )
    }
}
