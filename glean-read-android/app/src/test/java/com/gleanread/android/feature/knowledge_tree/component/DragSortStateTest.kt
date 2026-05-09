package com.gleanread.android.feature.knowledge_tree.component

import org.junit.Assert.assertEquals
import org.junit.Test

class DragSortStateTest {
    @Test
    fun `drag hit testing uses positioned item bounds`() {
        val nodeId = findDragNodeIdAt(
            pointerRootY = 124f,
            itemBounds = linkedMapOf(
                "current" to DragItemBounds(top = 88f, height = 96f),
                "next" to DragItemBounds(top = 200f, height = 96f),
            ),
        )

        assertEquals("current", nodeId)
    }

    @Test
    fun `auto scroll keeps maximum upward velocity when pointer moves above viewport`() {
        val velocity = calculateAutoScrollVelocity(
            pointerY = -24f,
            viewportHeight = 800f,
            zonePx = 48f,
            maxSpeedPxPerSecond = 2400f,
        )

        assertEquals(-2400f, velocity, 0.001f)
    }

    @Test
    fun `auto scroll keeps maximum downward velocity when pointer moves below viewport`() {
        val velocity = calculateAutoScrollVelocity(
            pointerY = 824f,
            viewportHeight = 800f,
            zonePx = 48f,
            maxSpeedPxPerSecond = 2400f,
        )

        assertEquals(2400f, velocity, 0.001f)
    }

    @Test
    fun `auto scroll stays idle outside edge zones`() {
        val velocity = calculateAutoScrollVelocity(
            pointerY = 400f,
            viewportHeight = 800f,
            zonePx = 48f,
            maxSpeedPxPerSecond = 2400f,
        )

        assertEquals(0f, velocity, 0.001f)
    }
}
