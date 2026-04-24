package com.gleanread.android.data.repository

import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.WorkspaceLocalSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 只负责聚合全局快照流，不包含任何 CRUD 操作。
 */
class WorkspaceSnapshotProvider(
    database: WorkspaceDatabase,
) {
    val localSnapshot: Flow<WorkspaceLocalSnapshot> = combine(
        database.excerptDao().observeExcerpts(),
        database.nodeDao().observeNodes(),
        database.tagDao().observeTags(),
        database.excerptTagDao().observeExcerptTags(),
    ) { excerpts, nodes, tags, relations ->
        WorkspaceLocalSnapshot(
            excerpts = excerpts,
            nodes = nodes,
            tags = tags,
            relations = relations,
        )
    }.distinctUntilChanged()
}
