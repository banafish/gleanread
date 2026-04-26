package com.gleanread.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeTreeNodeDao {
    @Query("SELECT * FROM knowledge_tree_node WHERE is_deleted = 0 ORDER BY create_time ASC")
    fun observeNodes(): Flow<List<KnowledgeTreeNodeEntity>>

    @Query("SELECT * FROM knowledge_tree_node WHERE is_deleted = 0 ORDER BY create_time ASC")
    suspend fun getNodesOnce(): List<KnowledgeTreeNodeEntity>

    @Query("SELECT * FROM knowledge_tree_node ORDER BY create_time ASC")
    suspend fun getAllNodesOnce(): List<KnowledgeTreeNodeEntity>

    @Query("SELECT * FROM knowledge_tree_node WHERE sync_status IN (:syncStatuses) ORDER BY local_dirty_time ASC")
    suspend fun findNodesBySyncStatuses(syncStatuses: List<String>): List<KnowledgeTreeNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KnowledgeTreeNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<KnowledgeTreeNodeEntity>)

    @Update
    suspend fun updateNode(node: KnowledgeTreeNodeEntity)

    @Update
    suspend fun updateNodes(nodes: List<KnowledgeTreeNodeEntity>)

    @Query("SELECT * FROM knowledge_tree_node WHERE id = :nodeId LIMIT 1")
    suspend fun findNodeById(nodeId: String): KnowledgeTreeNodeEntity?

    @Query("SELECT COUNT(*) FROM knowledge_tree_node WHERE is_deleted = 0")
    suspend fun countNodes(): Int

    @Query("SELECT COUNT(*) FROM knowledge_tree_node WHERE user_id = :userId")
    suspend fun countNodesByUserId(userId: String): Int

    @Query("DELETE FROM knowledge_tree_node")
    suspend fun deleteAllNodes()
}
