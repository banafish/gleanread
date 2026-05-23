package com.gleanread.android.data.sync

interface WorkspaceRemoteDataSource {
    suspend fun upsertNodes(
        accessToken: String,
        nodes: List<RemoteKnowledgeTreeNode>,
    ): List<ConditionalPushResult>

    suspend fun upsertTags(
        accessToken: String,
        tags: List<RemoteTag>,
    ): List<ConditionalPushResult>

    suspend fun upsertExcerpts(
        accessToken: String,
        excerpts: List<RemoteExcerpt>,
    ): List<ConditionalPushResult>

    suspend fun upsertExcerptTags(
        accessToken: String,
        relations: List<RemoteExcerptTag>,
    ): List<ConditionalPushResult>

    suspend fun fetchNode(
        accessToken: String,
        userId: String,
        nodeId: String,
    ): RemoteKnowledgeTreeNode?

    suspend fun fetchTag(
        accessToken: String,
        userId: String,
        tagId: String,
    ): RemoteTag?

    suspend fun fetchExcerpt(
        accessToken: String,
        userId: String,
        excerptId: String,
    ): RemoteExcerpt?

    suspend fun fetchExcerptTag(
        accessToken: String,
        userId: String,
        relationId: String,
    ): RemoteExcerptTag?

    suspend fun fetchChanges(
        accessToken: String,
        userId: String,
        updatedAfter: Long?,
    ): RemoteWorkspaceSnapshot
}
