package com.gleanread.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcerptTagDao {
    @Query("SELECT * FROM excerpt_tags WHERE is_deleted = 0")
    fun observeExcerptTags(): Flow<List<ExcerptTagEntity>>

    @Query("SELECT * FROM excerpt_tags WHERE is_deleted = 0")
    suspend fun getExcerptTagsOnce(): List<ExcerptTagEntity>

    @Query("SELECT * FROM excerpt_tags")
    suspend fun getAllExcerptTagsOnce(): List<ExcerptTagEntity>

    @Query("SELECT * FROM excerpt_tags WHERE sync_status IN (:syncStatuses) ORDER BY local_dirty_time ASC")
    suspend fun findExcerptTagsBySyncStatuses(syncStatuses: List<String>): List<ExcerptTagEntity>

    @Query("SELECT * FROM excerpt_tags WHERE excerpt_id = :excerptId")
    suspend fun getAllExcerptTagsByExcerptId(excerptId: String): List<ExcerptTagEntity>

    @Query("SELECT * FROM excerpt_tags WHERE id = :relationId LIMIT 1")
    suspend fun findExcerptTagById(relationId: String): ExcerptTagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerptTags(relations: List<ExcerptTagEntity>)

    @Update
    suspend fun updateExcerptTags(relations: List<ExcerptTagEntity>)

    @Query("SELECT COUNT(*) FROM excerpt_tags WHERE user_id = :userId")
    suspend fun countExcerptTagsByUserId(userId: String): Int

    @Query("DELETE FROM excerpt_tags")
    suspend fun deleteAllExcerptTags()
}
