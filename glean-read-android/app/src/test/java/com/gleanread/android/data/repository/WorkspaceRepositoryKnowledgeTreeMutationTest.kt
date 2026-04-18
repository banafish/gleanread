package com.gleanread.android.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.local.WorkspaceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceRepositoryKnowledgeTreeMutationTest {
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
    fun `createChildNode and renameNode update local tree`() = runBlocking {
        val rootId = repository.createRootNode("根节点")
        val childId = repository.createChildNode(rootId, "子节点")

        repository.renameNode(childId, "已重命名子节点")

        val savedChild = database.workspaceDao().findNodeById(childId)

        assertEquals(rootId, savedChild?.parentId)
        assertEquals("已重命名子节点", savedChild?.nodeTitle)
    }

    @Test
    fun `deleteNodeSubtree removes subtree and sends archived excerpts back to inbox`() = runBlocking {
        val rootId = repository.createRootNode("根节点")
        val childId = repository.createChildNode(rootId, "子节点")
        val excerptId = repository.saveQuickExcerpt(
            content = "挂载摘录",
            thought = "",
            url = null,
            sourceTitle = null,
            tagNames = emptyList(),
            archiveNodeId = childId,
        )

        repository.deleteNodeSubtree(rootId)

        val nodes = database.workspaceDao().getNodesOnce()
        val excerpt = database.workspaceDao().getExcerptsOnce().first { it.id == excerptId }

        assertTrue(nodes.all { it.isDeleted })
        assertNull(excerpt.treeNodeId)
    }
}
