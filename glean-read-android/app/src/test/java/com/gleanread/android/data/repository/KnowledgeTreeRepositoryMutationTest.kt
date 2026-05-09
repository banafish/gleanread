package com.gleanread.android.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.local.WorkspaceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KnowledgeTreeRepositoryMutationTest {
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
    fun `createChildNode and renameNode update local tree`() = runBlocking {
        val rootId = treeRepository.createRootNode("根节点")
        val childId = treeRepository.createChildNode(rootId, "子节点")

        treeRepository.renameNode(childId, "已重命名子节点")

        val savedChild = database.nodeDao().findNodeById(childId)

        assertEquals(rootId, savedChild?.parentId)
        assertEquals("已重命名子节点", savedChild?.nodeTitle)
    }

    @Test
    fun `tree mutations use current account user id`() = runBlocking {
        val accountUserId = "user-account-1"
        val accountTreeRepository = KnowledgeTreeRepository(
            database = database,
            ownerUserId = accountUserId,
        )

        val rootId = accountTreeRepository.createRootNode("账号根节点")
        accountTreeRepository.renameNode(rootId, "账号根节点-重命名")

        val savedRoot = database.nodeDao().findNodeById(rootId)

        assertEquals(accountUserId, savedRoot?.userId)
        assertEquals("账号根节点-重命名", savedRoot?.nodeTitle)
    }

    @Test
    fun `deleteNodeSubtree removes subtree and sends archived excerpts back to inbox`() = runBlocking {
        val rootId = treeRepository.createRootNode("根节点")
        val childId = treeRepository.createChildNode(rootId, "子节点")
        val excerptId = excerptRepository.createExcerpt(
            content = "挂载摘录",
            thought = "",
            url = null,
            sourceTitle = null,
            tagNames = emptySet(),
            archiveNodeId = childId,
        )

        treeRepository.deleteNodeSubtree(rootId)

        val savedRoot = database.nodeDao().findNodeById(rootId)
        val savedChild = database.nodeDao().findNodeById(childId)
        val excerpt = database.excerptDao().findExcerptById(excerptId)

        assertEquals(true, savedRoot?.isDeleted)
        assertEquals(true, savedChild?.isDeleted)
        assertNull(excerpt?.treeNodeId)
    }
}
