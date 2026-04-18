package com.gleanread.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM excerpts WHERE is_deleted = 0 ORDER BY create_time DESC")
    fun observeExcerpts(): Flow<List<ExcerptEntity>>

    @Query("SELECT * FROM knowledge_tree_node WHERE is_deleted = 0 ORDER BY create_time ASC")
    fun observeNodes(): Flow<List<KnowledgeTreeNodeEntity>>

    @Query("SELECT * FROM tags WHERE is_deleted = 0 ORDER BY heat_weight DESC, tag_name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM excerpt_tags WHERE is_deleted = 0")
    fun observeExcerptTags(): Flow<List<ExcerptTagEntity>>

    @Query("SELECT * FROM excerpts WHERE is_deleted = 0 ORDER BY create_time DESC")
    suspend fun getExcerptsOnce(): List<ExcerptEntity>

    @Query("SELECT * FROM knowledge_tree_node WHERE is_deleted = 0 ORDER BY create_time ASC")
    suspend fun getNodesOnce(): List<KnowledgeTreeNodeEntity>

    @Query("SELECT * FROM tags WHERE is_deleted = 0 ORDER BY heat_weight DESC, tag_name ASC")
    suspend fun getTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM excerpt_tags WHERE is_deleted = 0")
    suspend fun getExcerptTagsOnce(): List<ExcerptTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KnowledgeTreeNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<KnowledgeTreeNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerpt(excerpt: ExcerptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerpts(excerpts: List<ExcerptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerptTags(relations: List<ExcerptTagEntity>)

    @Update
    suspend fun updateNode(node: KnowledgeTreeNodeEntity)

    @Update
    suspend fun updateNodes(nodes: List<KnowledgeTreeNodeEntity>)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Update
    suspend fun updateTags(tags: List<TagEntity>)

    @Update
    suspend fun updateExcerpts(excerpts: List<ExcerptEntity>)

    @Query("SELECT * FROM tags WHERE user_id = :userId AND tag_name = :tagName LIMIT 1")
    suspend fun findTagByName(userId: String, tagName: String): TagEntity?

    @Query("SELECT * FROM knowledge_tree_node WHERE id = :nodeId LIMIT 1")
    suspend fun findNodeById(nodeId: String): KnowledgeTreeNodeEntity?

    @Query("SELECT * FROM excerpts WHERE is_deleted = 0 AND tree_node_id IN (:nodeIds)")
    suspend fun findExcerptsByNodeIds(nodeIds: List<String>): List<ExcerptEntity>

    @Query("SELECT COUNT(*) FROM excerpts WHERE is_deleted = 0")
    suspend fun countExcerpts(): Int

    @Query("SELECT COUNT(*) FROM knowledge_tree_node WHERE is_deleted = 0")
    suspend fun countNodes(): Int

    @Query("SELECT COUNT(*) FROM tags WHERE is_deleted = 0")
    suspend fun countTags(): Int
}
