package com.gleanread.android.data.repository

import com.gleanread.android.data.local.ActiveWorkspace
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.model.LOCAL_USER_ID

internal fun singleDatabaseWorkspace(
    database: WorkspaceDatabase,
    ownerUserId: String = LOCAL_USER_ID,
): ActiveWorkspace {
    return if (ownerUserId == LOCAL_USER_ID) {
        ActiveWorkspace.guest(
            databaseName = SINGLE_DATABASE_NAME,
            database = database,
        )
    } else {
        ActiveWorkspace.user(
            userId = ownerUserId,
            databaseName = SINGLE_DATABASE_NAME,
            database = database,
        )
    }
}

private const val SINGLE_DATABASE_NAME = "single.db"
