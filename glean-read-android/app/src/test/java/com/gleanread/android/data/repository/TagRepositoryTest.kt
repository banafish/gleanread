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
class TagRepositoryTest {
    private lateinit var database: WorkspaceDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun `tag create and delete trigger local sync`() = runBlocking {
        val syncTrigger = FakeLocalChangeSyncTrigger()
        val repository = TagRepository(
            database = database,
            localChangeSyncTrigger = syncTrigger,
        )

        val tagId = repository.createTag("compose")
        repository.deleteTags(setOf(tagId))

        assertEquals(2, syncTrigger.changeCount)
    }

    @Test
    fun `existing tag create does not trigger local sync`() = runBlocking {
        val syncTrigger = FakeLocalChangeSyncTrigger()
        val repository = TagRepository(
            database = database,
            localChangeSyncTrigger = syncTrigger,
        )

        repository.createTag("compose")
        repository.createTag("compose")

        assertEquals(1, syncTrigger.changeCount)
    }
}
