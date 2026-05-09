package com.gleanread.android.data.repository

import com.gleanread.android.data.local.ActiveWorkspace
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Tag 的 CRUD 仓库。
 */
class TagRepository internal constructor(
    private val activeWorkspaceProvider: () -> ActiveWorkspace,
    private val activeWorkspaceFlow: Flow<ActiveWorkspace>,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
) {
    constructor(
        databaseManager: WorkspaceDatabaseManager,
        deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
    ) : this(
        activeWorkspaceProvider = { databaseManager.activeWorkspace.value },
        activeWorkspaceFlow = databaseManager.activeWorkspace,
        deviceIdProvider = deviceIdProvider,
    )

    internal constructor(
        database: WorkspaceDatabase,
        deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
        ownerUserId: String = LOCAL_USER_ID,
    ) : this(
        activeWorkspaceProvider = { singleDatabaseWorkspace(database, ownerUserId) },
        activeWorkspaceFlow = flowOf(singleDatabaseWorkspace(database, ownerUserId)),
        deviceIdProvider = deviceIdProvider,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAvailableTagNames(): Flow<List<String>> {
        return activeWorkspaceFlow.flatMapLatest { workspace ->
            workspace.database.tagDao().observeTags().map { tags -> tags.map(TagEntity::tagName) }
        }
    }

    suspend fun createTag(rawTagName: String): String {
        val normalizedTagName = ExcerptRepository.normalizeTagName(rawTagName)
        if (normalizedTagName.isEmpty()) return ""

        val workspace = activeWorkspaceProvider()
        val database = workspace.database
        val ownerUserId = workspace.writeUserId
        val tagDao = database.tagDao()
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val existing = tagDao.findTagByName(ownerUserId, normalizedTagName)

        return if (existing == null) {
            val tagId = EntityIdGenerator.newTagId()
            tagDao.insertTag(
                TagEntity(
                    id = tagId,
                    userId = ownerUserId,
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
                        userId = ownerUserId,
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

        val workspace = activeWorkspaceProvider()
        val database = workspace.database
        val ownerUserId = workspace.writeUserId
        val tagDao = database.tagDao()
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val tagsToDelete = tagDao.getTagsOnce().filter { tagIds.contains(it.id) }
        if (tagsToDelete.isEmpty()) return
        tagDao.updateTags(
            tagsToDelete.map { tag ->
                tag.copy(
                    userId = ownerUserId,
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
