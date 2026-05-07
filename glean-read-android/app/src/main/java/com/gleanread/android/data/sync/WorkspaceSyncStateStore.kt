package com.gleanread.android.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceSyncStateStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _lastPullTime = MutableStateFlow(readLastPullTime())
    private val _isCloudSyncEnabled = MutableStateFlow(readCloudSyncEnabled())

    val lastPullTime: StateFlow<Long?> = _lastPullTime.asStateFlow()
    val isCloudSyncEnabled: StateFlow<Boolean> = _isCloudSyncEnabled.asStateFlow()

    fun lastPullTimeForUser(userId: String): Long? {
        return preferences.getLong(lastPullTimeKey(userId), 0L).takeIf { it > 0L }
    }

    fun saveLastPullTime(userId: String, value: Long) {
        preferences.edit().putLong(lastPullTimeKey(userId), value).apply()
        _lastPullTime.value = value
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, enabled).apply()
        _isCloudSyncEnabled.value = enabled
    }

    fun clear() {
        preferences.edit().clear().apply()
        _lastPullTime.value = null
        _isCloudSyncEnabled.value = false
    }

    private fun readLastPullTime(): Long? = null

    private fun readCloudSyncEnabled(): Boolean {
        return preferences.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
    }

    private fun lastPullTimeKey(userId: String): String = "${KEY_LAST_PULL_TIME}_$userId"

    private companion object {
        const val PREFERENCES_NAME = "glean_workspace_sync_state"
        const val KEY_LAST_PULL_TIME = "last_pull_time"
        const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
    }
}
