package com.gleanread.android.data.repository

import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tag 的 CRUD 仓库。
 */
class TagRepository(
    private val database: WorkspaceDatabase,
) {
    private val tagDao = database.tagDao()

    fun observeAvailableTagNames(): Flow<List<String>> {
        return tagDao.observeTags().map { tags -> tags.map(TagEntity::tagName) }
    }

    suspend fun createTag(rawTagName: String): String {
        val normalizedTagName = ExcerptRepository.normalizeTagName(rawTagName)
        if (normalizedTagName.isEmpty()) return ""

        val now = System.currentTimeMillis()
        val existing = tagDao.findTagByName(LOCAL_USER_ID, normalizedTagName)

        return if (existing == null) {
            val tagId = EntityIdGenerator.newTagId()
            tagDao.insertTag(
                TagEntity(
                    id = tagId,
                    userId = LOCAL_USER_ID,
                    tagName = normalizedTagName,
                    colorIcon = null,
                    heatWeight = 1,
                    createTime = now,
                    updateTime = now,
                    syncStatus = SyncStatus.PENDING_CREATE,
                ),
            )
            tagId
        } else {
            if (existing.isDeleted) {
                tagDao.updateTag(
                    existing.copy(
                        tagName = normalizedTagName,
                        isDeleted = false,
                        heatWeight = existing.heatWeight.coerceAtLeast(1),
                        updateTime = now,
                        syncStatus = SyncStatus.bump(existing.syncStatus),
                    ),
                )
            }
            existing.id
        }
    }

    suspend fun deleteTags(tagIds: Set<String>) {
        if (tagIds.isEmpty()) return

        val now = System.currentTimeMillis()
        val tagsToDelete = tagDao.getTagsOnce().filter { tagIds.contains(it.id) }
        if (tagsToDelete.isEmpty()) return
        tagDao.updateTags(
            tagsToDelete.map { tag ->
                tag.copy(
                    isDeleted = true,
                    updateTime = now,
                    syncStatus = SyncStatus.markDeleted(tag.syncStatus),
                )
            },
        )
    }
}
