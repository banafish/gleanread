package com.gleanread.android.feature.settings

import com.gleanread.android.data.ai.AiConfig
import com.gleanread.android.data.appearance.ThemeMode
import com.gleanread.android.data.appearance.ThemeColor

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val sessionEmail: String? = null,
    val avatarUrl: String? = null,
    val isAvatarUploading: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColor = ThemeColor.DYNAMIC,
    val isCloudSyncEnabled: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val aiConfig: AiConfig = AiConfig(),
    val isTestingAiConnection: Boolean = false,
    val aiConnectionMessage: String? = null,
    val isAiConnectionSuccess: Boolean? = null,
    val message: String? = null,
    val showOwnershipDialog: Boolean = false,
    val showUnsyncedWarning: Boolean = false,
)
