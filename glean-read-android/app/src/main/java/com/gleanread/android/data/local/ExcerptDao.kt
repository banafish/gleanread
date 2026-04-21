package com.gleanread.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcerptDao {
    @Query("SELECT * FROM excerpts WHERE is_deleted = 0 ORDER BY create_time DESC")
    fun observeExcerpts(): Flow<List<ExcerptEntity>>

    @Query("SELECT * FROM excerpts WHERE is_deleted = 0 ORDER BY create_time DESC")
    suspend fun getExcerptsOnce(): List<ExcerptEntity>

    @Query("SELECT * FROM excerpts WHERE id = :excerptId LIMIT 1")
    suspend fun findExcerptById(excerptId: String): ExcerptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerpt(excerpt: ExcerptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerpts(excerpts: List<ExcerptEntity>)

    @Update
    suspend fun updateExcerpts(excerpts: List<ExcerptEntity>)

    @Query("SELECT * FROM excerpts WHERE is_deleted = 0 AND tree_node_id IN (:nodeIds)")
    suspend fun findExcerptsByNodeIds(nodeIds: List<String>): List<ExcerptEntity>

    @Query("SELECT COUNT(*) FROM excerpts WHERE is_deleted = 0")
    suspend fun countExcerpts(): Int
}
