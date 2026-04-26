package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.OutlineDraft
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider

class AiSummaryRepository(
    private val database: WorkspaceDatabase,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
    private val outlineGenerator: OutlineGenerator = LocalOutlineGenerator(),
) {
    private val excerptDao = database.excerptDao()
    private val nodeDao = database.nodeDao()

    suspend fun generateOutline(selectedExcerptIds: List<String>): OutlineDraft {
        val excerpts = excerptDao.getExcerptsOnce()
            .filter { selectedExcerptIds.contains(it.id) }
            .sortedByDescending { it.createTime }
        return outlineGenerator.generate(excerpts.map { it.content })
    }

    suspend fun saveAiSummary(
        selectedExcerptIds: List<String>,
        outlineMarkdown: String,
        targetNodeId: String,
    ): String {
        val resolvedNodeId = targetNodeId.takeIf { it.isNotBlank() } ?: return ""
        if (selectedExcerptIds.isEmpty()) return ""
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        var savedNodeId = ""
        database.withTransaction {
            val node = nodeDao.findNodeById(resolvedNodeId) ?: return@withTransaction
            nodeDao.updateNode(
                node.copy(
                    outlineMarkdown = outlineMarkdown,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.bump(node.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                )
            )

            val excerpts = excerptDao.getExcerptsOnce().filter { selectedExcerptIds.contains(it.id) }
            excerptDao.updateExcerpts(
                excerpts.map { excerpt ->
                    excerpt.copy(
                        treeNodeId = resolvedNodeId,
                        updateTime = now,
                        deviceId = deviceId,
                        syncStatus = SyncStatus.bump(excerpt.syncStatus),
                        syncError = null,
                        localDirtyTime = now,
                    )
                },
            )
            savedNodeId = resolvedNodeId
        }
        return savedNodeId
    }
}
