package com.gleanread.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcerptTagDao {
    @Query("SELECT * FROM excerpt_tags WHERE is_deleted = 0")
    fun observeExcerptTags(): Flow<List<ExcerptTagEntity>>

    @Query("SELECT * FROM excerpt_tags WHERE is_deleted = 0")
    suspend fun getExcerptTagsOnce(): List<ExcerptTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcerptTags(relations: List<ExcerptTagEntity>)
}
