package com.gleanread.android.data.repository

import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tag 的 CRUD 仓库。
 */
class TagRepository(
    private val database: WorkspaceDatabase,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
) {
    private val tagDao = database.tagDao()

    fun observeAvailableTagNames(): Flow<List<String>> {
        return tagDao.observeTags().map { tags -> tags.map(TagEntity::tagName) }
    }

    suspend fun createTag(rawTagName: String): String {
        val normalizedTagName = ExcerptRepository.normalizeTagName(rawTagName)
        if (normalizedTagName.isEmpty()) return ""

        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
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
                    deviceId = deviceId,
                    syncStatus = SyncStatus.PENDING_CREATE,
                    localDirtyTime = now,
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
                        deviceId = deviceId,
                        syncStatus = SyncStatus.bump(existing.syncStatus),
                        syncError = null,
                        localDirtyTime = now,
                    ),
                )
            }
            existing.id
        }
    }

    suspend fun deleteTags(tagIds: Set<String>) {
        if (tagIds.isEmpty()) return

        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val tagsToDelete = tagDao.getTagsOnce().filter { tagIds.contains(it.id) }
        if (tagsToDelete.isEmpty()) return
        tagDao.updateTags(
            tagsToDelete.map { tag ->
                tag.copy(
                    isDeleted = true,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.markDeleted(tag.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                )
            },
        )
    }
}
