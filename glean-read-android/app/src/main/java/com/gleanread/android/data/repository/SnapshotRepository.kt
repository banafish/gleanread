package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
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

    suspend fun createExcerpt(
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
    ): String {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return ""

        val excerptId = EntityIdGenerator.newDraftExcerptId()
        val now = System.currentTimeMillis()
        database.withTransaction {
            val resolvedArchiveNodeId = archiveNodeId?.takeIf { nodeDao.findNodeById(it) != null }
            val normalizedTagNames = tagNames
                .map(::normalizeTagName)
                .filter(String::isNotEmpty)
                .toSet()
            val tagsByName = tagDao.getTagsOnce().associateBy(TagEntity::tagName)
            val targetTags = normalizedTagNames.mapNotNull(tagsByName::get)

            excerptDao.insertExcerpt(
                ExcerptEntity(
                    id = excerptId,
                    userId = LOCAL_USER_ID,
                    content = trimmedContent,
                    userThought = thought.trim().takeIf { it.isNotEmpty() },
                    sourceTitle = sourceTitle?.trim()?.takeIf { it.isNotEmpty() },
                    url = url?.trim()?.takeIf { it.isNotEmpty() },
                    treeNodeId = resolvedArchiveNodeId,
                    createTime = now,
                    updateTime = now,
                    syncStatus = SyncStatus.PENDING_CREATE.code,
                ),
            )

            if (targetTags.isNotEmpty()) {
                excerptTagDao.insertExcerptTags(
                    targetTags.map { tag ->
                        ExcerptTagEntity(
                            id = EntityIdGenerator.newRelationId(),
                            userId = LOCAL_USER_ID,
                            excerptId = excerptId,
                            tagId = tag.id,
                            createTime = now,
                            updateTime = now,
                            syncStatus = SyncStatus.PENDING_CREATE.code,
                        )
                    },
                )
            }
        }
        return excerptId
    }

    suspend fun updateExcerpt(
        excerptId: String,
        content: String,
        thought: String,
        sourceTitle: String?,
        url: String?,
        tagNames: Set<String>,
        archiveNodeId: String?,
    ): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return false

        var updated = false
        database.withTransaction {
            val excerpt = excerptDao.findExcerptById(excerptId) ?: return@withTransaction
            val resolvedArchiveNodeId = when {
                archiveNodeId == null -> null
                nodeDao.findNodeById(archiveNodeId) != null -> archiveNodeId
                else -> excerpt.treeNodeId
            }
            val normalizedTagNames = tagNames
                .map(::normalizeTagName)
                .filter(String::isNotEmpty)
                .toSet()
            val tagsByName = tagDao.getTagsOnce().associateBy(TagEntity::tagName)
            val targetTags = normalizedTagNames.mapNotNull(tagsByName::get)
            val targetTagIds = targetTags.map(TagEntity::id).toSet()
            val existingRelations = excerptTagDao.getExcerptTagsByExcerptId(excerptId)
            val existingRelationsByTagId = existingRelations.associateBy(ExcerptTagEntity::tagId)
            val now = System.currentTimeMillis()

            excerptDao.updateExcerpts(
                listOf(
                    excerpt.copy(
                        content = trimmedContent,
                        userThought = thought.trim().takeIf { it.isNotEmpty() },
                        sourceTitle = sourceTitle?.trim()?.takeIf { it.isNotEmpty() },
                        url = url?.trim()?.takeIf { it.isNotEmpty() },
                        treeNodeId = resolvedArchiveNodeId,
                        updateTime = now,
                        syncStatus = SyncStatus.bump(excerpt.syncStatus),
                    ),
                ),
            )

            val newRelations = mutableListOf<ExcerptTagEntity>()
            val updatedRelations = mutableListOf<ExcerptTagEntity>()

            targetTagIds.forEach { tagId ->
                val existing = existingRelationsByTagId[tagId]
                when {
                    existing == null -> {
                        newRelations += ExcerptTagEntity(
                            id = EntityIdGenerator.newRelationId(),
                            userId = LOCAL_USER_ID,
                            excerptId = excerptId,
                            tagId = tagId,
                            createTime = now,
                            updateTime = now,
                            syncStatus = SyncStatus.PENDING_CREATE.code,
                        )
                    }

                    existing.isDeleted -> {
                        updatedRelations += existing.copy(
                            isDeleted = false,
                            updateTime = now,
                            syncStatus = SyncStatus.bump(existing.syncStatus),
                        )
                    }
                }
            }

            existingRelations
                .filter { !it.isDeleted && it.tagId !in targetTagIds }
                .forEach { relation ->
                    updatedRelations += relation.copy(
                        isDeleted = true,
                        updateTime = now,
                        syncStatus = SyncStatus.markDeleted(relation.syncStatus),
                    )
                }

            if (newRelations.isNotEmpty()) {
                excerptTagDao.insertExcerptTags(newRelations)
            }
            if (updatedRelations.isNotEmpty()) {
                excerptTagDao.updateExcerptTags(updatedRelations)
            }

            updated = true
        }
        return updated
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
        val segments = rawTagName.removePrefix("#").split('/')
            .map(String::trim)
            .filter(String::isNotEmpty)

        return when {
            segments.isEmpty() -> ""
            segments.size == 1 -> segments.first()
            else -> "${segments.first()}/${segments[1]}"
        }
    }
}
