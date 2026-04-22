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
}
