package com.gleanread.android.feature.settings.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.feature.settings.SettingsUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SyncSection(
    uiState: SettingsUiState,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 同步标题
        Text(
            text = stringResource(R.string.settings_sync_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (uiState.isCloudSyncEnabled) {
                        stringResource(R.string.settings_sync_enabled)
                    } else {
                        stringResource(R.string.settings_sync_disabled)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.settings_last_sync,
                        formatSyncTime(uiState.lastSyncTime)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.failedCount > 0 || uiState.conflictCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_sync_attention,
                                uiState.failedCount,
                                uiState.conflictCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (uiState.conflictCount > 0) {
                    Text(
                        text = stringResource(R.string.settings_sync_conflict_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onSyncNow,
                    enabled = uiState.isLoggedIn && uiState.isCloudSyncEnabled && !uiState.isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (uiState.isSyncing) {
                            stringResource(R.string.settings_syncing)
                        } else {
                            stringResource(R.string.settings_sync_now)
                        }
                    )
                }
            }
        }
    }
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
