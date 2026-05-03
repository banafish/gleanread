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
class KnowledgeTreeRepositoryTest {
    private lateinit var database: WorkspaceDatabase
    private lateinit var repository: KnowledgeTreeRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = KnowledgeTreeRepository(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun `moveNode updates the parent when destination is valid`() = runBlocking {
        val rootId = repository.createRootNode("根节点")
        val archiveId = repository.createRootNode("归档")
        val childId = repository.createChildNode(rootId, "子节点")

        repository.moveNode(childId, archiveId)

        val movedChild = database.nodeDao().findNodeById(childId)

        assertEquals(archiveId, movedChild?.parentId)
    }

    @Test
    fun `moveNode ignores descendant destinations to avoid cycles`() = runBlocking {
        val rootId = repository.createRootNode("根节点")
        val childId = repository.createChildNode(rootId, "子节点")
        val grandchildId = repository.createChildNode(childId, "孙节点")

        repository.moveNode(rootId, grandchildId)

        val savedRoot = database.nodeDao().findNodeById(rootId)

        assertNull(savedRoot?.parentId)
    }

    @Test
    fun `createRootNode assigns incrementing sortOrder`() = runBlocking {
        val root1 = repository.createRootNode("根1")
        val root2 = repository.createRootNode("根2")

        val node1 = database.nodeDao().findNodeById(root1)
        val node2 = database.nodeDao().findNodeById(root2)

        assertEquals(65536L, node1?.sortOrder)
        assertEquals(131072L, node2?.sortOrder)
    }

    @Test
    fun `createChildNode assigns incrementing sortOrder under parent`() = runBlocking {
        val rootId = repository.createRootNode("根")
        val child1 = repository.createChildNode(rootId, "子1")
        val child2 = repository.createChildNode(rootId, "子2")

        val node1 = database.nodeDao().findNodeById(child1)
        val node2 = database.nodeDao().findNodeById(child2)

        assertEquals(65536L, node1?.sortOrder)
        assertEquals(131072L, node2?.sortOrder)
    }

    @Test
    fun `moveNode assigns sortOrder at end of target parent`() = runBlocking {
        val root1 = repository.createRootNode("根1")
        val root2 = repository.createRootNode("根2")
        val child1 = repository.createChildNode(root2, "子1")

        repository.moveNode(child1, root1)

        val movedChild = database.nodeDao().findNodeById(child1)
        val root1Children = database.nodeDao().getSiblingsOnce(root1)

        assertEquals(root1, movedChild?.parentId)
        // 应该追加到 root1 子节点末尾，即最大 sortOrder + GAP
        assertEquals(root1Children.last().id, child1)
    }

    @Test
    fun `moveNodeToPosition inserts node at target index`() = runBlocking {
        val rootId = repository.createRootNode("根")
        val child1 = repository.createChildNode(rootId, "子1")
        val child2 = repository.createChildNode(rootId, "子2")
        val child3 = repository.createChildNode(rootId, "子3")

        // 将 child3 移动到 index 0（第一个位置）
        repository.moveNodeToPosition(child3, 0)

        val siblings = database.nodeDao().getSiblingsOnce(rootId)
        assertEquals(child3, siblings[0].id)
        assertEquals(child1, siblings[1].id)
        assertEquals(child2, siblings[2].id)
    }

    @Test
    fun `moveNodeToPosition between two nodes calculates midpoint sortOrder`() = runBlocking {
        val rootId = repository.createRootNode("根")
        val child1 = repository.createChildNode(rootId, "子1")
        val child2 = repository.createChildNode(rootId, "子2")
        val child3 = repository.createChildNode(rootId, "子3")

        // 将 child3 插入到 child1 和 child2 之间（index 1）
        repository.moveNodeToPosition(child3, 1)

        val siblings = database.nodeDao().getSiblingsOnce(rootId)
        assertEquals(child1, siblings[0].id)
        assertEquals(child3, siblings[1].id)
        assertEquals(child2, siblings[2].id)

        // 验证中间值计算
        val child1Sort = siblings[0].sortOrder
        val child3Sort = siblings[1].sortOrder
        val child2Sort = siblings[2].sortOrder
        assert(child3Sort > child1Sort && child3Sort < child2Sort)
    }

    @Test
    fun `nodes are returned sorted by sortOrder`() = runBlocking {
        val rootId = repository.createRootNode("根")
        val child1 = repository.createChildNode(rootId, "A")
        val child2 = repository.createChildNode(rootId, "B")
        val child3 = repository.createChildNode(rootId, "C")

        // 将 C 移到 A 前面
        repository.moveNodeToPosition(child3, 0)

        val siblings = database.nodeDao().getSiblingsOnce(rootId)
        assertEquals(listOf(child3, child1, child2), siblings.map { it.id })
    }
}
