package com.gleanread.android.core.data

import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.model.WorkspaceSnapshotFactory
import com.gleanread.android.data.repository.WorkspaceSnapshotProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppSnapshotStore(
    snapshotProvider: WorkspaceSnapshotProvider,
    private val factory: WorkspaceSnapshotFactory = WorkspaceSnapshotFactory(),
) {
    val snapshot: Flow<WorkspaceSnapshot> = snapshotProvider.localSnapshot
        .map(factory::create)
        .distinctUntilChanged()
}
