package com.gleanread.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gleanread.android.data.model.SyncStatus

@Entity(
    tableName = "knowledge_tree_node",
    indices = [Index("user_id"), Index("parent_id")],
)
data class KnowledgeTreeNodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    @ColumnInfo(name = "node_title") val nodeTitle: String,
    @ColumnInfo(name = "outline_markdown") val outlineMarkdown: String?,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "update_time") val updateTime: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long? = null,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "local_dirty_time") val localDirtyTime: Long? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Long = 0,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["user_id", "tag_name"], unique = true), Index("heat_weight")],
)
data class TagEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "tag_name") val tagName: String,
    @ColumnInfo(name = "color_icon") val colorIcon: String?,
    @ColumnInfo(name = "heat_weight") val heatWeight: Int,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "update_time") val updateTime: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long? = null,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "local_dirty_time") val localDirtyTime: Long? = null,
)

@Entity(
    tableName = "excerpts",
    indices = [Index("user_id"), Index("tree_node_id"), Index("create_time")],
)
data class ExcerptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val content: String,
    val url: String?,
    @ColumnInfo(name = "source_title") val sourceTitle: String?,
    @ColumnInfo(name = "user_thought") val userThought: String?,
    @ColumnInfo(name = "tree_node_id") val treeNodeId: String?,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "update_time") val updateTime: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long? = null,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "local_dirty_time") val localDirtyTime: Long? = null,
)

@Entity(
    tableName = "excerpt_tags",
    indices = [Index(value = ["excerpt_id", "tag_id"], unique = true), Index("excerpt_id"), Index("tag_id")],
)
data class ExcerptTagEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "excerpt_id") val excerptId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "update_time") val updateTime: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long? = null,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "local_dirty_time") val localDirtyTime: Long? = null,
)
