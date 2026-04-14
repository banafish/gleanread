package com.gleanread.android.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.local.WorkspaceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceRepositoryQuickCaptureTest {
    private lateinit var database: WorkspaceDatabase
    private lateinit var repository: WorkspaceRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = WorkspaceRepository(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun `saveQuickExcerpt persists source title and url`() = runBlocking {
        val excerptId = repository.saveQuickExcerpt(
            content = "摘录正文",
            thought = "用户想法",
            url = "https://mp.weixin.qq.com/s/test",
            sourceTitle = "公众号标题",
            tagNames = emptyList(),
            archiveNodeId = null,
        )

        val saved = database.workspaceDao().getExcerptsOnce().first { it.id == excerptId }

        assertEquals("https://mp.weixin.qq.com/s/test", saved.url)
        assertEquals("公众号标题", saved.sourceTitle)
    }
}
