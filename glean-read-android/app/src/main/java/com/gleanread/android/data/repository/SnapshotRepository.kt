package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.SyncStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class SnapshotRepository(
    private val database: WorkspaceDatabase,
) {
    private val excerptDao = database.excerptDao()
    private val nodeDao = database.nodeDao()
    private val tagDao = database.tagDao()
    private val excerptTagDao = database.excerptTagDao()

    val localSnapshot = combine(
        excerptDao.observeExcerpts(),
        nodeDao.observeNodes(),
        tagDao.observeTags(),
        excerptTagDao.observeExcerptTags(),
    ) { excerpts, nodes, tags, relations ->
        WorkspaceLocalSnapshot(
            excerpts = excerpts,
            nodes = nodes,
            tags = tags,
            relations = relations,
        )
    }.distinctUntilChanged()

    suspend fun seedSampleData() {
        val hasData = excerptDao.countExcerpts() > 0 || nodeDao.countNodes() > 0 || tagDao.countTags() > 0
        if (hasData) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            nodeDao.insertNodes(SampleSeedData.nodes(now))
            tagDao.insertTags(SampleSeedData.tags(now))
            excerptDao.insertExcerpts(SampleSeedData.excerpts(now))
            excerptTagDao.insertExcerptTags(SampleSeedData.excerptTags(now))
        }
    }

    suspend fun deleteExcerpt(excerptId: String) {
        val excerpt = excerptDao.findExcerptById(excerptId) ?: return
        val now = System.currentTimeMillis()
        excerptDao.updateExcerpts(
            listOf(
                excerpt.copy(
                    isDeleted = true,
                    updateTime = now,
                    syncStatus = SyncStatus.markDeleted(excerpt.syncStatus),
                ),
            ),
        )
    }
}
