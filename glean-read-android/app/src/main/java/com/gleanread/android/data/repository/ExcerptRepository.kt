package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider

/**
 * 统一的摘录 CRUD 仓库。
 *
 * 合并了原 SnapshotRepository 和 ExcerptCaptureRepository 中摘录相关的逻辑，
 * 消除重复的创建/更新/删除实现。
 */
class ExcerptRepository private constructor(
    private val databaseProvider: () -> WorkspaceDatabase,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
    private val currentUserIdProvider: CurrentUserIdProvider = LocalCurrentUserIdProvider,
) {
    constructor(
        databaseManager: WorkspaceDatabaseManager,
        deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
        currentUserIdProvider: CurrentUserIdProvider = LocalCurrentUserIdProvider,
    ) : this(
        databaseProvider = { databaseManager.activeWorkspace.value.database },
        deviceIdProvider = deviceIdProvider,
        currentUserIdProvider = currentUserIdProvider,
    )

    internal constructor(
        database: WorkspaceDatabase,
        deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
        currentUserIdProvider: CurrentUserIdProvider = LocalCurrentUserIdProvider,
    ) : this(
        databaseProvider = { database },
        deviceIdProvider = deviceIdProvider,
        currentUserIdProvider = currentUserIdProvider,
    )

    private val database get() = databaseProvider()
    private val excerptDao get() = database.excerptDao()
    private val tagDao get() = database.tagDao()
    private val excerptTagDao get() = database.excerptTagDao()
    private val nodeDao get() = database.nodeDao()

    /**
     * 创建摘录并关联 tag。
     *
     * @param autoCreateTags 是否自动创建不存在的 tag（快速捕获场景）。
     *                       为 false 时只使用已存在的 tag（编辑器场景）。
     */
    suspend fun createExcerpt(
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
        autoCreateTags: Boolean = false,
    ): String {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return ""

        val excerptId = EntityIdGenerator.newDraftExcerptId()
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        database.withTransaction {
            val resolvedArchiveNodeId = archiveNodeId?.takeIf { nodeDao.findNodeById(it) != null }
            val targetTags = resolveTags(tagNames, now, autoCreateTags, ownerUserId)

            excerptDao.insertExcerpt(
                ExcerptEntity(
                    id = excerptId,
                    userId = ownerUserId,
                    content = trimmedContent,
                    userThought = thought.trim().takeIf { it.isNotEmpty() },
                    sourceTitle = sourceTitle?.trim()?.takeIf { it.isNotEmpty() },
                    url = url?.trim()?.takeIf { it.isNotEmpty() },
                    treeNodeId = resolvedArchiveNodeId,
                    createTime = now,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.PENDING_CREATE,
                    localDirtyTime = now,
                ),
            )

            if (targetTags.isNotEmpty()) {
                excerptTagDao.insertExcerptTags(
                    targetTags.map { tag ->
                        ExcerptTagEntity(
                            id = EntityIdGenerator.newRelationId(),
                            userId = ownerUserId,
                            excerptId = excerptId,
                            tagId = tag.id,
                            createTime = now,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.PENDING_CREATE,
                            localDirtyTime = now,
                        )
                    },
                )
            }
        }
        return excerptId
    }

    suspend fun updateExcerpt(
        excerptId: String,
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
    ): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return false

        var updated = false
        val ownerUserId = currentUserIdProvider.currentUserId()
        database.withTransaction {
            val excerpt = excerptDao.findExcerptById(excerptId) ?: return@withTransaction
            val resolvedArchiveNodeId = when {
                archiveNodeId == null -> null
                nodeDao.findNodeById(archiveNodeId) != null -> archiveNodeId
                else -> excerpt.treeNodeId
            }
            val normalizedTagNames = tagNames
                .map(::normalizeTagName)
                .filter(String::isNotEmpty)
                .toSet()
            val tagsByName = tagDao.getTagsOnce()
                .filter { it.userId == ownerUserId }
                .associateBy(TagEntity::tagName)
            val targetTags = normalizedTagNames.mapNotNull(tagsByName::get)
            val targetTagIds = targetTags.map(TagEntity::id).toSet()
            val existingRelations = excerptTagDao.getAllExcerptTagsByExcerptId(excerptId)
            val existingRelationsByTagId = existingRelations.associateBy(ExcerptTagEntity::tagId)
            val now = System.currentTimeMillis()
            val deviceId = deviceIdProvider.currentDeviceId()

            excerptDao.updateExcerpts(
                listOf(
                    excerpt.copy(
                        userId = ownerUserId,
                        content = trimmedContent,
                        userThought = thought.trim().takeIf { it.isNotEmpty() },
                        sourceTitle = sourceTitle?.trim()?.takeIf { it.isNotEmpty() },
                        url = url?.trim()?.takeIf { it.isNotEmpty() },
                        treeNodeId = resolvedArchiveNodeId,
                        updateTime = now,
                        deviceId = deviceId,
                        syncStatus = SyncStatus.bump(excerpt.syncStatus),
                        syncError = null,
                        localDirtyTime = now,
                    ),
                ),
            )

            val newRelations = mutableListOf<ExcerptTagEntity>()
            val updatedRelations = mutableListOf<ExcerptTagEntity>()

            targetTagIds.forEach { tagId ->
                val existing = existingRelationsByTagId[tagId]
                when {
                    existing == null -> {
                        newRelations += ExcerptTagEntity(
                            id = EntityIdGenerator.newRelationId(),
                            userId = ownerUserId,
                            excerptId = excerptId,
                            tagId = tagId,
                            createTime = now,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.PENDING_CREATE,
                            localDirtyTime = now,
                        )
                    }

                    existing.isDeleted -> {
                        updatedRelations += existing.copy(
                            userId = ownerUserId,
                            isDeleted = false,
                            updateTime = now,
                            deviceId = deviceId,
                            syncStatus = SyncStatus.bump(existing.syncStatus),
                            syncError = null,
                            localDirtyTime = now,
                        )
                    }
                }
            }

            existingRelations
                .filter { !it.isDeleted && it.tagId !in targetTagIds }
                .forEach { relation ->
                    updatedRelations += relation.copy(
                        userId = ownerUserId,
                        isDeleted = true,
                        updateTime = now,
                        deviceId = deviceId,
                        syncStatus = SyncStatus.markDeleted(relation.syncStatus),
                        syncError = null,
                        localDirtyTime = now,
                    )
                }

            if (newRelations.isNotEmpty()) {
                excerptTagDao.insertExcerptTags(newRelations)
            }
            if (updatedRelations.isNotEmpty()) {
                excerptTagDao.updateExcerptTags(updatedRelations)
            }

            updated = true
        }
        return updated
    }

    suspend fun deleteExcerpt(excerptId: String) {
        val excerpt = excerptDao.findExcerptById(excerptId) ?: return
        val now = System.currentTimeMillis()
        val deviceId = deviceIdProvider.currentDeviceId()
        val ownerUserId = currentUserIdProvider.currentUserId()
        excerptDao.updateExcerpts(
            listOf(
                excerpt.copy(
                    userId = ownerUserId,
                    isDeleted = true,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.markDeleted(excerpt.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                ),
            ),
        )
    }

    /**
     * 解析 tag 名称列表为 TagEntity 列表。
     *
     * @param autoCreate 为 true 时自动创建不存在的 tag 并更新已有 tag 的热度权重。
     */
    private suspend fun resolveTags(
        tagNames: Set<String>,
        now: Long,
        autoCreate: Boolean,
        ownerUserId: String,
    ): List<TagEntity> {
        val normalizedNames = tagNames
            .map(::normalizeTagName)
            .filter(String::isNotEmpty)
            .distinct()

        if (!autoCreate) {
            val tagsByName = tagDao.getTagsOnce()
                .filter { it.userId == ownerUserId }
                .associateBy(TagEntity::tagName)
            return normalizedNames.mapNotNull(tagsByName::get)
        }

        return normalizedNames.map { tagName ->
            val existing = tagDao.findTagByName(ownerUserId, tagName)
            if (existing == null) {
                val deviceId = deviceIdProvider.currentDeviceId()
                TagEntity(
                    id = EntityIdGenerator.newTagId(),
                    userId = ownerUserId,
                    tagName = tagName,
                    colorIcon = null,
                    heatWeight = 1,
                    createTime = now,
                    updateTime = now,
                    deviceId = deviceId,
                    syncStatus = SyncStatus.PENDING_CREATE,
                    localDirtyTime = now,
                ).also { tagDao.insertTag(it) }
            } else {
                existing.copy(
                    userId = ownerUserId,
                    heatWeight = existing.heatWeight + 1,
                    updateTime = now,
                    deviceId = deviceIdProvider.currentDeviceId(),
                    syncStatus = SyncStatus.bump(existing.syncStatus),
                    syncError = null,
                    localDirtyTime = now,
                ).also { tagDao.updateTag(it) }
            }
        }
    }

    companion object {
        fun normalizeTagName(rawTagName: String): String {
            val segments = rawTagName.removePrefix("#").split('/')
                .map(String::trim)
                .filter(String::isNotEmpty)

            return when {
                segments.isEmpty() -> ""
                segments.size == 1 -> segments.first()
                else -> "${segments.first()}/${segments[1]}"
            }
        }
    }
}
