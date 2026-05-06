package com.gleanread.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.gleanread.android.app.appContainer
import com.gleanread.android.data.auth.LocalDataOwnershipChoice

@Composable
fun SettingsRoute(
    onNavigateToAuth: () -> Unit = {}
) {
    val appContainer = LocalContext.current.appContainer
    val viewModel: SettingsViewModel = viewModel(factory = appContainer.settingsViewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onNavigateToAuth = onNavigateToAuth,
        onAvatarSelected = viewModel::uploadAvatar,
        onThemeModeChange = viewModel::setThemeMode,
        onThemeColorChange = viewModel::setThemeColor,
        onSignOut = viewModel::signOut,
        onSyncNow = viewModel::syncNow,
        onMergeLocalData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) },
        onKeepLocalData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.KEEP_LOCAL) },
        onUseCloudData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.USE_CLOUD) },
        onDismissOwnershipDialog = viewModel::dismissOwnershipDialog,
        onClearMessage = viewModel::clearMessage,
        onConfirmSignOutWithUnsyncedData = viewModel::confirmSignOutWithUnsyncedData,
        onDismissUnsyncedWarning = viewModel::dismissUnsyncedWarning,
        onClearLocalCache = viewModel::clearLocalCache,
    )
}
