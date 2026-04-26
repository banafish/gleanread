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

    fun saveLastPullTime(value: Long) {
        preferences.edit().putLong(KEY_LAST_PULL_TIME, value).apply()
        _lastPullTime.value = value
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, enabled).apply()
        _isCloudSyncEnabled.value = enabled
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_LAST_PULL_TIME)
            .remove(KEY_CLOUD_SYNC_ENABLED)
            .apply()
        _lastPullTime.value = null
        _isCloudSyncEnabled.value = false
    }

    private fun readLastPullTime(): Long? {
        return preferences.getLong(KEY_LAST_PULL_TIME, 0L).takeIf { it > 0L }
    }

    private fun readCloudSyncEnabled(): Boolean {
        return preferences.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
    }

    private companion object {
        const val PREFERENCES_NAME = "glean_workspace_sync_state"
        const val KEY_LAST_PULL_TIME = "last_pull_time"
        const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
    }
}
