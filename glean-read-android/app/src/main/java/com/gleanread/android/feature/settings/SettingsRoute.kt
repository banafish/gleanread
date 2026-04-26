package com.gleanread.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import com.gleanread.android.app.appContainer
import androidx.compose.ui.platform.LocalContext
import com.gleanread.android.data.auth.LocalDataOwnershipChoice

@Composable
fun SettingsRoute() {
    val appContainer = LocalContext.current.appContainer
    val viewModel: SettingsViewModel = viewModel(factory = appContainer.settingsViewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onSignIn = viewModel::signIn,
        onSignOut = viewModel::signOut,
        onSyncNow = viewModel::syncNow,
        onMergeLocalData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) },
        onKeepLocalData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.KEEP_LOCAL) },
        onUseCloudData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.USE_CLOUD) },
        onDismissOwnershipDialog = viewModel::dismissOwnershipDialog,
    )
}
