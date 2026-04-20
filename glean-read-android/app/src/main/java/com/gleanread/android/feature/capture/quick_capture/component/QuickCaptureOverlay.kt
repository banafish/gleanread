@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.feature.capture.quick_capture.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.model.TreeNodeUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.core.ui.component.CaptureBottomSheet
import com.gleanread.android.core.ui.component.TagPill
import com.gleanread.android.core.ui.richtext.InlineLinkEditor
import com.gleanread.android.feature.capture.quick_capture.QuickCaptureDraft

@Composable
fun QuickCaptureOverlay(
    snapshot: WorkspaceSnapshot,
    draft: QuickCaptureDraft,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onThoughtChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onArchiveNodeSelect: (String?) -> Unit,
    onSave: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showArchivePicker by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.quick_capture_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            InlineLinkEditor(
                rawText = draft.content,
                placeholder = stringResource(R.string.quick_capture_content_placeholder),
                onRawTextChange = onContentChange,
                searchSuggestions = searchSuggestions,
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                autoFocus = true,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.quick_capture_link_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            InlineLinkEditor(
                rawText = draft.thought,
                placeholder = stringResource(R.string.quick_capture_thought_placeholder),
                onRawTextChange = onThoughtChange,
                searchSuggestions = searchSuggestions,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.quick_capture_url_placeholder))
                    }
                },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.quick_capture_suggested_tags),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                snapshot.suggestedTags.forEach { tag ->
                    val selected = draft.selectedTags.contains(tag.fullName)
                    TagPill(label = tag.label, isSelected = selected) { onTagToggle(tag.fullName) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { showArchivePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = draft.archiveNodeId?.let {
                            snapshot.flatNodes[it]?.title ?: stringResource(R.string.archive_picker_inbox)
                        } ?: stringResource(R.string.archive_picker_inbox),
                        textAlign = TextAlign.Left,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    onSave()
                },
                enabled = draft.content.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.quick_capture_submit))
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showArchivePicker) {
        ArchivePickerDialog(
            snapshot = snapshot,
            selectedNodeId = draft.archiveNodeId,
            onDismiss = { showArchivePicker = false },
            onSelect = {
                onArchiveNodeSelect(it)
                showArchivePicker = false
            },
        )
    }
}

@Composable
private fun ArchivePickerDialog(
    snapshot: WorkspaceSnapshot,
    selectedNodeId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.archive_picker_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                TextButton(onClick = { onSelect(null) }) {
                    if (selectedNodeId == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(stringResource(R.string.archive_picker_inbox))
                }
                snapshot.treeRoots.forEach { node ->
                    ArchiveNodeRow(
                        node = node,
                        selectedNodeId = selectedNodeId,
                        onSelect = onSelect,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

@Composable
private fun ArchiveNodeRow(
    node: TreeNodeUiModel,
    selectedNodeId: String?,
    onSelect: (String?) -> Unit,
    level: Int = 0,
) {
    Column {
        TextButton(
            onClick = { onSelect(node.id) },
            modifier = Modifier.padding(start = (level * 16).dp),
        ) {
            if (selectedNodeId == node.id) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(node.title, color = MaterialTheme.colorScheme.onSurface)
        }
        node.children.forEach {
            ArchiveNodeRow(
                node = it,
                selectedNodeId = selectedNodeId,
                onSelect = onSelect,
                level = level + 1,
            )
        }
    }
}

