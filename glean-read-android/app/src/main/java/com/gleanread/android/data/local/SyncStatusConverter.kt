package com.gleanread.android.data.local

import androidx.room.TypeConverter
import com.gleanread.android.data.model.SyncStatus

class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): Int = status.code

    @TypeConverter
    fun toSyncStatus(code: Int): SyncStatus = SyncStatus.fromCode(code)
}
