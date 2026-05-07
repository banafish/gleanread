package com.gleanread.android.data.repository

import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.local.WorkspaceDatabaseManager
import com.gleanread.android.data.model.WorkspaceLocalSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 只负责聚合全局快照流，不包含任何 CRUD 操作。
 */
class WorkspaceSnapshotProvider private constructor(
    private val databaseFlow: Flow<WorkspaceDatabase>,
) {
    constructor(databaseManager: WorkspaceDatabaseManager) : this(databaseManager.currentDatabase)

    internal constructor(database: WorkspaceDatabase) : this(flowOf(database))

    @OptIn(ExperimentalCoroutinesApi::class)
    val localSnapshot: Flow<WorkspaceLocalSnapshot> = databaseFlow.flatMapLatest { database ->
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
