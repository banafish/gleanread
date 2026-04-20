package com.gleanread.android.feature.excerpts.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.ui.richtext.LinkAwareText

@Composable
fun ExcerptPreviewDialog(
    excerpt: ExcerptUiModel,
    onDismiss: () -> Unit,
    onOpenNode: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = excerpt.sourceTitle ?: stringResource(R.string.excerpt_preview_title),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkAwareText(rawText = excerpt.content, onLinkClick = {})
                if (excerpt.thought.isNotBlank()) {
                    LinkAwareText(rawText = excerpt.thought, onLinkClick = {})
                }
                if (excerpt.archivedNodeTitle != null) {
                    Text(
                        text = stringResource(R.string.excerpt_preview_archived_to, excerpt.archivedNodeTitle),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpenNode(excerpt.archivedNodeId) }) {
                Text(stringResource(R.string.excerpt_preview_open_node))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}
