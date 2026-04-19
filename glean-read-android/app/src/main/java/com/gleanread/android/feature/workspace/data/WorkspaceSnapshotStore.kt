package com.gleanread.android.feature.workspace.data

import com.gleanread.android.data.repository.WorkspaceRepository
import com.gleanread.android.feature.workspace.model.WorkspaceSnapshot
import com.gleanread.android.feature.workspace.model.WorkspaceSnapshotFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WorkspaceSnapshotStore(
    repository: WorkspaceRepository,
    private val factory: WorkspaceSnapshotFactory = WorkspaceSnapshotFactory(),
) {
    val snapshot: Flow<WorkspaceSnapshot> = repository.localSnapshot
        .map(factory::create)
        .distinctUntilChanged()
}
