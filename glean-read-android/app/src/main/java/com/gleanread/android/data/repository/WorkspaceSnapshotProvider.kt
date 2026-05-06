package com.gleanread.android.data.repository

import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.WorkspaceLocalSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

/**
 * 只负责聚合全局快照流，不包含任何 CRUD 操作。
 */
class WorkspaceSnapshotProvider(
    private val databaseManager: WorkspaceDatabaseManager,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val localSnapshot: Flow<WorkspaceLocalSnapshot> = databaseManager.currentDatabase.flatMapLatest { database ->
        combine(
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
        }
    }.distinctUntilChanged()
}
