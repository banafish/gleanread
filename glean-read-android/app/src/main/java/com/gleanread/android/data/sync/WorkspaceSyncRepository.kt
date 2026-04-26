package com.gleanread.android.data.sync

import androidx.room.withTransaction
import com.gleanread.android.data.auth.AuthSession
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.auth.SupabaseSessionRefresher
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class WorkspaceSyncRepository(
    private val database: WorkspaceDatabase,
    private val remoteDataSource: WorkspaceRemoteDataSource,
    private val sessionStore: SupabaseSessionStore,
    private val stateStore: WorkspaceSyncStateStore,
    private val sessionRefresher: SupabaseSessionRefresher? = null,
) {
    private val _syncState = MutableStateFlow(WorkspaceSyncUiState())
    private val syncMutex = Mutex()
    val syncState: StateFlow<WorkspaceSyncUiState> = _syncState.asStateFlow()
    val isCloudSyncEnabled: StateFlow<Boolean> = stateStore.isCloudSyncEnabled

    fun setCloudSyncEnabled(enabled: Boolean) {
        stateStore.setCloudSyncEnabled(enabled)
    }

    suspend fun syncNow(repairMissingRemote: Boolean = false): WorkspaceSyncResult {
        if (!syncMutex.tryLock()) {
            return WorkspaceSyncResult.Skipped("同步正在进行")
        }
        try {
            val session = currentSession()
                ?: return WorkspaceSyncResult.Skipped("未登录，已跳过云端同步")
            if (!stateStore.isCloudSyncEnabled.value) {
                return WorkspaceSyncResult.Skipped("云同步未开启")
            }
            _syncState.value = _syncState.value.copy(isSyncing = true, errorMessage = null)

            return runCatching {
                pullRemoteChanges(session.accessToken, session.userId)
                uploadPendingChanges(
                    accessToken = session.accessToken,
                    userId = session.userId,
                    repairMissingRemote = repairMissingRemote,
                )
                val completedAt = System.currentTimeMillis()
                _syncState.value = WorkspaceSyncUiState(
                    isSyncing = false,
                    lastSyncTime = completedAt,
                    errorMessage = null,
                    failedCount = countByStatus(SyncStatus.FAILED),
                    conflictCount = countByStatus(SyncStatus.CONFLICT),
                )
                WorkspaceSyncResult.Success(completedAt)
            }.getOrElse { error ->
                val message = error.message ?: "同步失败"
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    errorMessage = message,
                    failedCount = countByStatus(SyncStatus.FAILED),
                    conflictCount = countByStatus(SyncStatus.CONFLICT),
                )
                WorkspaceSyncResult.Failure(message)
            }
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun applyRealtimeChange(
        tableName: String,
        record: JsonObject,
    ) {
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            database.withTransaction {
                when (tableName) {
                    REMOTE_TABLE_KNOWLEDGE_TREE_NODE -> applyRemoteNode(record.decodeRealtimeRecord(), now)
                    REMOTE_TABLE_TAGS -> applyRemoteTag(record.decodeRealtimeRecord(), now)
                    REMOTE_TABLE_EXCERPTS -> applyRemoteExcerpt(record.decodeRealtimeRecord(), now)
                    REMOTE_TABLE_EXCERPT_TAGS -> applyRemoteExcerptTag(record.decodeRealtimeRecord(), now)
                    else -> return@withTransaction
                }
            }
            _syncState.value = _syncState.value.copy(
                lastSyncTime = now,
                errorMessage = null,
            )
        }
    }

    private suspend fun currentSession(): AuthSession? {
        return sessionRefresher?.currentSessionOrRefresh() ?: sessionStore.session.value
    }

    private suspend fun uploadPendingChanges(
        accessToken: String,
        userId: String,
        repairMissingRemote: Boolean,
    ) {
        val failures = mutableListOf<String>()
        val statuses = listOf(
            SyncStatus.PENDING_CREATE.name,
            SyncStatus.PENDING_UPDATE.name,
            SyncStatus.PENDING_DELETE.name,
            SyncStatus.FAILED.name,
        )
        uploadNodes(accessToken, database.nodeDao().findNodesBySyncStatuses(statuses))?.let(failures::add)
        uploadTags(accessToken, database.tagDao().findTagsBySyncStatuses(statuses))?.let(failures::add)
        uploadExcerpts(accessToken, database.excerptDao().findExcerptsBySyncStatuses(statuses))?.let(failures::add)
        uploadExcerptTags(accessToken, database.excerptTagDao().findExcerptTagsBySyncStatuses(statuses))
            ?.let(failures::add)
        if (repairMissingRemote) {
            uploadSyncedRecordsMissingFromRemote(accessToken, userId, failures)
        }
        if (failures.isNotEmpty()) {
            throw WorkspaceSyncUploadException(failures.distinct().joinToString(separator = "；"))
        }
    }

    private suspend fun uploadSyncedRecordsMissingFromRemote(
        accessToken: String,
        userId: String,
        failures: MutableList<String>,
    ) {
        val remote = remoteDataSource.fetchChanges(
            accessToken = accessToken,
            userId = userId,
            updatedAfter = null,
        )
        val remoteNodeIds = remote.nodes.mapTo(mutableSetOf(), RemoteKnowledgeTreeNode::id)
        val remoteTagIds = remote.tags.mapTo(mutableSetOf(), RemoteTag::id)
        val remoteExcerptIds = remote.excerpts.mapTo(mutableSetOf(), RemoteExcerpt::id)
        val remoteExcerptTagIds = remote.excerptTags.mapTo(mutableSetOf(), RemoteExcerptTag::id)

        uploadNodes(
            accessToken = accessToken,
            nodes = database.nodeDao().getAllNodesOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteNodeIds) },
        )?.let(failures::add)
        uploadTags(
            accessToken = accessToken,
            tags = database.tagDao().getAllTagsOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteTagIds) },
        )?.let(failures::add)
        uploadExcerpts(
            accessToken = accessToken,
            excerpts = database.excerptDao().getAllExcerptsOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteExcerptIds) },
        )?.let(failures::add)
        uploadExcerptTags(
            accessToken = accessToken,
            relations = database.excerptTagDao().getAllExcerptTagsOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteExcerptTagIds) },
        )?.let(failures::add)
    }

    private suspend fun uploadNodes(accessToken: String, nodes: List<KnowledgeTreeNodeEntity>): String? {
        if (nodes.isEmpty()) return null
        val syncing = nodes.markNodesSyncing()
        database.nodeDao().updateNodes(syncing)
        return runCatching {
            remoteDataSource.upsertNodes(accessToken, syncing.map { it.toRemote() })
        }.fold(
            onSuccess = {
                database.nodeDao().updateNodes(syncing.markNodesSynced())
                null
            },
            onFailure = {
                database.nodeDao().updateNodes(syncing.markNodesFailed(it))
                it.toUploadFailureMessage("知识树节点")
            },
        )
    }

    private suspend fun uploadTags(accessToken: String, tags: List<TagEntity>): String? {
        if (tags.isEmpty()) return null
        val syncing = tags.markTagsSyncing()
        database.tagDao().updateTags(syncing)
        return runCatching {
            remoteDataSource.upsertTags(accessToken, syncing.map { it.toRemote() })
        }.fold(
            onSuccess = {
                database.tagDao().updateTags(syncing.markTagsSynced())
                null
            },
            onFailure = {
                database.tagDao().updateTags(syncing.markTagsFailed(it))
                it.toUploadFailureMessage("标签")
            },
        )
    }

    private suspend fun uploadExcerpts(accessToken: String, excerpts: List<ExcerptEntity>): String? {
        if (excerpts.isEmpty()) return null
        val syncing = excerpts.markExcerptsSyncing()
        database.excerptDao().updateExcerpts(syncing)
        return runCatching {
            remoteDataSource.upsertExcerpts(accessToken, syncing.map { it.toRemote() })
        }.fold(
            onSuccess = {
                database.excerptDao().updateExcerpts(syncing.markExcerptsSynced())
                null
            },
            onFailure = {
                database.excerptDao().updateExcerpts(syncing.markExcerptsFailed(it))
                it.toUploadFailureMessage("摘录")
            },
        )
    }

    private suspend fun uploadExcerptTags(accessToken: String, relations: List<ExcerptTagEntity>): String? {
        if (relations.isEmpty()) return null
        val syncing = relations.markExcerptTagsSyncing()
        database.excerptTagDao().updateExcerptTags(syncing)
        return runCatching {
            remoteDataSource.upsertExcerptTags(accessToken, syncing.map { it.toRemote() })
        }.fold(
            onSuccess = {
                database.excerptTagDao().updateExcerptTags(syncing.markExcerptTagsSynced())
                null
            },
            onFailure = {
                database.excerptTagDao().updateExcerptTags(syncing.markExcerptTagsFailed(it))
                it.toUploadFailureMessage("摘录标签关系")
            },
        )
    }

    private suspend fun pullRemoteChanges(accessToken: String, userId: String) {
        val now = System.currentTimeMillis()
        val remote = remoteDataSource.fetchChanges(
            accessToken = accessToken,
            userId = userId,
            updatedAfter = stateStore.lastPullTime.value,
        )
        database.withTransaction {
            applyRemoteNodes(remote.nodes, now)
            applyRemoteTags(remote.tags, now)
            applyRemoteExcerpts(remote.excerpts, now)
            applyRemoteExcerptTags(remote.excerptTags, now)
        }
        stateStore.saveLastPullTime(now)
    }

    private suspend fun applyRemoteNodes(remoteNodes: List<RemoteKnowledgeTreeNode>, now: Long) {
        if (remoteNodes.isEmpty()) return
        val localById = database.nodeDao().getAllNodesOnce().associateBy(KnowledgeTreeNodeEntity::id)
        database.nodeDao().insertNodes(
            remoteNodes.map { remote ->
                val local = localById[remote.id]
                if (local != null && local.hasConflictWith(remote.updateTime)) {
                    resolveNodeConflict(local, remote, now)
                } else {
                    remote.toEntity(SyncStatus.SYNCED, now)
                }
            },
        )
    }

    private suspend fun applyRemoteNode(remote: RemoteKnowledgeTreeNode, now: Long) {
        val local = database.nodeDao().findNodeById(remote.id)
        database.nodeDao().insertNode(
            if (local != null && local.hasConflictWith(remote.updateTime)) {
                resolveNodeConflict(local, remote, now)
            } else {
                remote.toEntity(SyncStatus.SYNCED, now)
            },
        )
    }

    private suspend fun applyRemoteTags(remoteTags: List<RemoteTag>, now: Long) {
        if (remoteTags.isEmpty()) return
        val localById = database.tagDao().getAllTagsOnce().associateBy(TagEntity::id)
        database.tagDao().insertTags(
            remoteTags.map { remote ->
                val local = localById[remote.id]
                if (local != null && local.hasConflictWith(remote.updateTime)) {
                    resolveTagConflict(local, remote, now)
                } else {
                    remote.toEntity(SyncStatus.SYNCED, now)
                }
            },
        )
    }

    private suspend fun applyRemoteTag(remote: RemoteTag, now: Long) {
        val local = database.tagDao().findTagById(remote.id)
        database.tagDao().insertTag(
            if (local != null && local.hasConflictWith(remote.updateTime)) {
                resolveTagConflict(local, remote, now)
            } else {
                remote.toEntity(SyncStatus.SYNCED, now)
            },
        )
    }

    private suspend fun applyRemoteExcerpts(remoteExcerpts: List<RemoteExcerpt>, now: Long) {
        if (remoteExcerpts.isEmpty()) return
        val localById = database.excerptDao().getAllExcerptsOnce().associateBy(ExcerptEntity::id)
        database.excerptDao().insertExcerpts(
            remoteExcerpts.map { remote ->
                val local = localById[remote.id]
                if (local != null && local.hasConflictWith(remote.updateTime)) {
                    resolveExcerptConflict(local, remote, now)
                } else {
                    remote.toEntity(SyncStatus.SYNCED, now)
                }
            },
        )
    }

    private suspend fun applyRemoteExcerpt(remote: RemoteExcerpt, now: Long) {
        val local = database.excerptDao().findExcerptById(remote.id)
        database.excerptDao().insertExcerpt(
            if (local != null && local.hasConflictWith(remote.updateTime)) {
                resolveExcerptConflict(local, remote, now)
            } else {
                remote.toEntity(SyncStatus.SYNCED, now)
            },
        )
    }

    private suspend fun applyRemoteExcerptTags(remoteRelations: List<RemoteExcerptTag>, now: Long) {
        if (remoteRelations.isEmpty()) return
        val localById = database.excerptTagDao().getAllExcerptTagsOnce().associateBy(ExcerptTagEntity::id)
        database.excerptTagDao().insertExcerptTags(
            remoteRelations.map { remote ->
                val local = localById[remote.id]
                if (local != null && local.hasConflictWith(remote.updateTime)) {
                    resolveExcerptTagConflict(local, remote, now)
                } else {
                    remote.toEntity(SyncStatus.SYNCED, now)
                }
            },
        )
    }

    private suspend fun applyRemoteExcerptTag(remote: RemoteExcerptTag, now: Long) {
        val local = database.excerptTagDao().findExcerptTagById(remote.id)
        database.excerptTagDao().insertExcerptTags(
            listOf(
                if (local != null && local.hasConflictWith(remote.updateTime)) {
                    resolveExcerptTagConflict(local, remote, now)
                } else {
                    remote.toEntity(SyncStatus.SYNCED, now)
                },
            ),
        )
    }

    private suspend fun countByStatus(status: SyncStatus): Int {
        return database.nodeDao().findNodesBySyncStatuses(listOf(status.name)).size +
            database.tagDao().findTagsBySyncStatuses(listOf(status.name)).size +
            database.excerptDao().findExcerptsBySyncStatuses(listOf(status.name)).size +
            database.excerptTagDao().findExcerptTagsBySyncStatuses(listOf(status.name)).size
    }

    private fun KnowledgeTreeNodeEntity.hasConflictWith(remoteUpdateTime: Long): Boolean {
        return syncStatus != SyncStatus.SYNCED &&
            localDirtyTime != null &&
            remoteUpdateTime > (lastSyncTime ?: 0L)
    }

    private fun TagEntity.hasConflictWith(remoteUpdateTime: Long): Boolean {
        return syncStatus != SyncStatus.SYNCED &&
            localDirtyTime != null &&
            remoteUpdateTime > (lastSyncTime ?: 0L)
    }

    private fun ExcerptEntity.hasConflictWith(remoteUpdateTime: Long): Boolean {
        return syncStatus != SyncStatus.SYNCED &&
            localDirtyTime != null &&
            remoteUpdateTime > (lastSyncTime ?: 0L)
    }

    private fun ExcerptTagEntity.hasConflictWith(remoteUpdateTime: Long): Boolean {
        return syncStatus != SyncStatus.SYNCED &&
            localDirtyTime != null &&
            remoteUpdateTime > (lastSyncTime ?: 0L)
    }

    private fun KnowledgeTreeNodeEntity.shouldRepairMissingRemote(
        currentUserId: String,
        remoteIds: Set<String>,
    ): Boolean {
        return userId == currentUserId && syncStatus == SyncStatus.SYNCED && id !in remoteIds
    }

    private fun TagEntity.shouldRepairMissingRemote(currentUserId: String, remoteIds: Set<String>): Boolean {
        return userId == currentUserId && syncStatus == SyncStatus.SYNCED && id !in remoteIds
    }

    private fun ExcerptEntity.shouldRepairMissingRemote(currentUserId: String, remoteIds: Set<String>): Boolean {
        return userId == currentUserId && syncStatus == SyncStatus.SYNCED && id !in remoteIds
    }

    private fun ExcerptTagEntity.shouldRepairMissingRemote(currentUserId: String, remoteIds: Set<String>): Boolean {
        return userId == currentUserId && syncStatus == SyncStatus.SYNCED && id !in remoteIds
    }

    private fun resolveNodeConflict(
        local: KnowledgeTreeNodeEntity,
        remote: RemoteKnowledgeTreeNode,
        now: Long,
    ): KnowledgeTreeNodeEntity {
        return if (local.updateTime > remote.updateTime) {
            local.copy(syncStatus = SyncStatus.CONFLICT, lastSyncTime = now)
        } else {
            remote.toEntity(SyncStatus.CONFLICT, now)
        }
    }

    private fun resolveTagConflict(local: TagEntity, remote: RemoteTag, now: Long): TagEntity {
        return if (local.updateTime > remote.updateTime) {
            local.copy(syncStatus = SyncStatus.CONFLICT, lastSyncTime = now)
        } else {
            remote.toEntity(SyncStatus.CONFLICT, now)
        }
    }

    private fun resolveExcerptConflict(local: ExcerptEntity, remote: RemoteExcerpt, now: Long): ExcerptEntity {
        return if (local.updateTime > remote.updateTime) {
            local.copy(syncStatus = SyncStatus.CONFLICT, lastSyncTime = now)
        } else {
            remote.toEntity(SyncStatus.CONFLICT, now)
        }
    }

    private fun resolveExcerptTagConflict(
        local: ExcerptTagEntity,
        remote: RemoteExcerptTag,
        now: Long,
    ): ExcerptTagEntity {
        return if (local.updateTime > remote.updateTime) {
            local.copy(syncStatus = SyncStatus.CONFLICT, lastSyncTime = now)
        } else {
            remote.toEntity(SyncStatus.CONFLICT, now)
        }
    }
}

