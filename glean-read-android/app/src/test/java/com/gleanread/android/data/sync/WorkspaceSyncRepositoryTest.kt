package com.gleanread.android.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.auth.AuthSession
import com.gleanread.android.data.auth.SupabaseSessionRefresher
import com.gleanread.android.data.auth.SupabaseSessionStore
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.remote.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
        assertEquals(2_000L, stateStore.lastPullTimeForUser("user-1"))
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
    fun `sync reports failure instead of throwing when stored refresh token is expired`() = runBlocking {
        sessionStore.saveSession(
            AuthSession(
                accessToken = "expired-token",
                refreshToken = "stale-refresh-token",
                userId = "user-1",
                email = "user@example.com",
                expiresAtMillis = 1_000L,
            ),
        )
        val httpClient = HttpClient(
            MockEngine {
                respond(
                    content = """{"msg":"Invalid Refresh Token: Refresh Token Not Found"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }
        }
        try {
            val refresher = SupabaseSessionRefresher(
                config = SupabaseConfig(
                    url = "https://example.supabase.co",
                    anonKey = "anon-key",
                ),
                httpClient = httpClient,
                sessionStore = sessionStore,
                nowMillis = { 2_000L },
            )

            val result = syncRepository(sessionRefresher = refresher).syncNow()

            assertTrue(result is WorkspaceSyncResult.Failure)
            assertTrue((result as WorkspaceSyncResult.Failure).message.isNotBlank())
            assertNull(sessionStore.session.value)
            assertEquals(0, remoteDataSource.fetchCount)
        } finally {
            httpClient.close()
        }
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

    @Test
    fun `sync preserves cancellation while leaving record retryable`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-cancelled",
                userId = "user-1",
                content = "上传时取消",
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
        remoteDataSource.excerptUploadError = CancellationException("cancelled")

        try {
            syncRepository().syncNow()
            fail("syncNow should preserve coroutine cancellation")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }

        val saved = database.excerptDao().findExcerptById("excerpt-cancelled")
        assertEquals(SyncStatus.SYNCING, saved?.syncStatus)
    }

    @Test
    fun `sync retries rows left in syncing state`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-syncing",
                userId = "user-1",
                content = "上次同步中断",
                url = null,
                sourceTitle = null,
                userThought = null,
                treeNodeId = null,
                createTime = 100L,
                updateTime = 200L,
                deviceId = "device-local",
                syncStatus = SyncStatus.SYNCING,
                localDirtyTime = 200L,
            ),
        )

        val result = syncRepository().syncNow()
        val saved = database.excerptDao().findExcerptById("excerpt-syncing")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(listOf("excerpt-syncing"), remoteDataSource.uploadedExcerpts.map(RemoteExcerpt::id))
        assertEquals(SyncStatus.SYNCED, saved?.syncStatus)
    }

    @Test
    fun `push-only sync uploads pending rows without fetching remote changes`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-push-only",
                userId = "user-1",
                content = "鍙笂琛岀殑鎽樺綍",
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
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "remote-only",
                    userId = "user-1",
                    content = "涓嶅簲涓嬭",
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

        val result = syncRepository().syncNow(pullRemote = false)
        val saved = database.excerptDao().findExcerptById("excerpt-push-only")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(0, remoteDataSource.fetchCount)
        assertNull(database.excerptDao().findExcerptById("remote-only"))
        assertEquals(listOf("excerpt-push-only"), remoteDataSource.uploadedExcerpts.map(RemoteExcerpt::id))
        assertEquals(listOf(1), remoteDataSource.excerptUploadBatchSizes)
        assertEquals(SyncStatus.SYNCED, saved?.syncStatus)
        assertNull(saved?.localDirtyTime)
    }

    @Test
    fun `push-only sync batches pending rows by table`() = runBlocking {
        database.excerptDao().insertExcerpts(
            listOf(
                ExcerptEntity(
                    id = "excerpt-batch-1",
                    userId = "user-1",
                    content = "鎵归噺 1",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 100L,
                    updateTime = 200L,
                    deviceId = "device-local",
                    syncStatus = SyncStatus.PENDING_CREATE,
                    localDirtyTime = 200L,
                ),
                ExcerptEntity(
                    id = "excerpt-batch-2",
                    userId = "user-1",
                    content = "鎵归噺 2",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 100L,
                    updateTime = 201L,
                    deviceId = "device-local",
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    localDirtyTime = 201L,
                ),
            ),
        )

        val result = syncRepository().syncNow(pullRemote = false)

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals(listOf(2), remoteDataSource.excerptUploadBatchSizes)
        assertEquals(listOf("excerpt-batch-1", "excerpt-batch-2"), remoteDataSource.uploadedExcerpts.map(RemoteExcerpt::id))
    }

    @Test
    fun `push conflict applies fetched newer remote row`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-rpc-conflict",
                userId = "user-1",
                content = "local content",
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
                    id = "excerpt-rpc-conflict",
                    userId = "user-1",
                    content = "remote content",
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
        remoteDataSource.excerptPushResults = listOf(
            ConditionalPushResult(
                id = "excerpt-rpc-conflict",
                status = ConditionalPushResult.STATUS_CONFLICT,
                remoteUpdateTime = 300L,
            ),
        )

        val result = syncRepository().syncNow(pullRemote = false)
        val saved = database.excerptDao().findExcerptById("excerpt-rpc-conflict")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals("remote content", saved?.content)
        assertEquals(SyncStatus.SYNCED, saved?.syncStatus)
        assertNull(saved?.localDirtyTime)
    }

    @Test
    fun `push conflict keeps local row conflicted when fetched remote is stale`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-stale-conflict",
                userId = "user-1",
                content = "local wins",
                url = null,
                sourceTitle = null,
                userThought = null,
                treeNodeId = null,
                createTime = 100L,
                updateTime = 300L,
                deviceId = "device-local",
                syncStatus = SyncStatus.PENDING_UPDATE,
                lastSyncTime = 100L,
                localDirtyTime = 300L,
            ),
        )
        remoteDataSource.remoteSnapshot = RemoteWorkspaceSnapshot(
            nodes = emptyList(),
            tags = emptyList(),
            excerpts = listOf(
                RemoteExcerpt(
                    id = "excerpt-stale-conflict",
                    userId = "user-1",
                    content = "remote stale",
                    url = null,
                    sourceTitle = null,
                    userThought = null,
                    treeNodeId = null,
                    createTime = 100L,
                    updateTime = 200L,
                    isDeleted = false,
                    deviceId = "device-remote",
                ),
            ),
            excerptTags = emptyList(),
        )
        remoteDataSource.excerptPushResults = listOf(
            ConditionalPushResult(
                id = "excerpt-stale-conflict",
                status = ConditionalPushResult.STATUS_CONFLICT,
                remoteUpdateTime = 200L,
            ),
        )

        val result = syncRepository().syncNow(pullRemote = false)
        val saved = database.excerptDao().findExcerptById("excerpt-stale-conflict")

        assertTrue(result is WorkspaceSyncResult.Success)
        assertEquals("local wins", saved?.content)
        assertEquals(SyncStatus.CONFLICT, saved?.syncStatus)
        assertTrue(saved?.syncError.orEmpty().contains("Remote version"))
    }

    @Test
    fun `push rpc error result marks local row failed`() = runBlocking {
        database.excerptDao().insertExcerpt(
            ExcerptEntity(
                id = "excerpt-rpc-error",
                userId = "user-1",
                content = "will fail",
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
        remoteDataSource.excerptPushResults = listOf(
            ConditionalPushResult(
                id = "excerpt-rpc-error",
                status = ConditionalPushResult.STATUS_FORBIDDEN,
                error = "not owner",
            ),
        )

        val result = syncRepository().syncNow(pullRemote = false)
        val saved = database.excerptDao().findExcerptById("excerpt-rpc-error")

        assertTrue(result is WorkspaceSyncResult.Failure)
        assertEquals(SyncStatus.FAILED, saved?.syncStatus)
        assertTrue(saved?.syncError.orEmpty().contains("not owner"))
    }

    private fun syncRepository(
        sessionRefresher: SupabaseSessionRefresher? = null,
    ): WorkspaceSyncRepository {
        return WorkspaceSyncRepository(
            database = database,
            remoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            stateStore = stateStore,
            sessionRefresher = sessionRefresher,
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
    val excerptUploadBatchSizes = mutableListOf<Int>()
    val updatedAfterCalls = mutableListOf<Long?>()
    var nodePushResults: List<ConditionalPushResult>? = null
    var tagPushResults: List<ConditionalPushResult>? = null
    var excerptPushResults: List<ConditionalPushResult>? = null
    var excerptTagPushResults: List<ConditionalPushResult>? = null
    var excerptUploadError: Throwable? = null
    var fetchCount = 0

    override suspend fun upsertNodes(
        accessToken: String,
        nodes: List<RemoteKnowledgeTreeNode>,
    ): List<ConditionalPushResult> {
        uploadedNodes += nodes
        return nodePushResults ?: nodes.toAppliedResults(RemoteKnowledgeTreeNode::id)
    }

    override suspend fun upsertTags(
        accessToken: String,
        tags: List<RemoteTag>,
    ): List<ConditionalPushResult> {
        return tagPushResults ?: tags.toAppliedResults(RemoteTag::id)
    }

    override suspend fun upsertExcerpts(
        accessToken: String,
        excerpts: List<RemoteExcerpt>,
    ): List<ConditionalPushResult> {
        excerptUploadError?.let { throw it }
        excerptUploadBatchSizes += excerpts.size
        uploadedExcerpts += excerpts
        return excerptPushResults ?: excerpts.toAppliedResults(RemoteExcerpt::id)
    }

    override suspend fun upsertExcerptTags(
        accessToken: String,
        relations: List<RemoteExcerptTag>,
    ): List<ConditionalPushResult> {
        return excerptTagPushResults ?: relations.toAppliedResults(RemoteExcerptTag::id)
    }

    override suspend fun fetchNode(
        accessToken: String,
        userId: String,
        nodeId: String,
    ): RemoteKnowledgeTreeNode? {
        return remoteSnapshot.nodes.firstOrNull { it.userId == userId && it.id == nodeId }
    }

    override suspend fun fetchTag(
        accessToken: String,
        userId: String,
        tagId: String,
    ): RemoteTag? {
        return remoteSnapshot.tags.firstOrNull { it.userId == userId && it.id == tagId }
    }

    override suspend fun fetchExcerpt(
        accessToken: String,
        userId: String,
        excerptId: String,
    ): RemoteExcerpt? {
        return remoteSnapshot.excerpts.firstOrNull { it.userId == userId && it.id == excerptId }
    }

    override suspend fun fetchExcerptTag(
        accessToken: String,
        userId: String,
        relationId: String,
    ): RemoteExcerptTag? {
        return remoteSnapshot.excerptTags.firstOrNull { it.userId == userId && it.id == relationId }
    }

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

private fun <T> List<T>.toAppliedResults(getId: (T) -> String): List<ConditionalPushResult> {
    return map { ConditionalPushResult(id = getId(it), status = ConditionalPushResult.STATUS_APPLIED) }
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
