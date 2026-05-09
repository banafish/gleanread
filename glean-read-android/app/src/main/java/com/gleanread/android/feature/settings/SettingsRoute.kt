package com.gleanread.android.feature.settings

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.gleanread.android.app.appContainer
import com.gleanread.android.data.auth.LocalDataOwnershipChoice
import com.gleanread.android.platform.page_context.PageContextAccessibilityState

@Composable
fun SettingsRoute(
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContainer = context.appContainer
    val routeLifecycleOwner = LocalLifecycleOwner.current
    val lifecycleOwner = remember(context, routeLifecycleOwner) {
        context.findLifecycleOwner() ?: routeLifecycleOwner
    }
    val viewModel: SettingsViewModel = viewModel(factory = appContainer.settingsViewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isAccessibilityEnabled by remember(context) {
        mutableStateOf(PageContextAccessibilityState.isEnabled(context))
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = PageContextAccessibilityState.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SettingsScreen(
        uiState = uiState,
        onNavigateToAuth = onNavigateToAuth,
        onAvatarSelected = viewModel::uploadAvatar,
        onThemeModeChange = viewModel::setThemeMode,
        onThemeColorChange = viewModel::setThemeColor,
        isAccessibilityEnabled = isAccessibilityEnabled,
        onOpenAccessibilitySettings = {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
            )
        },
        onSignOut = viewModel::signOut,
        onSyncNow = viewModel::syncNow,
        onMergeLocalData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.MERGE_TO_ACCOUNT) },
        onKeepLocalData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.KEEP_LOCAL) },
        onUseCloudData = { viewModel.chooseOwnership(LocalDataOwnershipChoice.USE_CLOUD) },
        onDismissOwnershipDialog = viewModel::dismissOwnershipDialog,
        onClearMessage = viewModel::clearMessage,
        onConfirmSignOutWithUnsyncedData = viewModel::confirmSignOutWithUnsyncedData,
        onDismissUnsyncedWarning = viewModel::dismissUnsyncedWarning,
        onSaveAiConfig = viewModel::saveAiConfig,
        onTestAiConnection = viewModel::testAiConnection,
        onClearAiConnectionMessage = viewModel::clearAiConnectionMessage,
    )
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}
