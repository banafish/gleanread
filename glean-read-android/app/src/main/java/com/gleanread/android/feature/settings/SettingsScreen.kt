package com.gleanread.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.ui.theme.GleanReadTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSendMagicLink: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onMergeLocalData: () -> Unit,
    onKeepLocalData: () -> Unit,
    onUseCloudData: () -> Unit,
    onDismissOwnershipDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        AccountSection(
            uiState = uiState,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onSignIn = onSignIn,
            onSendMagicLink = onSendMagicLink,
            onSignOut = onSignOut,
        )

        SyncSection(
            uiState = uiState,
            onSyncNow = onSyncNow,
        )

        uiState.message?.takeIf(String::isNotBlank)?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (uiState.showOwnershipDialog) {
        DataOwnershipDialog(
            onMergeLocalData = onMergeLocalData,
            onKeepLocalData = onKeepLocalData,
            onUseCloudData = onUseCloudData,
            onDismiss = onDismissOwnershipDialog,
        )
    }
}

@Composable
private fun AccountSection(
    uiState: SettingsUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSendMagicLink: () -> Unit,
    onSignOut: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.Login,
                title = stringResource(R.string.settings_account_title),
            )

            if (uiState.isLoggedIn) {
                Text(
                    text = stringResource(
                        R.string.settings_logged_in_as,
                        uiState.sessionEmail ?: stringResource(R.string.settings_logged_in_unknown),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !uiState.isSubmitting && !uiState.isSyncing,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_sign_out))
                }
            } else {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_email_label)) },
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text(stringResource(R.string.settings_password_label)) },
                )
                Button(
                    onClick = onSignIn,
                    enabled = !uiState.isSubmitting,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_sign_in))
                }
                OutlinedButton(
                    onClick = onSendMagicLink,
                    enabled = !uiState.isSubmitting,
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_send_magic_link))
                }
                Text(
                    text = stringResource(R.string.settings_magic_link_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SyncSection(
    uiState: SettingsUiState,
    onSyncNow: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(
                icon = Icons.Default.CloudSync,
                title = stringResource(R.string.settings_sync_title),
            )
            Text(
                text = if (uiState.isCloudSyncEnabled) {
                    stringResource(R.string.settings_sync_enabled)
                } else {
                    stringResource(R.string.settings_sync_disabled)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.settings_last_sync,
                    formatSyncTime(uiState.lastSyncTime),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.failedCount > 0 || uiState.conflictCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_sync_attention,
                            uiState.failedCount,
                            uiState.conflictCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (uiState.conflictCount > 0) {
                Text(
                    text = stringResource(R.string.settings_sync_conflict_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSyncNow,
                enabled = uiState.isLoggedIn && uiState.isCloudSyncEnabled && !uiState.isSyncing,
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (uiState.isSyncing) {
                        stringResource(R.string.settings_syncing)
                    } else {
                        stringResource(R.string.settings_sync_now)
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
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

private fun formatSyncTime(value: Long?): String {
    if (value == null || value <= 0L) return "-"
    return runCatching {
        Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .format(SyncTimeFormatter)
    }.getOrDefault("-")
}

private val SyncTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    GleanReadTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                isLoggedIn = true,
                sessionEmail = "user@example.com",
                isCloudSyncEnabled = true,
                failedCount = 1,
                conflictCount = 1,
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onSendMagicLink = {},
            onSignOut = {},
            onSyncNow = {},
            onMergeLocalData = {},
            onKeepLocalData = {},
            onUseCloudData = {},
            onDismissOwnershipDialog = {},
        )
    }
}
