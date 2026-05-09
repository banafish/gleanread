package com.gleanread.android.feature.settings.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.data.ai.AiConfig

@Composable
fun AiConfigSection(
    aiConfig: AiConfig,
    isTestingConnection: Boolean,
    connectionMessage: String?,
    isConnectionSuccess: Boolean?,
    onSaveConfig: (AiConfig) -> Unit,
    onTestConnection: (AiConfig) -> Unit,
    onClearConnectionMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_ai_config_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Card(
            onClick = {
                onClearConnectionMessage()
                showDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false,
                )
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (aiConfig.isComplete) {
                            stringResource(R.string.settings_ai_configured)
                        } else {
                            stringResource(R.string.settings_ai_not_configured)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = aiConfig.model.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.settings_ai_config_summary_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDialog) {
        AiConfigDialog(
            aiConfig = aiConfig,
            isTestingConnection = isTestingConnection,
            connectionMessage = connectionMessage,
            isConnectionSuccess = isConnectionSuccess,
            onDismiss = { showDialog = false },
            onSaveConfig = { config ->
                onSaveConfig(config)
                showDialog = false
            },
            onTestConnection = onTestConnection,
            onClearConnectionMessage = onClearConnectionMessage,
        )
    }
}

@Composable
private fun AiConfigDialog(
    aiConfig: AiConfig,
    isTestingConnection: Boolean,
    connectionMessage: String?,
    isConnectionSuccess: Boolean?,
    onDismiss: () -> Unit,
    onSaveConfig: (AiConfig) -> Unit,
    onTestConnection: (AiConfig) -> Unit,
    onClearConnectionMessage: () -> Unit,
) {
    var baseUrl by rememberSaveable(aiConfig.baseUrl) { mutableStateOf(aiConfig.baseUrl) }
    var token by rememberSaveable(aiConfig.token) { mutableStateOf(aiConfig.token) }
    var model by rememberSaveable(aiConfig.model) { mutableStateOf(aiConfig.model) }
    val draftConfig = AiConfig(
        baseUrl = baseUrl,
        token = token,
        model = model,
    )

    AlertDialog(
        onDismissRequest = {
            if (!isTestingConnection) {
                onDismiss()
            }
        },
        title = {
            Text(stringResource(R.string.settings_ai_config_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AiConfigTextField(
                    label = stringResource(R.string.settings_ai_base_url_label),
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        onClearConnectionMessage()
                    },
                    placeholder = stringResource(R.string.settings_ai_base_url_placeholder),
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                )
                AiConfigTextField(
                    label = stringResource(R.string.settings_ai_token_label),
                    value = token,
                    onValueChange = {
                        token = it
                        onClearConnectionMessage()
                    },
                    placeholder = stringResource(R.string.settings_ai_token_placeholder),
                    isSecret = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                )
                AiConfigTextField(
                    label = stringResource(R.string.settings_ai_model_label),
                    value = model,
                    onValueChange = {
                        model = it
                        onClearConnectionMessage()
                    },
                    placeholder = stringResource(R.string.settings_ai_model_placeholder),
                    imeAction = ImeAction.Done,
                )
                Button(
                    onClick = { onTestConnection(draftConfig) },
                    enabled = !isTestingConnection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_ai_test_connection))
                    }
                }
                connectionMessage?.let { message ->
                    AiConnectionResult(
                        message = message,
                        isSuccess = isConnectionSuccess == true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSaveConfig(draftConfig) },
                enabled = !isTestingConnection,
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isTestingConnection,
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun AiConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 4.dp),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (isSecret && !visible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isSecret) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (visible) {
                                stringResource(R.string.settings_ai_hide_token)
                            } else {
                                stringResource(R.string.settings_ai_show_token)
                            },
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            shape = RoundedCornerShape(32.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(32.dp),
                ),
        )
    }
}

@Composable
private fun AiConnectionResult(
    message: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSuccess) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isSuccess) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
