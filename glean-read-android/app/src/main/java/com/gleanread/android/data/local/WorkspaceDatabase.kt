package com.gleanread.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        KnowledgeTreeNodeEntity::class,
        TagEntity::class,
        ExcerptEntity::class,
        ExcerptTagEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(SyncStatusConverter::class)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun excerptDao(): ExcerptDao
    abstract fun nodeDao(): KnowledgeTreeNodeDao
    abstract fun tagDao(): TagDao
    abstract fun excerptTagDao(): ExcerptTagDao
}
