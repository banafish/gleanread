package com.gleanread.android.data.sync

interface LocalChangeSyncTrigger {
    fun onLocalDataChanged()

    fun flushPendingChanges()
}

object NoOpLocalChangeSyncTrigger : LocalChangeSyncTrigger {
    override fun onLocalDataChanged() = Unit

    override fun flushPendingChanges() = Unit
}
