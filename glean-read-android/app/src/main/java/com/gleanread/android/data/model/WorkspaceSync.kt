package com.gleanread.android.data.model

const val LOCAL_USER_ID = "local-user"

enum class SyncStatus(val code: Int) {
    SYNCED(0),
    PENDING_CREATE(1),
    PENDING_UPDATE(2),
    PENDING_DELETE(3),
    ;

    companion object {
        fun bump(current: Int): Int {
            return if (current == PENDING_CREATE.code) PENDING_CREATE.code else PENDING_UPDATE.code
        }

        fun markDeleted(current: Int): Int {
            return if (current == PENDING_CREATE.code) PENDING_CREATE.code else PENDING_DELETE.code
        }
    }
}
