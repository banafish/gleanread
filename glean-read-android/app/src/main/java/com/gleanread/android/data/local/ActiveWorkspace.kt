package com.gleanread.android.data.local

/**
 * 当前可写工作区快照。
 *
 * 账号身份、数据库文件名和 Room 实例必须一起传递，避免一次同步或切换流程中
 * 把旧账号身份和新数据库实例拼在一起使用。
 */
data class ActiveWorkspace(
    val owner: ActiveWorkspaceOwner,
    val databaseName: String,
    val database: WorkspaceDatabase,
) {
    val userId: String?
        get() = (owner as? ActiveWorkspaceOwner.User)?.userId

    val isGuest: Boolean
        get() = owner == ActiveWorkspaceOwner.Guest

    companion object {
        fun guest(databaseName: String, database: WorkspaceDatabase): ActiveWorkspace {
            return ActiveWorkspace(
                owner = ActiveWorkspaceOwner.Guest,
                databaseName = databaseName,
                database = database,
            )
        }

        fun user(
            userId: String,
            databaseName: String,
            database: WorkspaceDatabase,
        ): ActiveWorkspace {
            return ActiveWorkspace(
                owner = ActiveWorkspaceOwner.User(userId),
                databaseName = databaseName,
                database = database,
            )
        }
    }
}

sealed interface ActiveWorkspaceOwner {
    data object Guest : ActiveWorkspaceOwner

    data class User(val userId: String) : ActiveWorkspaceOwner
}
