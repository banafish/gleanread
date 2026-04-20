package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExcerptCaptureRepository(
    private val database: WorkspaceDatabase,
) {
    private val excerptDao = database.excerptDao()
    private val tagDao = database.tagDao()
    private val excerptTagDao = database.excerptTagDao()

    fun observeAvailableTagNames(): Flow<List<String>> {
        return tagDao.observeTags().map { tags -> tags.map(TagEntity::tagName) }
    }

    suspend fun saveQuickExcerpt(
        content: String,
        thought: String,
        url: String?,
        sourceTitle: String?,
        tagNames: List<String>,
        archiveNodeId: String?,
    ): String {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return ""
        val now = System.currentTimeMillis()
        val excerptId = EntityIdGenerator.newDraftExcerptId()
        database.withTransaction {
            val resolvedTags = resolveTags(tagNames, now)
            excerptDao.insertExcerpt(
                ExcerptEntity(
                    id = excerptId,
                    userId = LOCAL_USER_ID,
                    content = trimmedContent,
                    url = url?.trim()?.takeIf { it.isNotEmpty() },
                    sourceTitle = sourceTitle?.trim()?.takeIf { it.isNotEmpty() },
                    userThought = thought.trim().takeIf { it.isNotEmpty() },
                    treeNodeId = archiveNodeId,
                    createTime = now,
                    updateTime = now,
                    syncStatus = SyncStatus.PENDING_CREATE.code,
                ),
            )
            if (resolvedTags.isNotEmpty()) {
                excerptTagDao.insertExcerptTags(
                    resolvedTags.map { tag ->
                        ExcerptTagEntity(
                            id = EntityIdGenerator.newRelationId(),
                            userId = LOCAL_USER_ID,
                            excerptId = excerptId,
                            tagId = tag.id,
                            createTime = now,
                            updateTime = now,
                            syncStatus = SyncStatus.PENDING_CREATE.code,
                        )
                    },
                )
            }
        }
        return excerptId
    }

    private suspend fun resolveTags(tagNames: List<String>, now: Long): List<TagEntity> {
        return tagNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { tagName ->
                val existing = tagDao.findTagByName(LOCAL_USER_ID, tagName)
                if (existing == null) {
                    TagEntity(
                        id = EntityIdGenerator.newTagId(),
                        userId = LOCAL_USER_ID,
                        tagName = tagName,
                        colorIcon = null,
                        heatWeight = 1,
                        createTime = now,
                        updateTime = now,
                        syncStatus = SyncStatus.PENDING_CREATE.code,
                    ).also { tagDao.insertTag(it) }
                } else {
                    existing.copy(
                        heatWeight = existing.heatWeight + 1,
                        updateTime = now,
                        syncStatus = SyncStatus.bump(existing.syncStatus),
                    ).also { tagDao.updateTag(it) }
                }
            }
    }
}
