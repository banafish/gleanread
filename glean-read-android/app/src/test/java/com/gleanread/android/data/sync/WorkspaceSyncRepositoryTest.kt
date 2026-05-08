package com.gleanread.android.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.auth.AuthSession
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceSyncRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: WorkspaceDatabase
    private lateinit var sessionStore: SupabaseSessionStore
    private lateinit var stateStore: WorkspaceSyncStateStore
    private lateinit var remoteDataSource: FakeWorkspaceRemoteDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        sessionStore = SupabaseSessionStore(context)
        sessionStore.clearSession()
        sessionStore.saveSession(
            AuthSession(
                accessToken = "token",
                refreshToken = null,
                userId = "user-1",
                email = "user@example.com",
                expiresAtMillis = null,
            ),
        )
        stateStore = WorkspaceSyncStateStore(context)
        stateStore.clear()
        stateStore.setCloudSyncEnabled(true)
        remoteDataSource = FakeWorkspaceRemoteDataSource()
    }

    @After
    fun tearDown() {
        database.close()
        sessionStore.clearSession()
        stateStore.clear()
    }

    @Test
    fun `sync marks conflict and applies newer remote record before upload`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-1",
                userId = "user-1",
                content = "本地正文",
                url = null,
                sourceTitle = null,
                userThought = null,
                treeNodeId = null,
                createTime = 100L,
                updateTime = 200L,
                deviceId = "device-local",
                syncStatus = SyncStatus.PENDING_UPDATE,
                lastSyncTime = 100L,
                localDirtyTime = 200L,
            ),
        )
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "excerpt-1",
                    userId = "user-1",
                    content = "远端正文",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 100L,
                    updateTime = 300L,
                    isDeleted = false,
                    deviceId = "device-remote",
                ),
            ),
            excerptTags = emptyList(),
        )

        val result = syncRepository().syncNow(repairMissingRemote = true)
        val saved = database.excerptDao().findExcerptById("excerpt-1")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals("远端正文", saved?.content)
        assertEquals(SyncStatus.CONFLICT, saved?.syncStatus)
        assertEquals(0, remoteDataSource.uploadedExcerpts.size)
    }

    @Test
    fun `sync repairs local synced records that are missing from remote`() = runBlocking {
        database.nodeDao().insertNode(
            KnowledgeTreeNodeEntity(
                id = "node-1",
                userId = "user-1",
                parentId = null,
                nodeTitle = "本地节点",
                outlineMarkdown = null,
                createTime = 100L,
                updateTime = 100L,
                deviceId = "device-local",
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = 100L,
            ),
        )
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-2",
                userId = "user-1",
                content = "本地摘录",
                url = null,
                sourceTitle = null,
                userThought = null,
                treeNodeId = "node-1",
                createTime = 100L,
                updateTime = 100L,
                deviceId = "device-local",
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = 100L,
            ),
        )

        val result = syncRepository().syncNow(repairMissingRemote = true)

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(listOf("node-1"), remoteDataSource.uploadedNodes.map(RemoteKnowledgeTreeNode::id))
        assertEquals(listOf("excerpt-2"), remoteDataSource.uploadedExcerpts.map(RemoteExcerpt::id))
    }

    @Test
    fun `sync does not repair missing remote records by default`() = runBlocking {
        database.nodeDao().insertNode(
            KnowledgeTreeNodeEntity(
                id = "node-default",
                userId = "user-1",
                parentId = null,
                nodeTitle = "默认不补传",
                outlineMarkdown = null,
                createTime = 100L,
                updateTime = 100L,
                deviceId = "device-local",
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = 100L,
            ),
        )

        val result = syncRepository().syncNow()

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(0, remoteDataSource.uploadedNodes.size)
    }

    @Test
    fun `sync uses a separate pull cursor for the active account`() = runBlocking {
        stateStore.saveLastPullTime("other-user", 10_000L)
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "cloud-excerpt",
                    userId = "user-1",
                    content = "云端旧数据",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 1_000L,
                    updateTime = 2_000L,
                    isDeleted = false,
                    deviceId = "device-remote",
                ),
            ),
            excerptTags = emptyList(),
        )

        val result = syncRepository().syncNow()
        val saved = database.excerptDao().findExcerptById("cloud-excerpt")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertNull(remoteDataSource.updatedAfterCalls.single())
        assertEquals("云端旧数据", saved?.content)
    }

    @Test
    fun `sync skips when active workspace does not match session user`() = runBlocking {
        sessionStore.saveSession(
            AuthSession(
                accessToken = "token",
                refreshToken = null,
                userId = "user-2",
                email = "other@example.com",
                expiresAtMillis = null,
            ),
        )
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "wrong-workspace-excerpt",
                    userId = "user-2",
                    content = "不应写入当前库",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 1_000L,
                    updateTime = 2_000L,
                    isDeleted = false,
                    deviceId = "device-remote",
                ),
            ),
            excerptTags = emptyList(),
        )

        val result = syncRepository().syncNow()
        val saved = database.excerptDao().findExcerptById("wrong-workspace-excerpt")

        assertTrue(result is WorkspaceSyncResult.Skipped)
        assertNull(saved)
        assertEquals(0, remoteDataSource.fetchCount)
    }

    @Test
    fun `manual repair sync downloads remote records missing locally outside the pull cursor`() = runBlocking {
        stateStore.saveLastPullTime("user-1", 10_000L)
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "remote-only-excerpt",
                    userId = "user-1",
                    content = "本地漏掉的云端旧数据",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 1_000L,
                    updateTime = 2_000L,
                    isDeleted = false,
                    deviceId = "device-remote",
                ),
            ),
            excerptTags = emptyList(),
        )

        val result = syncRepository().syncNow(repairMissingRemote = true)
        val saved = database.excerptDao().findExcerptById("remote-only-excerpt")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(listOf(10_000L, null), remoteDataSource.updatedAfterCalls)
        assertEquals("本地漏掉的云端旧数据", saved?.content)
        assertEquals(SyncStatus.SYNCED, saved?.syncStatus)
    }

    @Test
    fun `incremental sync does not repair remote records outside the pull cursor by default`() = runBlocking {
        stateStore.saveLastPullTime("user-1", 10_000L)
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "remote-only-default-sync",
                    userId = "user-1",
                    content = "默认增量不补的旧数据",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 1_000L,
                    updateTime = 2_000L,
                    isDeleted = false,
                    deviceId = "device-remote",
                ),
            ),
            excerptTags = emptyList(),
        )

        val result = syncRepository().syncNow()
        val saved = database.excerptDao().findExcerptById("remote-only-default-sync")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(listOf(10_000L), remoteDataSource.updatedAfterCalls)
        assertNull(saved)
    }

    @Test
    fun `realtime applies pushed row without fetching the remote snapshot`() = runBlocking {
        val repository = syncRepository()
        val remoteExcerpt = RemoteExcerpt(
            id = "excerpt-realtime",
            userId = "user-1",
            content = "实时正文",
            url = null,
            sourceTitle = null,
            userThought = "实时想法",
            treeNodeId = null,
            createTime = 100L,
            updateTime = 200L,
            isDeleted = false,
            deviceId = "device-remote",
        )

        repository.applyRealtimeChange(
            userId = "user-1",
            tableName = REMOTE_TABLE_EXCERPTS,
            record = Json.encodeToJsonElement(remoteExcerpt).jsonObject,
        )

        val saved = database.excerptDao().findExcerptById("excerpt-realtime")
        assertEquals("实时正文", saved?.content)
        assertEquals("实时想法", saved?.userThought)
        assertEquals(SyncStatus.SYNCED, saved?.syncStatus)
        assertEquals(0, remoteDataSource.fetchCount)
    }

    @Test
    fun `realtime ignores stale subscription user`() = runBlocking {
        val repository = syncRepository()
        val remoteExcerpt = RemoteExcerpt(
            id = "stale-realtime-excerpt",
            userId = "other-user",
            content = "旧订阅事件",
            url = null,
            sourceTitle = null,
            userThought = null,
            treeNodeId = null,
            createTime = 100L,
            updateTime = 200L,
            isDeleted = false,
            deviceId = "device-remote",
        )

        repository.applyRealtimeChange(
            userId = "other-user",
            tableName = REMOTE_TABLE_EXCERPTS,
            record = Json.encodeToJsonElement(remoteExcerpt).jsonObject,
        )

        assertNull(database.excerptDao().findExcerptById("stale-realtime-excerpt"))
    }

    @Test
    fun `sync reports failure when an upload batch fails`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-failed",
                userId = "user-1",
                content = "上传会失败",
                url = null,
                sourceTitle = null,
                userThought = null,
                treeNodeId = null,
                createTime = 100L,
                updateTime = 200L,
                deviceId = "device-local",
                syncStatus = SyncStatus.PENDING_UPDATE,
                localDirtyTime = 200L,
            ),
        )
        remoteDataSource.excerptUploadError = IllegalStateException("missing content column")

        val result = syncRepository().syncNow()
        val saved = database.excerptDao().findExcerptById("excerpt-failed")

        assertTrue(result is WorkspaceSyncResult.Failure)
        assertTrue((result as WorkspaceSyncResult.Failure).message.contains("missing content column"))
        assertEquals(SyncStatus.FAILED, saved?.syncStatus)
    }

    private fun syncRepository(): WorkspaceSyncRepository {
        return WorkspaceSyncRepository(
            database = database,
            remoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            stateStore = stateStore,
        )
    }
}

