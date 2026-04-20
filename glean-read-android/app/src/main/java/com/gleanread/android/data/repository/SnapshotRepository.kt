package com.gleanread.android.data.repository

import androidx.room.withTransaction
import com.gleanread.android.data.local.WorkspaceDatabase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class SnapshotRepository(
    private val database: WorkspaceDatabase,
) {
    private val dao = database.workspaceDao()

    val localSnapshot = combine(
        dao.observeExcerpts(),
        dao.observeNodes(),
        dao.observeTags(),
        dao.observeExcerptTags(),
    ) { excerpts, nodes, tags, relations ->
        WorkspaceLocalSnapshot(
            excerpts = excerpts,
            nodes = nodes,
            tags = tags,
            relations = relations,
        )
    }.distinctUntilChanged()

    suspend fun seedSampleData() {
        val hasData = dao.countExcerpts() > 0 || dao.countNodes() > 0 || dao.countTags() > 0
        if (hasData) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.insertNodes(WorkspaceSeedData.nodes(now))
            dao.insertTags(WorkspaceSeedData.tags(now))
            dao.insertExcerpts(WorkspaceSeedData.excerpts(now))
            dao.insertExcerptTags(WorkspaceSeedData.excerptTags(now))
        }
    }
}
