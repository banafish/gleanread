package com.gleanread.android.data.repository

import com.gleanread.android.data.model.SyncStatus
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleSeedDataTest {
    @Test
    fun `sample data uses generated ids and current ownership`() {
        val sampleData = SampleSeedData.create(
            now = 100_000L,
            userId = "user-1",
            deviceId = "device-1",
        )
        val allIds = sampleData.nodes.map { it.id } +
            sampleData.tags.map { it.id } +
            sampleData.excerpts.map { it.id } +
            sampleData.excerptTags.map { it.id }
        val nodeIds = sampleData.nodes.map { it.id }.toSet()
        val excerptIds = sampleData.excerpts.map { it.id }.toSet()
        val tagIds = sampleData.tags.map { it.id }.toSet()

        assertEquals(allIds.size, allIds.toSet().size)
        allIds.forEach { UUID.fromString(it) }
        assertTrue(sampleData.nodes.all { it.userId == "user-1" && it.deviceId == "device-1" })
        assertTrue(sampleData.tags.all { it.userId == "user-1" && it.deviceId == "device-1" })
        assertTrue(sampleData.excerpts.all { it.userId == "user-1" && it.deviceId == "device-1" })
        assertTrue(sampleData.excerptTags.all { it.userId == "user-1" && it.deviceId == "device-1" })
        assertTrue(sampleData.nodes.all { it.syncStatus == SyncStatus.PENDING_CREATE && it.localDirtyTime != null })
        assertTrue(sampleData.tags.all { it.syncStatus == SyncStatus.PENDING_CREATE && it.localDirtyTime != null })
        assertTrue(sampleData.excerpts.all { it.syncStatus == SyncStatus.PENDING_CREATE && it.localDirtyTime != null })
        assertTrue(sampleData.excerptTags.all { it.syncStatus == SyncStatus.PENDING_CREATE && it.localDirtyTime != null })
        assertTrue(sampleData.nodes.mapNotNull { it.parentId }.all { it in nodeIds })
        assertTrue(sampleData.excerpts.mapNotNull { it.treeNodeId }.all { it in nodeIds })
        assertTrue(sampleData.excerptTags.all { it.excerptId in excerptIds && it.tagId in tagIds })
    }
}