data class WorkspaceSyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
)

sealed interface WorkspaceSyncResult {
    data class Success(val completedAt: Long) : WorkspaceSyncResult
    data class Failure(val message: String) : WorkspaceSyncResult
    data class Skipped(val message: String) : WorkspaceSyncResult
}

private class WorkspaceSyncUploadException(message: String) : RuntimeException(message)

private val RealtimeRecordJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private inline fun <reified T> JsonObject.decodeRealtimeRecord(): T {
    return RealtimeRecordJson.decodeFromJsonElement(this)
}

private fun Throwable.toUploadFailureMessage(label: String): String {
    return "$label 同步失败：${message ?: "未知错误"}"
}

private fun List<KnowledgeTreeNodeEntity>.markNodesSyncing(): List<KnowledgeTreeNodeEntity> {
    return map { it.copy(syncStatus = SyncStatus.SYNCING, syncError = null) }
}

private fun List<TagEntity>.markTagsSyncing(): List<TagEntity> {
    return map { it.copy(syncStatus = SyncStatus.SYNCING, syncError = null) }
}

private fun List<ExcerptEntity>.markExcerptsSyncing(): List<ExcerptEntity> {
    return map { it.copy(syncStatus = SyncStatus.SYNCING, syncError = null) }
}

private fun List<ExcerptTagEntity>.markExcerptTagsSyncing(): List<ExcerptTagEntity> {
    return map { it.copy(syncStatus = SyncStatus.SYNCING, syncError = null) }
}

