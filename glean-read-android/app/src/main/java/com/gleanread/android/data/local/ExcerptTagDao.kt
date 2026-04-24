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

    @Query("SELECT * FROM excerpt_tags WHERE excerpt_id = :excerptId")
    suspend fun getAllExcerptTagsByExcerptId(excerptId: String): List<ExcerptTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerptTags(relations: List<ExcerptTagEntity>)

    @Update
    suspend fun updateExcerptTags(relations: List<ExcerptTagEntity>)
}
