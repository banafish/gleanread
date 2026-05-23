package com.gleanread.android.data.sync

import androidx.room.withTransaction
import com.gleanread.android.data.auth.AuthSession
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.auth.SupabaseSessionRefresher
import com.gleanread.android.data.local.ActiveWorkspace
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class WorkspaceSyncRepository private constructor(
    private val activeWorkspaceProvider: () -> ActiveWorkspace,
    private val remoteDataSource: WorkspaceRemoteDataSource,
    private val sessionStore: SupabaseSessionStore,
    private val stateStore: WorkspaceSyncStateStore,
    private val sessionRefresher: SupabaseSessionRefresher? = null,
) {
    constructor(
        databaseManager: WorkspaceDatabaseManager,
        remoteDataSource: WorkspaceRemoteDataSource,
        sessionStore: SupabaseSessionStore,
        stateStore: WorkspaceSyncStateStore,
        sessionRefresher: SupabaseSessionRefresher? = null,
    ) : this(
        activeWorkspaceProvider = { databaseManager.activeWorkspace.value },
        remoteDataSource = remoteDataSource,
        sessionStore = sessionStore,
        stateStore = stateStore,
        sessionRefresher = sessionRefresher,
    )

    internal constructor(
        database: WorkspaceDatabase,
        remoteDataSource: WorkspaceRemoteDataSource,
        sessionStore: SupabaseSessionStore,
        stateStore: WorkspaceSyncStateStore,
        sessionRefresher: SupabaseSessionRefresher? = null,
    ) : this(
        activeWorkspaceProvider = {
            ActiveWorkspace.user(
                userId = DEFAULT_TEST_USER_ID,
                databaseName = "test.db",
                database = database,
            )
        },
        remoteDataSource = remoteDataSource,
        sessionStore = sessionStore,
        stateStore = stateStore,
        sessionRefresher = sessionRefresher,
    )

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
            val session = try {
                currentSession()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message ?: "登录状态不可用，请重新登录"
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    errorMessage = message,
                )
                return WorkspaceSyncResult.Failure(message)
            }
                ?: return WorkspaceSyncResult.Skipped("未登录，已跳过云端同步")
            if (!stateStore.isCloudSyncEnabled.value) {
                return WorkspaceSyncResult.Skipped("云同步未开启")
            }
            val activeWorkspace = activeWorkspaceProvider()
            if (activeWorkspace.userId != session.userId) {
                return WorkspaceSyncResult.Skipped("当前数据库不属于登录账号")
            }
            val activeDatabase = activeWorkspace.database
            _syncState.value = _syncState.value.copy(isSyncing = true, errorMessage = null)

            return try {
                pullRemoteChanges(activeDatabase, session.accessToken, session.userId)
                val repairSnapshot = if (repairMissingRemote) {
                    repairMissingLocalRecords(
                        database = activeDatabase,
                        accessToken = session.accessToken,
                        userId = session.userId,
                    )
                } else {
                    null
                }
                uploadPendingChanges(
                    database = activeDatabase,
                    accessToken = session.accessToken,
                    userId = session.userId,
                    repairRemoteSnapshot = repairSnapshot,
                )
                val completedAt = System.currentTimeMillis()
                _syncState.value = WorkspaceSyncUiState(
                    isSyncing = false,
                    lastSyncTime = completedAt,
                    errorMessage = null,
                    failedCount = countByStatus(activeDatabase, SyncStatus.FAILED),
                    conflictCount = countByStatus(activeDatabase, SyncStatus.CONFLICT),
                )
                WorkspaceSyncResult.Success(completedAt)
            } catch (error: CancellationException) {
                _syncState.value = _syncState.value.copy(isSyncing = false)
                throw error
            } catch (error: Throwable) {
                val message = error.message ?: "同步失败"
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    errorMessage = message,
                    failedCount = countByStatus(activeDatabase, SyncStatus.FAILED),
                    conflictCount = countByStatus(activeDatabase, SyncStatus.CONFLICT),
                )
                WorkspaceSyncResult.Failure(message)
            }
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun applyRealtimeChange(
        userId: String,
        tableName: String,
        record: JsonObject,
    ) {
        syncMutex.withLock {
            if (!stateStore.isCloudSyncEnabled.value || sessionStore.session.value?.userId != userId) {
                return@withLock
            }
            val activeWorkspace = activeWorkspaceProvider()
            if (activeWorkspace.userId != userId) {
                return@withLock
            }
            val activeDatabase = activeWorkspace.database
            val now = System.currentTimeMillis()
            activeDatabase.withTransaction {
                when (tableName) {
                    REMOTE_TABLE_KNOWLEDGE_TREE_NODE -> applyRemoteNode(activeDatabase, record.decodeRealtimeRecord(), now)
                    REMOTE_TABLE_TAGS -> applyRemoteTag(activeDatabase, record.decodeRealtimeRecord(), now)
                    REMOTE_TABLE_EXCERPTS -> applyRemoteExcerpt(activeDatabase, record.decodeRealtimeRecord(), now)
                    REMOTE_TABLE_EXCERPT_TAGS -> applyRemoteExcerptTag(activeDatabase, record.decodeRealtimeRecord(), now)
                    else -> return@withTransaction
                }
            }
            _syncState.value = _syncState.value.copy(
                lastSyncTime = now,
                errorMessage = null,
            )
        }
    }

    suspend fun awaitIdle() {
        syncMutex.withLock {
            // The lock is held by sync and realtime apply work; acquiring it here waits for both to finish.
        }
    }

    fun clearError() {
        _syncState.value = _syncState.value.copy(errorMessage = null)
    }

    private suspend fun currentSession(): AuthSession? {
        return sessionRefresher?.currentSessionOrRefresh() ?: sessionStore.session.value
    }

    private suspend fun uploadPendingChanges(
        database: WorkspaceDatabase,
        accessToken: String,
        userId: String,
        repairRemoteSnapshot: RemoteWorkspaceSnapshot?,
    ) {
        val failures = mutableListOf<String>()
        val statuses = listOf(
            SyncStatus.PENDING_CREATE.name,
            SyncStatus.PENDING_UPDATE.name,
            SyncStatus.PENDING_DELETE.name,
            SyncStatus.SYNCING.name,
            SyncStatus.FAILED.name,
        )
        uploadNodes(
            database = database,
            accessToken = accessToken,
            nodes = database.nodeDao().findNodesBySyncStatuses(statuses),
        )?.let(failures::add)
        uploadTags(
            database = database,
            accessToken = accessToken,
            tags = database.tagDao().findTagsBySyncStatuses(statuses),
        )?.let(failures::add)
        uploadExcerpts(
            database = database,
            accessToken = accessToken,
            excerpts = database.excerptDao().findExcerptsBySyncStatuses(statuses),
        )?.let(failures::add)
        uploadExcerptTags(
            database = database,
            accessToken = accessToken,
            relations = database.excerptTagDao().findExcerptTagsBySyncStatuses(statuses),
        )
            ?.let(failures::add)
        if (repairRemoteSnapshot != null) {
            uploadSyncedRecordsMissingFromRemote(database, accessToken, userId, repairRemoteSnapshot, failures)
        }
        if (failures.isNotEmpty()) {
            throw WorkspaceSyncUploadException(failures.distinct().joinToString(separator = "；"))
        }
    }

    private suspend fun uploadSyncedRecordsMissingFromRemote(
        database: WorkspaceDatabase,
        accessToken: String,
        userId: String,
        remote: RemoteWorkspaceSnapshot,
        failures: MutableList<String>,
    ) {
        val remoteNodeIds = remote.nodes.mapTo(mutableSetOf(), RemoteKnowledgeTreeNode::id)
        val remoteTagIds = remote.tags.mapTo(mutableSetOf(), RemoteTag::id)
        val remoteExcerptIds = remote.excerpts.mapTo(mutableSetOf(), RemoteExcerpt::id)
        val remoteExcerptTagIds = remote.excerptTags.mapTo(mutableSetOf(), RemoteExcerptTag::id)

        uploadNodes(
            database = database,
            accessToken = accessToken,
            nodes = database.nodeDao().getAllNodesOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteNodeIds) },
        )?.let(failures::add)
        uploadTags(
            database = database,
            accessToken = accessToken,
            tags = database.tagDao().getAllTagsOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteTagIds) },
        )?.let(failures::add)
        uploadExcerpts(
            database = database,
            accessToken = accessToken,
            excerpts = database.excerptDao().getAllExcerptsOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteExcerptIds) },
        )?.let(failures::add)
        uploadExcerptTags(
            database = database,
            accessToken = accessToken,
            relations = database.excerptTagDao().getAllExcerptTagsOnce()
                .filter { it.shouldRepairMissingRemote(userId, remoteExcerptTagIds) },
        )?.let(failures::add)
    }

    private suspend fun uploadNodes(
        database: WorkspaceDatabase,
        accessToken: String,
        nodes: List<KnowledgeTreeNodeEntity>,
    ): String? {
        return uploadRecords(
            records = nodes,
            label = "知识树节点",
            markSyncing = List<KnowledgeTreeNodeEntity>::markNodesSyncing,
            updateLocalRecords = database.nodeDao()::updateNodes,
            uploadRemoteRecords = { remote ->
                remoteDataSource.upsertNodes(accessToken, remote)
            },
            toRemote = KnowledgeTreeNodeEntity::toRemote,
            markSynced = List<KnowledgeTreeNodeEntity>::markNodesSynced,
            markFailed = List<KnowledgeTreeNodeEntity>::markNodesFailed,
        )
    }

    private suspend fun uploadTags(
        database: WorkspaceDatabase,
        accessToken: String,
        tags: List<TagEntity>,
    ): String? {
        return uploadRecords(
            records = tags,
            label = "标签",
            markSyncing = List<TagEntity>::markTagsSyncing,
            updateLocalRecords = database.tagDao()::updateTags,
            uploadRemoteRecords = { remote ->
                remoteDataSource.upsertTags(accessToken, remote)
            },
            toRemote = TagEntity::toRemote,
            markSynced = List<TagEntity>::markTagsSynced,
            markFailed = List<TagEntity>::markTagsFailed,
        )
    }

    private suspend fun uploadExcerpts(
        database: WorkspaceDatabase,
        accessToken: String,
        excerpts: List<ExcerptEntity>,
    ): String? {
        return uploadRecords(
            records = excerpts,
            label = "摘录",
            markSyncing = List<ExcerptEntity>::markExcerptsSyncing,
            updateLocalRecords = database.excerptDao()::updateExcerpts,
            uploadRemoteRecords = { remote ->
                remoteDataSource.upsertExcerpts(accessToken, remote)
            },
            toRemote = ExcerptEntity::toRemote,
            markSynced = List<ExcerptEntity>::markExcerptsSynced,
            markFailed = List<ExcerptEntity>::markExcerptsFailed,
        )
    }

    private suspend fun uploadExcerptTags(
        database: WorkspaceDatabase,
        accessToken: String,
        relations: List<ExcerptTagEntity>,
    ): String? {
        return uploadRecords(
            records = relations,
            label = "摘录标签关系",
            markSyncing = List<ExcerptTagEntity>::markExcerptTagsSyncing,
            updateLocalRecords = database.excerptTagDao()::updateExcerptTags,
            uploadRemoteRecords = { remote ->
                remoteDataSource.upsertExcerptTags(accessToken, remote)
            },
            toRemote = ExcerptTagEntity::toRemote,
            markSynced = List<ExcerptTagEntity>::markExcerptTagsSynced,
            markFailed = List<ExcerptTagEntity>::markExcerptTagsFailed,
        )
    }

    private suspend fun <LocalRecord, RemoteRecord> uploadRecords(
        records: List<LocalRecord>,
        label: String,
        markSyncing: (List<LocalRecord>) -> List<LocalRecord>,
        updateLocalRecords: suspend (List<LocalRecord>) -> Unit,
        uploadRemoteRecords: suspend (List<RemoteRecord>) -> Unit,
        toRemote: (LocalRecord) -> RemoteRecord,
        markSynced: (List<LocalRecord>) -> List<LocalRecord>,
        markFailed: (List<LocalRecord>, Throwable) -> List<LocalRecord>,
    ): String? {
        if (records.isEmpty()) return null
        val syncing = markSyncing(records)
        updateLocalRecords(syncing)
        return try {
            uploadRemoteRecords(syncing.map(toRemote))
            updateLocalRecords(markSynced(syncing))
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            updateLocalRecords(markFailed(syncing, error))
            error.toUploadFailureMessage(label)
        }
    }

    private suspend fun pullRemoteChanges(database: WorkspaceDatabase, accessToken: String, userId: String) {
        val remote = fetchAndApplyRemoteChanges(
            database = database,
            accessToken = accessToken,
            userId = userId,
            updatedAfter = stateStore.lastPullTimeForUser(userId),
        )
        remote.maxUpdateTime()?.let { stateStore.saveLastPullTime(userId, it) }
    }

    private suspend fun repairMissingLocalRecords(
        database: WorkspaceDatabase,
        accessToken: String,
        userId: String,
    ): RemoteWorkspaceSnapshot {
        val now = System.currentTimeMillis()
        val remote = remoteDataSource.fetchChanges(
            accessToken = accessToken,
            userId = userId,
            updatedAfter = null,
        )
        applyRemoteRecordsMissingLocally(database, remote, now)
        remote.maxUpdateTime()?.let { stateStore.saveLastPullTime(userId, it) }
        return remote
    }

    private suspend fun fetchAndApplyRemoteChanges(
        database: WorkspaceDatabase,
        accessToken: String,
        userId: String,
        updatedAfter: Long?,
    ): RemoteWorkspaceSnapshot {
        val now = System.currentTimeMillis()
        val remote = remoteDataSource.fetchChanges(
            accessToken = accessToken,
            userId = userId,
            updatedAfter = updatedAfter,
        )
        database.withTransaction {
            applyRemoteNodes(database, remote.nodes, now)
            applyRemoteTags(database, remote.tags, now)
            applyRemoteExcerpts(database, remote.excerpts, now)
            applyRemoteExcerptTags(database, remote.excerptTags, now)
        }
        return remote
    }

    private suspend fun applyRemoteRecordsMissingLocally(
        database: WorkspaceDatabase,
        remote: RemoteWorkspaceSnapshot,
        now: Long,
    ) {
        database.withTransaction {
            val localNodeIds = database.nodeDao().getAllNodesOnce()
                .mapTo(mutableSetOf(), KnowledgeTreeNodeEntity::id)
            val localTagIds = database.tagDao().getAllTagsOnce()
                .mapTo(mutableSetOf(), TagEntity::id)
            val localExcerptIds = database.excerptDao().getAllExcerptsOnce()
                .mapTo(mutableSetOf(), ExcerptEntity::id)
            val localExcerptTagIds = database.excerptTagDao().getAllExcerptTagsOnce()
                .mapTo(mutableSetOf(), ExcerptTagEntity::id)

            applyRemoteNodes(database, remote.nodes.filter { it.id !in localNodeIds }, now)
            applyRemoteTags(database, remote.tags.filter { it.id !in localTagIds }, now)
            applyRemoteExcerpts(database, remote.excerpts.filter { it.id !in localExcerptIds }, now)
            applyRemoteExcerptTags(database, remote.excerptTags.filter { it.id !in localExcerptTagIds }, now)
        }
    }

    private suspend fun applyRemoteNodes(
        database: WorkspaceDatabase,
        remoteNodes: List<RemoteKnowledgeTreeNode>,
        now: Long,
    ) {
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

    private suspend fun applyRemoteNode(database: WorkspaceDatabase, remote: RemoteKnowledgeTreeNode, now: Long) {
        val local = database.nodeDao().findNodeById(remote.id)
        database.nodeDao().insertNode(
            if (local != null && local.hasConflictWith(remote.updateTime)) {
                resolveNodeConflict(local, remote, now)
            } else {
                remote.toEntity(SyncStatus.SYNCED, now)
            },
        )
    }

    private suspend fun applyRemoteTags(database: WorkspaceDatabase, remoteTags: List<RemoteTag>, now: Long) {
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

    private suspend fun applyRemoteTag(database: WorkspaceDatabase, remote: RemoteTag, now: Long) {
        val local = database.tagDao().findTagById(remote.id)
        database.tagDao().insertTag(
            if (local != null && local.hasConflictWith(remote.updateTime)) {
                resolveTagConflict(local, remote, now)
            } else {
                remote.toEntity(SyncStatus.SYNCED, now)
            },
        )
    }

    private suspend fun applyRemoteExcerpts(
        database: WorkspaceDatabase,
        remoteExcerpts: List<RemoteExcerpt>,
        now: Long,
    ) {
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

    private suspend fun applyRemoteExcerpt(database: WorkspaceDatabase, remote: RemoteExcerpt, now: Long) {
        val local = database.excerptDao().findExcerptById(remote.id)
        database.excerptDao().insertExcerpt(
            if (local != null && local.hasConflictWith(remote.updateTime)) {
                resolveExcerptConflict(local, remote, now)
            } else {
                remote.toEntity(SyncStatus.SYNCED, now)
            },
        )
    }

    private suspend fun applyRemoteExcerptTags(
        database: WorkspaceDatabase,
        remoteRelations: List<RemoteExcerptTag>,
        now: Long,
    ) {
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

    private suspend fun applyRemoteExcerptTag(database: WorkspaceDatabase, remote: RemoteExcerptTag, now: Long) {
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

    private suspend fun countByStatus(database: WorkspaceDatabase, status: SyncStatus): Int {
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

private const val DEFAULT_TEST_USER_ID = "user-1"

private val RealtimeRecordJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private inline fun <reified T> JsonObject.decodeRealtimeRecord(): T {
    return RealtimeRecordJson.decodeFromJsonElement(this)
}

private fun RemoteWorkspaceSnapshot.maxUpdateTime(): Long? {
    return listOfNotNull(
        nodes.maxOfOrNull(RemoteKnowledgeTreeNode::updateTime),
        tags.maxOfOrNull(RemoteTag::updateTime),
        excerpts.maxOfOrNull(RemoteExcerpt::updateTime),
        excerptTags.maxOfOrNull(RemoteExcerptTag::updateTime),
    ).maxOrNull()
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
