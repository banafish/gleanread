package com.gleanread.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.gleanread.android.R
import com.gleanread.android.data.appearance.ThemeColor
import com.gleanread.android.data.appearance.ThemeMode
import com.gleanread.android.data.avatar.CompressedImage
import com.gleanread.android.feature.settings.component.AppearanceSection
import com.gleanread.android.feature.settings.component.SyncSection
import com.gleanread.android.feature.settings.component.UserAvatarSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateToAuth: () -> Unit,
    onAvatarSelected: (CompressedImage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeColorChange: (ThemeColor) -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onMergeLocalData: () -> Unit,
    onKeepLocalData: () -> Unit,
    onUseCloudData: () -> Unit,
    onDismissOwnershipDialog: () -> Unit,
    onClearMessage: () -> Unit,
    onConfirmSignOutWithUnsyncedData: () -> Unit = {},
    onDismissUnsyncedWarning: () -> Unit = {},
    onClearLocalCache: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.message) {
        uiState.message?.takeIf { it.isNotBlank() }?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
            onClearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        val icon = when {
                            data.visuals.message.contains("成功") || data.visuals.message.contains("完成") -> Icons.Default.CheckCircle
                            data.visuals.message.contains("失败") -> Icons.Default.Error
                            else -> Icons.Default.Info
                        }
                        val iconColor = when {
                            data.visuals.message.contains("成功") || data.visuals.message.contains("完成") -> MaterialTheme.colorScheme.primary
                            data.visuals.message.contains("失败") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserAvatarSection(
                isLoggedIn = uiState.isLoggedIn,
                email = uiState.sessionEmail,
                avatarUrl = uiState.avatarUrl,
                isAvatarUploading = uiState.isAvatarUploading,
                onNavigateToAuth = onNavigateToAuth,
                onAvatarSelected = onAvatarSelected
            )

            AppearanceSection(
                themeMode = uiState.themeMode,
                themeColor = uiState.themeColor,
                onThemeModeChange = onThemeModeChange,
                onThemeColorChange = onThemeColorChange
            )

            SyncSection(
                uiState = uiState,
                onSyncNow = onSyncNow,
            )

            // 清除本地缓存按钮
            OutlinedButton(
                onClick = onClearLocalCache,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_clear_local_cache))
            }

            if (uiState.isLoggedIn) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_sign_out))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 本地数据归属弹窗
    if (uiState.showOwnershipDialog) {
        DataOwnershipDialog(
            onMergeLocalData = onMergeLocalData,
            onKeepLocalData = onKeepLocalData,
            onUseCloudData = onUseCloudData,
            onDismiss = onDismissOwnershipDialog,
        )
    }

    // 未同步数据警告弹窗
    if (uiState.showUnsyncedWarning) {
        UnsyncedDataWarningDialog(
            onConfirm = onConfirmSignOutWithUnsyncedData,
            onDismiss = onDismissUnsyncedWarning,
        )
    }
}

@Composable
private fun DataOwnershipDialog(
    onMergeLocalData: () -> Unit,
    onKeepLocalData: () -> Unit,
    onUseCloudData: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_ownership_title)) },
        text = { Text(stringResource(R.string.settings_ownership_body)) },
        confirmButton = {
            TextButton(onClick = onMergeLocalData) {
                Text(stringResource(R.string.settings_ownership_merge))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onKeepLocalData) {
                    Text(stringResource(R.string.settings_ownership_keep_local))
                }
                TextButton(onClick = onUseCloudData) {
                    Text(stringResource(R.string.settings_ownership_use_cloud))
                }
            }
        },
    )
}

@Composable
private fun UnsyncedDataWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.settings_unsynced_warning_title)) },
        text = { Text(stringResource(R.string.settings_unsynced_warning_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_unsynced_warning_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_unsynced_warning_cancel))
            }
        },
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    com.gleanread.android.core.ui.theme.GleanReadTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                isLoggedIn = true,
                sessionEmail = "user@example.com",
                isCloudSyncEnabled = true,
                failedCount = 1,
                conflictCount = 1,
            ),
            onNavigateToAuth = {},
            onAvatarSelected = {},
            onThemeModeChange = {},
            onThemeColorChange = {},
            onSignOut = {},
            onSyncNow = {},
            onMergeLocalData = {},
            onKeepLocalData = {},
            onUseCloudData = {},
            onDismissOwnershipDialog = {},
            onClearMessage = {},
        )
    }
}
