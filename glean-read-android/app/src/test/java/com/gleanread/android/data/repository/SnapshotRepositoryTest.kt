package com.gleanread.android.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class SnapshotRepositoryTest {
    private lateinit var database: WorkspaceDatabase
    private lateinit var repository: SnapshotRepository
    private lateinit var treeRepository: KnowledgeTreeRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = SnapshotRepository(database)
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
                syncStatus = SyncStatus.PENDING_CREATE.code,
            ),
        )

        val excerptId = repository.createExcerpt(
            content = "  摘录正文  ",
            thought = "  我的想法  ",
            sourceTitle = "  来源标题  ",
            url = "  https://example.com/article  ",
            tagNames = setOf("compose"),
            archiveNodeId = nodeId,
        )

        val saved = database.excerptDao().findExcerptById(excerptId)
        val relations = database.excerptTagDao().getExcerptTagsByExcerptId(excerptId)

        assertEquals("摘录正文", saved?.content)
        assertEquals("我的想法", saved?.userThought)
        assertEquals("来源标题", saved?.sourceTitle)
        assertEquals("https://example.com/article", saved?.url)
        assertEquals(nodeId, saved?.treeNodeId)
        assertEquals(1, relations.size)
        assertEquals(tagId, relations.single().tagId)
    }
}