private fun List<KnowledgeTreeNodeEntity>.markNodesSynced(): List<KnowledgeTreeNodeEntity> {
    val now = System.currentTimeMillis()
    return map { it.copy(syncStatus = SyncStatus.SYNCED, lastSyncTime = now, syncError = null, retryCount = 0) }
}

private fun List<TagEntity>.markTagsSynced(): List<TagEntity> {
    val now = System.currentTimeMillis()
    return map { it.copy(syncStatus = SyncStatus.SYNCED, lastSyncTime = now, syncError = null, retryCount = 0) }
}

private fun List<ExcerptEntity>.markExcerptsSynced(): List<ExcerptEntity> {
    val now = System.currentTimeMillis()
    return map { it.copy(syncStatus = SyncStatus.SYNCED, lastSyncTime = now, syncError = null, retryCount = 0) }
}

private fun List<ExcerptTagEntity>.markExcerptTagsSynced(): List<ExcerptTagEntity> {
    val now = System.currentTimeMillis()
    return map { it.copy(syncStatus = SyncStatus.SYNCED, lastSyncTime = now, syncError = null, retryCount = 0) }
}

private fun List<KnowledgeTreeNodeEntity>.markNodesFailed(error: Throwable): List<KnowledgeTreeNodeEntity> {
    val message = error.message ?: "同步失败"
    return map { it.copy(syncStatus = SyncStatus.FAILED, syncError = message, retryCount = it.retryCount + 1) }
}

private fun List<TagEntity>.markTagsFailed(error: Throwable): List<TagEntity> {
    val message = error.message ?: "同步失败"
    return map { it.copy(syncStatus = SyncStatus.FAILED, syncError = message, retryCount = it.retryCount + 1) }
}

private fun List<ExcerptEntity>.markExcerptsFailed(error: Throwable): List<ExcerptEntity> {
    val message = error.message ?: "同步失败"
    return map { it.copy(syncStatus = SyncStatus.FAILED, syncError = message, retryCount = it.retryCount + 1) }
}

private fun List<ExcerptTagEntity>.markExcerptTagsFailed(error: Throwable): List<ExcerptTagEntity> {
    val message = error.message ?: "同步失败"
    return map { it.copy(syncStatus = SyncStatus.FAILED, syncError = message, retryCount = it.retryCount + 1) }
}