private class FakeWorkspaceRemoteDataSource : WorkspaceRemoteDataSource {
    var remoteSnapshot = RemoteWorkspaceSnapshot(
        nodes = emptyList(),
        tags = emptyList(),
        excerpts = emptyList(),
        excerptTags = emptyList(),
    )
    val uploadedExcerpts = mutableListOf<RemoteExcerpt>()
    val uploadedNodes = mutableListOf<RemoteKnowledgeTreeNode>()
    val updatedAfterCalls = mutableListOf<Long?>()
    var excerptUploadError: Throwable? = null
    var fetchCount = 0

    override suspend fun upsertNodes(
        accessToken: String,
        nodes: List<RemoteKnowledgeTreeNode>,
    ) {
        uploadedNodes += nodes
    }

    override suspend fun upsertTags(
        accessToken: String,
        tags: List<RemoteTag>,
    ) = Unit

    override suspend fun upsertExcerpts(
        accessToken: String,
        excerpts: List<RemoteExcerpt>,
    ) {
        excerptUploadError?.let { throw it }
        uploadedExcerpts += excerpts
    }

    override suspend fun upsertExcerptTags(
        accessToken: String,
        relations: List<RemoteExcerptTag>,
    ) = Unit

    override suspend fun fetchChanges(
        accessToken: String,
        userId: String,
        updatedAfter: Long?,
    ): RemoteWorkspaceSnapshot {
        fetchCount += 1
        updatedAfterCalls += updatedAfter
        return remoteSnapshot.filterUpdatedAfter(updatedAfter)
    }
}

private fun RemoteWorkspaceSnapshot.filterUpdatedAfter(updatedAfter: Long?): RemoteWorkspaceSnapshot {
    if (updatedAfter == null) return this
    return copy(
        nodes = nodes.filter { it.updateTime > updatedAfter },
        tags = tags.filter { it.updateTime > updatedAfter },
        excerpts = excerpts.filter { it.updateTime > updatedAfter },
        excerptTags = excerptTags.filter { it.updateTime > updatedAfter },
    )
}
