package com.gleanread.android.feature.tags.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gleanread.android.R

@Composable
fun AddTagDialog(
    tagName: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val inputBackgroundColor = if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tags_add_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = tagName,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.tags_add_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedContainerColor = inputBackgroundColor,
                        unfocusedContainerColor = inputBackgroundColor,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.tags_hierarchy_rule_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.tags_hierarchy_rule_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = normalizedTagName(tagName).isNotEmpty(),
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

private fun normalizedTagName(rawTagName: String): String {
    val segments = rawTagName.split('/')
        .map(String::trim)
        .filter(String::isNotEmpty)

    return when {
        segments.isEmpty() -> ""
        segments.size == 1 -> segments.first()
        else -> "${segments.first()}/${segments[1]}"
    }
}
