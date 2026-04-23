package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.SyncStatus

class AiSummaryRepository(
    private val database: WorkspaceDatabase,
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
        var savedNodeId = ""
        database.withTransaction {
            val node = nodeDao.findNodeById(resolvedNodeId) ?: return@withTransaction
            nodeDao.updateNode(
                node.copy(
                    outlineMarkdown = outlineMarkdown,
                    updateTime = now,
                    syncStatus = SyncStatus.bump(node.syncStatus),
                )
            )

            val excerpts = excerptDao.getExcerptsOnce().filter { selectedExcerptIds.contains(it.id) }
            excerptDao.updateExcerpts(
                excerpts.map { excerpt ->
                    excerpt.copy(
                        treeNodeId = resolvedNodeId,
                        updateTime = now,
                        syncStatus = SyncStatus.bump(excerpt.syncStatus),
                    )
                },
            )
            savedNodeId = resolvedNodeId
        }
        return savedNodeId
    }
}
