package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.OutlineDraft
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider
import com.gleanread.android.data.sync.LocalChangeSyncTrigger
import com.gleanread.android.data.sync.NoOpLocalChangeSyncTrigger

class AiSummaryRepository(
    private val databaseManager: WorkspaceDatabaseManager,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
    private val outlineGenerator: OutlineGenerator = LocalOutlineGenerator(),
    private val localChangeSyncTrigger: LocalChangeSyncTrigger = NoOpLocalChangeSyncTrigger,
) {
    suspend fun generateOutline(selectedExcerptIds: List<String>): OutlineDraft {
        val database = databaseManager.activeWorkspace.value.database
        val excerptDao = database.excerptDao()
        val excerpts = excerptDao.getExcerptsOnce()
            .filter { selectedExcerptIds.contains(it.id) }
            .sortedByDescending { it.createTime }
        return outlineGenerator.generate(
            excerpts.map {
                AiExcerptInput(
                    content = it.content,
                    userThought = it.userThought,
                    sourceTitle = it.sourceTitle,
                    url = it.url,
                )
            }
        )
    }

    suspend fun saveAiSummary(
        selectedExcerptIds: List<String>,
        outlineMarkdown: String,
        targetNodeId: String,
    ): String {
        val resolvedNodeId = targetNodeId.takeIf { it.isNotBlank() } ?: return ""
        if (selectedExcerptIds.isEmpty()) return ""
        val workspace = databaseManager.activeWorkspace.value
        val database = workspace.database
        val ownerUserId = workspace.writeUserId
        val excerptDao = database.excerptDao()
        val nodeDao = database.nodeDao()
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        var savedNodeId = ""
        database.withTransaction {
            val node = nodeDao.findNodeById(resolvedNodeId) ?: return@withTransaction
            nodeDao.updateNode(
                node.copy(
                    userId = ownerUserId,
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
                        userId = ownerUserId,
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
        if (savedNodeId.isNotBlank()) {
            localChangeSyncTrigger.onLocalDataChanged()
        }
        return savedNodeId
    }
}
