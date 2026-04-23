package com.gleanread.android.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus
import com.gleanread.android.data.repository.SnapshotRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSnapshotStoreSuggestedTagsTest {
    private lateinit var database: WorkspaceDatabase
    private lateinit var store: AppSnapshotStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkspaceDatabase::class.java,
        ).allowMainThreadQueries().build()
        store = AppSnapshotStore(SnapshotRepository(database))
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun `snapshot suggested tags keep hierarchical path`() = runBlocking {
        val now = 1L
        database.tagDao().insertTag(
            TagEntity(
                id = "tag-1",
                userId = LOCAL_USER_ID,
                tagName = "AI/大模型",
                colorIcon = null,
                heatWeight = 3,
                createTime = now,
                updateTime = now,
                syncStatus = SyncStatus.SYNCED.code,
            ),
        )

        val snapshot = store.snapshot.first()

        assertEquals(listOf("AI/大模型"), snapshot.suggestedTags.map { it.fullName })
        assertEquals(listOf("#大模型"), snapshot.suggestedTags.map { it.label })
    }
}
