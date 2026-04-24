package com.gleanread.android.data.model

const val LOCAL_USER_ID = "local-user"

enum class SyncStatus(val code: Int) {
    SYNCED(0),
    PENDING_CREATE(1),
    PENDING_UPDATE(2),
    PENDING_DELETE(3),
    ;

    companion object {
        fun fromCode(code: Int): SyncStatus {
            return entries.first { it.code == code }
        }

        fun bump(current: SyncStatus): SyncStatus {
            return if (current == PENDING_CREATE) PENDING_CREATE else PENDING_UPDATE
        }

        fun markDeleted(current: SyncStatus): SyncStatus {
            return if (current == PENDING_CREATE) PENDING_CREATE else PENDING_DELETE
        }
    }
}
