package com.gleanread.android.data.repository

import com.gleanread.android.data.sync.LocalChangeSyncTrigger

class FakeLocalChangeSyncTrigger : LocalChangeSyncTrigger {
    var changeCount = 0
        private set
    var flushCount = 0
        private set

    override fun onLocalDataChanged() {
        changeCount += 1
    }

    override fun flushPendingChanges() {
        flushCount += 1
    }
}
