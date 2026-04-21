package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID
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

    suspend fun createTag(rawTagName: String): String {
        val normalizedTagName = normalizeTagName(rawTagName)
        if (normalizedTagName.isEmpty()) return ""

        val now = System.currentTimeMillis()
        val existing = tagDao.findTagByName(LOCAL_USER_ID, normalizedTagName)

        return if (existing == null) {
            val tagId = EntityIdGenerator.newTagId()
            tagDao.insertTag(
                TagEntity(
                    id = tagId,
                    userId = LOCAL_USER_ID,
                    tagName = normalizedTagName,
                    colorIcon = null,
                    heatWeight = 1,
                    createTime = now,
                    updateTime = now,
                    syncStatus = SyncStatus.PENDING_CREATE.code,
                ),
            )
            tagId
        } else {
            if (existing.isDeleted) {
                tagDao.updateTag(
                    existing.copy(
                        tagName = normalizedTagName,
                        isDeleted = false,
                        heatWeight = existing.heatWeight.coerceAtLeast(1),
                        updateTime = now,
                        syncStatus = SyncStatus.bump(existing.syncStatus),
                    ),
                )
            }
            existing.id
        }
    }

    suspend fun deleteTags(tagIds: Set<String>) {
        if (tagIds.isEmpty()) return

        val now = System.currentTimeMillis()
        database.withTransaction {
            val tagsToDelete = tagDao.getTagsOnce().filter { tagIds.contains(it.id) }
            if (tagsToDelete.isEmpty()) return@withTransaction
            tagDao.updateTags(
                tagsToDelete.map { tag ->
                    tag.copy(
                        isDeleted = true,
                        updateTime = now,
                        syncStatus = SyncStatus.markDeleted(tag.syncStatus),
                    )
                },
            )
        }
    }

    private fun normalizeTagName(rawTagName: String): String {
        val segments = rawTagName.split('/')
            .map(String::trim)
            .filter(String::isNotEmpty)

        return when {
            segments.isEmpty() -> ""
            segments.size == 1 -> segments.first()
            else -> "${segments.first()}/${segments[1]}"
        }
    }
}
