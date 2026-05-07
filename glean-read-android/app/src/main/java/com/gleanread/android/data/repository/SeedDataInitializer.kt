package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.sync.DeviceIdProvider
import com.gleanread.android.data.sync.LocalDeviceIdProvider

/**
 * 种子数据初始化器，独立于业务 Repository。
 */
class SeedDataInitializer(
    private val databaseManager: WorkspaceDatabaseManager,
    private val deviceIdProvider: DeviceIdProvider = LocalDeviceIdProvider,
    private val currentUserIdProvider: CurrentUserIdProvider = LocalCurrentUserIdProvider,
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
        val ownerUserId = currentUserIdProvider.currentUserId()
        val deviceId = deviceIdProvider.currentDeviceId()
        val sampleData = SampleSeedData.create(
            now = now,
            userId = ownerUserId,
            deviceId = deviceId,
        )
        database.withTransaction {
            nodeDao.insertNodes(sampleData.nodes)
            tagDao.insertTags(sampleData.tags)
            excerptDao.insertExcerpts(sampleData.excerpts)
            excerptTagDao.insertExcerptTags(sampleData.excerptTags)
        }
    }
}
