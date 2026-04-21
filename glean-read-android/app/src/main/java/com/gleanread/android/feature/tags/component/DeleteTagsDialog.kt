package com.gleanread.android.feature.tags.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.core.model.TagUiModel

@Composable
internal fun DeleteTagsDialog(
    tags: List<TagUiModel>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_delete_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.tags_delete_body,
                    tags.deletePreviewText(),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.tags_delete_selected))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun List<TagUiModel>.deletePreviewText(): String {
    if (isEmpty()) return "..."

    val preview = take(3)
        .joinToString(separator = "、") { "#${it.fullName}" }

    return if (size > 3) {
        "$preview 等 $size 个标签"
    } else {
        preview
    }
}
