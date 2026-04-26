package com.gleanread.android.data.sync

interface WorkspaceRemoteDataSource {
    suspend fun upsertNodes(
        accessToken: String,
        nodes: List<RemoteKnowledgeTreeNode>,
    )

    suspend fun upsertTags(
        accessToken: String,
        tags: List<RemoteTag>,
    )

    suspend fun upsertExcerpts(
        accessToken: String,
        excerpts: List<RemoteExcerpt>,
    )

    suspend fun upsertExcerptTags(
        accessToken: String,
        relations: List<RemoteExcerptTag>,
    )

    suspend fun fetchChanges(
        accessToken: String,
        userId: String,
        updatedAfter: Long?,
    ): RemoteWorkspaceSnapshot
}
