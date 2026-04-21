package com.gleanread.android.feature.excerpts.feed

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel

@Composable
internal fun DeleteExcerptDialog(
    excerpt: ExcerptUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feed_delete_excerpt_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.feed_delete_excerpt_body,
                    excerpt.content.deletePreviewText(),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.knowledge_tree_delete_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun String.deletePreviewText(): String {
    return lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ")
        .ifBlank { "..." }
        .take(80)
}
