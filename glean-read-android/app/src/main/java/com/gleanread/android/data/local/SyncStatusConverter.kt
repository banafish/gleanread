package com.gleanread.android.data.local

import androidx.room.TypeConverter
import com.gleanread.android.data.model.SyncStatus

class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.fromStoredValue(value)
}
