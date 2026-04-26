package com.gleanread.android.feature.settings

data class SettingsUiState(
    val email: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val sessionEmail: String? = null,
    val isCloudSyncEnabled: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val message: String? = null,
    val showOwnershipDialog: Boolean = false,
)
