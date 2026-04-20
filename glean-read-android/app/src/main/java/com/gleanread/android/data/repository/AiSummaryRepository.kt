package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus

class AiSummaryRepository(
    private val database: WorkspaceDatabase,
    private val outlineGenerator: OutlineGenerator = LocalOutlineGenerator(),
) {
    private val dao = database.workspaceDao()

    suspend fun generateOutline(selectedExcerptIds: List<String>): OutlineDraft {
        val excerpts = dao.getExcerptsOnce()
            .filter { selectedExcerptIds.contains(it.id) }
            .sortedByDescending { it.createTime }
        return outlineGenerator.generate(excerpts.map { it.content })
    }

    suspend fun saveAiSummary(
        selectedExcerptIds: List<String>,
        outlineMarkdown: String,
        targetNodeId: String?,
        newNodeTitle: String?,
        parentNodeId: String?,
    ): String {
        if (selectedExcerptIds.isEmpty()) return ""
        val now = System.currentTimeMillis()
        var resolvedNodeId = targetNodeId.orEmpty()
        database.withTransaction {
            if (resolvedNodeId.isBlank()) {
                val title = newNodeTitle?.trim().takeUnless { it.isNullOrEmpty() } ?: "New Node"
                resolvedNodeId = WorkspaceSeedData.newNodeId()
                dao.insertNode(
                    KnowledgeTreeNodeEntity(
                        id = resolvedNodeId,
                        userId = LOCAL_USER_ID,
                        parentId = parentNodeId,
                        nodeTitle = title,
                        outlineMarkdown = outlineMarkdown,
                        createTime = now,
                        updateTime = now,
                        syncStatus = SyncStatus.PENDING_CREATE.code,
                    ),
                )
            } else {
                dao.findNodeById(resolvedNodeId)?.let { node ->
                    dao.updateNode(
                        node.copy(
                            outlineMarkdown = outlineMarkdown,
                            updateTime = now,
                            syncStatus = SyncStatus.bump(node.syncStatus),
                        ),
                    )
                }
            }

            val excerpts = dao.getExcerptsOnce().filter { selectedExcerptIds.contains(it.id) }
            dao.updateExcerpts(
                excerpts.map { excerpt ->
                    excerpt.copy(
                        treeNodeId = resolvedNodeId,
                        updateTime = now,
                        syncStatus = SyncStatus.bump(excerpt.syncStatus),
                    )
                },
            )
        }
        return resolvedNodeId
    }
}
