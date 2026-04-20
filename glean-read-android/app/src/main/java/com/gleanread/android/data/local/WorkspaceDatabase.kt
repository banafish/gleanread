package com.gleanread.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KnowledgeTreeNodeEntity::class,
        TagEntity::class,
        ExcerptEntity::class,
        ExcerptTagEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun excerptDao(): ExcerptDao
    abstract fun nodeDao(): KnowledgeTreeNodeDao
    abstract fun tagDao(): TagDao
    abstract fun excerptTagDao(): ExcerptTagDao

    companion object {
        @Volatile
        private var instance: WorkspaceDatabase? = null

        fun get(context: Context): WorkspaceDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkspaceDatabase::class.java,
                    "glean_workspace.db",
                ).build().also { instance = it }
            }
        }
    }
}
