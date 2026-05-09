package com.gleanread.android.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.local.ActiveWorkspace
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExcerptRepositoryTest {
    private lateinit var database: WorkspaceDatabase
    private lateinit var excerptRepository: ExcerptRepository
    private lateinit var treeRepository: KnowledgeTreeRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        excerptRepository = ExcerptRepository(database)
        treeRepository = KnowledgeTreeRepository(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun `createExcerpt persists editor fields and selected tags`() = runBlocking {
        val now = System.currentTimeMillis()
        val tagId = "tag-compose"
        val nodeId = treeRepository.createRootNode("Compose")
        database.tagDao().insertTag(
            TagEntity(
                id = tagId,
                userId = LOCAL_USER_ID,
                tagName = "compose",
                colorIcon = null,
                heatWeight = 1,
                createTime = now,
                updateTime = now,
                syncStatus = SyncStatus.PENDING_CREATE,
            ),
        )

        val excerptId = excerptRepository.createExcerpt(
            content = "  摘录正文  ",
            thought = "  我的想法  ",
            sourceTitle = "  来源标题  ",
            url = "  https://example.com/article  ",
            tagNames = setOf("compose"),
            archiveNodeId = nodeId,
        )

        val saved = database.excerptDao().findExcerptById(excerptId)
        val relations = database.excerptTagDao().getAllExcerptTagsByExcerptId(excerptId)

        assertEquals("摘录正文", saved?.content)
        assertEquals("我的想法", saved?.userThought)
        assertEquals("来源标题", saved?.sourceTitle)
        assertEquals("https://example.com/article", saved?.url)
        assertEquals(nodeId, saved?.treeNodeId)
        assertEquals("local-device", saved?.deviceId)
        assertEquals(SyncStatus.PENDING_CREATE, saved?.syncStatus)
        assertEquals(1, relations.size)
        assertEquals(tagId, relations.single().tagId)
        assertEquals("local-device", relations.single().deviceId)
    }

    @Test
    fun `createExcerpt with autoCreateTags creates new tags`() = runBlocking {
        val excerptId = excerptRepository.createExcerpt(
            content = "摘录正文",
            thought = "用户想法",
            url = "https://mp.weixin.qq.com/s/test",
            sourceTitle = "公众号标题",
            tagNames = setOf("newTag"),
            archiveNodeId = null,
            autoCreateTags = true,
        )

        val saved = database.excerptDao().findExcerptById(excerptId)
        val relations = database.excerptTagDao().getAllExcerptTagsByExcerptId(excerptId)
        val tag = database.tagDao().findTagByName(LOCAL_USER_ID, "newTag")

        assertEquals("https://mp.weixin.qq.com/s/test", saved?.url)
        assertEquals("公众号标题", saved?.sourceTitle)
        assertEquals(1, relations.size)
        assertEquals(tag?.id, relations.single().tagId)
    }

    @Test
    fun `createExcerpt uses current account user id for excerpt tags and relations`() = runBlocking {
        val accountUserId = "user-account-1"
        val accountRepository = ExcerptRepository(
            database = database,
            ownerUserId = accountUserId,
        )

        val excerptId = accountRepository.createExcerpt(
            content = "账号摘录",
            thought = "",
            url = null,
            sourceTitle = null,
            tagNames = setOf("accountTag"),
            archiveNodeId = null,
            autoCreateTags = true,
        )

        val saved = database.excerptDao().findExcerptById(excerptId)
        val tag = database.tagDao().findTagByName(accountUserId, "accountTag")
        val relations = database.excerptTagDao().getAllExcerptTagsByExcerptId(excerptId)

        assertEquals(accountUserId, saved?.userId)
        assertEquals(accountUserId, tag?.userId)
        assertEquals(accountUserId, relations.single().userId)
    }

    @Test
    fun `createExcerpt uses one active workspace snapshot for database and owner`() = runBlocking {
        val secondDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            var calls = 0
            val accountRepository = ExcerptRepository(
                activeWorkspaceProvider = {
                    calls += 1
                    if (calls == 1) {
                        ActiveWorkspace.user("user-a", "a.db", database)
                    } else {
                        ActiveWorkspace.user("user-b", "b.db", secondDatabase)
                    }
                },
            )

            val excerptId = accountRepository.createExcerpt(
                content = "账号 A 摘录",
                thought = "",
                url = null,
                sourceTitle = null,
                tagNames = emptySet(),
                archiveNodeId = null,
            )

            assertEquals("user-a", database.excerptDao().findExcerptById(excerptId)?.userId)
            assertEquals(0, secondDatabase.excerptDao().getAllExcerptsOnce().size)
        } finally {
            secondDatabase.close()
        }
    }
}
