package com.gleanread.android.data.sync

import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.model.SyncStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class RemoteWorkspaceSnapshot(
    val nodes: List<RemoteKnowledgeTreeNode>,
    val tags: List<RemoteTag>,
    val excerpts: List<RemoteExcerpt>,
    val excerptTags: List<RemoteExcerptTag>,
)

@Serializable
data class RemoteKnowledgeTreeNode(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("parent_id") val parentId: String?,
    @SerialName("node_title") val nodeTitle: String,
    @SerialName("outline_markdown") val outlineMarkdown: String?,
    @SerialName("create_time") val createTime: Long,
    @SerialName("update_time") val updateTime: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("device_id") val deviceId: String?,
)

@Serializable
data class RemoteTag(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("tag_name") val tagName: String,
    @SerialName("color_icon") val colorIcon: String?,
    @SerialName("heat_weight") val heatWeight: Int,
    @SerialName("create_time") val createTime: Long,
    @SerialName("update_time") val updateTime: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("device_id") val deviceId: String?,
)

@Serializable
data class RemoteExcerpt(
    val id: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    val url: String?,
    @SerialName("source_title") val sourceTitle: String?,
    @SerialName("user_thought") val userThought: String?,
    @SerialName("tree_node_id") val treeNodeId: String?,
    @SerialName("create_time") val createTime: Long,
    @SerialName("update_time") val updateTime: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("device_id") val deviceId: String?,
)

@Serializable
data class RemoteExcerptTag(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("excerpt_id") val excerptId: String,
    @SerialName("tag_id") val tagId: String,
    @SerialName("create_time") val createTime: Long,
    @SerialName("update_time") val updateTime: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("device_id") val deviceId: String?,
)

fun KnowledgeTreeNodeEntity.toRemote(): RemoteKnowledgeTreeNode {
    return RemoteKnowledgeTreeNode(
        id = id,
        userId = userId,
        parentId = parentId,
        nodeTitle = nodeTitle,
        outlineMarkdown = outlineMarkdown,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
    )
}

fun TagEntity.toRemote(): RemoteTag {
    return RemoteTag(
        id = id,
        userId = userId,
        tagName = tagName,
        colorIcon = colorIcon,
        heatWeight = heatWeight,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
    )
}

fun ExcerptEntity.toRemote(): RemoteExcerpt {
    return RemoteExcerpt(
        id = id,
        userId = userId,
        content = content,
        url = url,
        sourceTitle = sourceTitle,
        userThought = userThought,
        treeNodeId = treeNodeId,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
    )
}

fun ExcerptTagEntity.toRemote(): RemoteExcerptTag {
    return RemoteExcerptTag(
        id = id,
        userId = userId,
        excerptId = excerptId,
        tagId = tagId,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
    )
}

fun RemoteKnowledgeTreeNode.toEntity(
    syncStatus: SyncStatus,
    lastSyncTime: Long,
): KnowledgeTreeNodeEntity {
    return KnowledgeTreeNodeEntity(
        id = id,
        userId = userId,
        parentId = parentId,
        nodeTitle = nodeTitle,
        outlineMarkdown = outlineMarkdown,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
        syncStatus = syncStatus,
        lastSyncTime = lastSyncTime,
    )
}

fun RemoteTag.toEntity(
    syncStatus: SyncStatus,
    lastSyncTime: Long,
): TagEntity {
    return TagEntity(
        id = id,
        userId = userId,
        tagName = tagName,
        colorIcon = colorIcon,
        heatWeight = heatWeight,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
        syncStatus = syncStatus,
        lastSyncTime = lastSyncTime,
    )
}

fun RemoteExcerpt.toEntity(
    syncStatus: SyncStatus,
    lastSyncTime: Long,
): ExcerptEntity {
    return ExcerptEntity(
        id = id,
        userId = userId,
        content = content,
        url = url,
        sourceTitle = sourceTitle,
        userThought = userThought,
        treeNodeId = treeNodeId,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
        syncStatus = syncStatus,
        lastSyncTime = lastSyncTime,
    )
}

fun RemoteExcerptTag.toEntity(
    syncStatus: SyncStatus,
    lastSyncTime: Long,
): ExcerptTagEntity {
    return ExcerptTagEntity(
        id = id,
        userId = userId,
        excerptId = excerptId,
        tagId = tagId,
        createTime = createTime,
        updateTime = updateTime,
        isDeleted = isDeleted,
        deviceId = deviceId,
        syncStatus = syncStatus,
        lastSyncTime = lastSyncTime,
    )
}
