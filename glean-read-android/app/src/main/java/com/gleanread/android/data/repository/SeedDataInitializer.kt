package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager

/**
 * 种子数据初始化器，独立于业务 Repository。
 */
class SeedDataInitializer(
    private val databaseManager: WorkspaceDatabaseManager,
) {
    private val database get() = databaseManager.currentDatabase.value
    suspend fun seedSampleData() {
        val excerptDao = database.excerptDao()
        val nodeDao = database.nodeDao()
        val tagDao = database.tagDao()
        val excerptTagDao = database.excerptTagDao()

        val hasData = excerptDao.countExcerpts() > 0 ||
            nodeDao.countNodes() > 0 ||
            tagDao.countTags() > 0
        if (hasData) return

        val now = System.currentTimeMillis()
        database.withTransaction {
            nodeDao.insertNodes(SampleSeedData.nodes(now))
            tagDao.insertTags(SampleSeedData.tags(now))
            excerptDao.insertExcerpts(SampleSeedData.excerpts(now))
            excerptTagDao.insertExcerptTags(SampleSeedData.excerptTags(now))
        }
    }
}
