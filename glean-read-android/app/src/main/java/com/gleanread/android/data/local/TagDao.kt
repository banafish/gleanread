package com.gleanread.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE is_deleted = 0 ORDER BY heat_weight DESC, tag_name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE is_deleted = 0 ORDER BY heat_weight DESC, tag_name ASC")
    suspend fun getTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM tags ORDER BY heat_weight DESC, tag_name ASC")
    suspend fun getAllTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE sync_status IN (:syncStatuses) ORDER BY local_dirty_time ASC")
    suspend fun findTagsBySyncStatuses(syncStatuses: List<String>): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Update
    suspend fun updateTags(tags: List<TagEntity>)

    @Query("SELECT * FROM tags WHERE user_id = :userId AND tag_name = :tagName LIMIT 1")
    suspend fun findTagByName(userId: String, tagName: String): TagEntity?

    @Query("SELECT COUNT(*) FROM tags WHERE is_deleted = 0")
    suspend fun countTags(): Int

    @Query("SELECT COUNT(*) FROM tags WHERE user_id = :userId")
    suspend fun countTagsByUserId(userId: String): Int

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
