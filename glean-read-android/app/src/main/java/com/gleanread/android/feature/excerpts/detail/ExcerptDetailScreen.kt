@file:OptIn(ExperimentalMaterial3Api::class)

package com.gleanread.android.feature.excerpts.detail

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.model.TagGroupUiModel
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.richtext.LinkSuggestion
import com.gleanread.android.core.ui.richtext.InlineLinkEditor
import com.gleanread.android.core.ui.richtext.LinkAwareText
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeBreadcrumbUiModel
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeNodePickerDestinationUiModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExcerptDetailScreen(
    excerpt: ExcerptUiModel,
    content: String,
    thought: String,
    sourceTitle: String,
    url: String,
    selectedTagNames: List<String>,
    archiveNodeId: String?,
    archiveNodeTitle: String?,
    tagGroups: List<TagGroupUiModel>,
    breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    destinations: List<KnowledgeTreeNodePickerDestinationUiModel>,
    isEditing: Boolean,
    isCreateMode: Boolean = false,
    canSave: Boolean,
    isTagPickerOpen: Boolean,
    isMountPickerOpen: Boolean,
    mountPickerCurrentNodeId: String?,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onBack: () -> Unit,
    onCloseEditing: () -> Unit,
    onStartEditing: () -> Unit,
    onSave: () -> Unit,
    onContentChange: (String) -> Unit,
    onThoughtChange: (String) -> Unit,
    onSourceTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onOpenArchiveNode: (String) -> Unit,
    onOpenLinkedTarget: (String) -> Unit,
    onOpenTagPicker: () -> Unit,
    onDismissTagPicker: () -> Unit,
    onToggleTag: (String) -> Unit,
    onOpenMountPicker: () -> Unit,
    onDismissMountPicker: () -> Unit,
    onNavigateMountPicker: (String?) -> Unit,
    onConfirmMountPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clearEditorFocus = remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            when {
                                isCreateMode -> R.string.excerpt_detail_create_title
                                isEditing -> R.string.excerpt_detail_edit_title
                                else -> R.string.excerpt_detail_title
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (isEditing) onCloseEditing else onBack) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close
                            else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                if (isEditing) R.string.common_close else R.string.common_back,
                            ),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = if (isEditing) onSave else onStartEditing, enabled = !isEditing || canSave) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = stringResource(
                                if (isEditing) R.string.common_save else R.string.excerpt_detail_edit_action,
                            ),
                        )
                    }
                },
                colors = excerptDetailTopBarColors(),
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "meta") {
                ExcerptMetadataSection(
                    archiveNodeId = archiveNodeId,
                    archiveNodeTitle = archiveNodeTitle,
                    selectedTagNames = selectedTagNames,
                    createTime = excerpt.createTime,
                    isEditing = isEditing,
                    onOpenArchiveNode = onOpenArchiveNode,
                    onOpenTagPicker = {
                        clearEditorFocus()
                        onOpenTagPicker()
                    },
                    onOpenMountPicker = {
                        clearEditorFocus()
                        onOpenMountPicker()
                    },
                )
            }

            item(key = "content") {
                ExcerptContentSection(
                    title = stringResource(R.string.excerpt_detail_content_title),
                    rawText = content,
                    placeholder = stringResource(R.string.excerpt_detail_content_placeholder),
                    isEditing = isEditing,
                    searchSuggestions = searchSuggestions,
                    containerColor = if (isEditing) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                    minLines = 8,
                    onValueChange = onContentChange,
                    onOpenLinkedTarget = onOpenLinkedTarget,
                )
            }

            item(key = "thought") {
                ExcerptThoughtSection(
                    thought = thought,
                    isEditing = isEditing,
                    searchSuggestions = searchSuggestions,
                    onThoughtChange = onThoughtChange,
                    onOpenLinkedTarget = onOpenLinkedTarget,
                )
            }

            if (isEditing || sourceTitle.isNotBlank() || url.isNotBlank()) {
                item(key = "source") {
                    ExcerptSourceSection(
                        sourceTitle = sourceTitle,
                        url = url,
                        isEditing = isEditing,
                        onSourceTitleChange = onSourceTitleChange,
                        onUrlChange = onUrlChange,
                    )
                }
            }
        }
    }

    if (isTagPickerOpen) {
        ExcerptTagPickerBottomSheet(
            tagGroups = tagGroups,
            selectedTagNames = selectedTagNames,
            onDismiss = onDismissTagPicker,
            onToggleTag = onToggleTag,
        )
    }

    if (isMountPickerOpen) {
        ExcerptMountNodeBottomSheet(
            breadcrumbs = breadcrumbs,
            destinations = destinations,
            currentNodeId = mountPickerCurrentNodeId,
            selectedTargetNodeId = archiveNodeId,
            onDismiss = onDismissMountPicker,
            onConfirm = onConfirmMountPicker,
            onNavigateToNode = onNavigateMountPicker,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExcerptMetadataSection(
    archiveNodeId: String?,
    archiveNodeTitle: String?,
    selectedTagNames: List<String>,
    createTime: Long,
    isEditing: Boolean,
    onOpenArchiveNode: (String) -> Unit,
    onOpenTagPicker: () -> Unit,
    onOpenMountPicker: () -> Unit,
) {
    val createTimeText = remember(createTime) { formatExcerptCreateTime(createTime) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExcerptMetadataNodeBadge(
            text = archiveNodeTitle ?: stringResource(R.string.archive_picker_inbox),
            backgroundColor = if (archiveNodeTitle == null) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (archiveNodeTitle == null) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            onClick = when {
                isEditing -> onOpenMountPicker
                archiveNodeId != null -> ({ onOpenArchiveNode(archiveNodeId) })
                else -> null
            },
        )
        if (selectedTagNames.isEmpty()) {
            if (isEditing) {
                ExcerptMetadataTagLabel(
                    text = stringResource(R.string.excerpt_detail_add_tag_action),
                    onClick = onOpenTagPicker,
                )
            }
        } else {
            selectedTagNames.forEach { tagName ->
                ExcerptMetadataTagLabel(
                    text = "#$tagName",
                    onClick = if (isEditing) onOpenTagPicker else null,
                )
            }
        }
    }
    if (createTimeText.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.excerpt_detail_created_at, createTimeText),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExcerptContentSection(
    title: String,
    rawText: String,
    placeholder: String,
    isEditing: Boolean,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    containerColor: Color,
    textStyle: TextStyle,
    minLines: Int,
    onValueChange: (String) -> Unit,
    onOpenLinkedTarget: (String) -> Unit,
) {
    ExcerptSectionTitle(title = title)
    if (isEditing) {
        InlineLinkEditor(
            rawText = rawText,
            placeholder = placeholder,
            onRawTextChange = onValueChange,
            searchSuggestions = searchSuggestions,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            containerColor = containerColor,
            textStyle = textStyle,
        )
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
        ) {
            LinkAwareText(
                rawText = rawText,
                onLinkClick = onOpenLinkedTarget,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun ExcerptThoughtSection(
    thought: String,
    isEditing: Boolean,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onThoughtChange: (String) -> Unit,
    onOpenLinkedTarget: (String) -> Unit,
) {
    val containerColor = if (isEditing) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f)
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)

    ExcerptSectionTitle(title = stringResource(R.string.excerpt_detail_thought_title))
    if (isEditing) {
        InlineLinkEditor(
            rawText = thought,
            placeholder = stringResource(R.string.excerpt_detail_thought_placeholder),
            onRawTextChange = onThoughtChange,
            searchSuggestions = searchSuggestions,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            containerColor = containerColor,
            textStyle = textStyle,
        )
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
        ) {
            if (thought.isBlank()) {
                Text(
                    text = stringResource(R.string.excerpt_detail_thought_empty),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LinkAwareText(
                    rawText = thought,
                    onLinkClick = onOpenLinkedTarget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    textStyle = textStyle,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExcerptSourceSection(
    sourceTitle: String,
    url: String,
    isEditing: Boolean,
    onSourceTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val fieldColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f)

    ExcerptSectionTitle(title = stringResource(R.string.excerpt_detail_source_section_title))
    if (isEditing) {
        Text(
            text = stringResource(R.string.excerpt_detail_source_name_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        DetailBorderlessTextField(
            value = sourceTitle,
            placeholder = stringResource(R.string.excerpt_detail_source_name_placeholder),
            containerColor = fieldColor,
            onValueChange = onSourceTitleChange,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.excerpt_detail_source_url_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        DetailBorderlessTextField(
            value = url,
            placeholder = stringResource(R.string.excerpt_detail_source_url_placeholder),
            containerColor = fieldColor,
            singleLine = true,
            onValueChange = onUrlChange,
        )
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = fieldColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sourceTitle.isNotBlank()) {
                    Text(
                        text = sourceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (url.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching { uriHandler.openUri(url) }
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = url,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBorderlessTextField(
    value: String,
    placeholder: String,
    containerColor: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ExcerptSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ExcerptMetadataNodeBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(10.dp)
    val containerModifier = Modifier
        .clip(shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        )
        .background(backgroundColor)
        .padding(horizontal = 8.dp, vertical = 4.dp)

    Text(
        text = text,
        modifier = containerModifier,
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ExcerptMetadataTagLabel(
    text: String,
    onClick: (() -> Unit)?,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun excerptDetailTopBarColors() = TopAppBarDefaults.centerAlignedTopAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
)

private fun formatExcerptCreateTime(createTime: Long): String {
    if (createTime <= 0L) return ""
    return runCatching {
        Instant.ofEpochMilli(createTime)
            .atZone(ZoneId.systemDefault())
            .format(ExcerptCreateTimeFormatter)
    }.getOrDefault("")
}

private val ExcerptCreateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Preview(showBackground = true)
@Composable
private fun ExcerptDetailScreenPreview() {
    val snapshot = WorkspacePreviewData.snapshot()
    val excerpt = snapshot.excerptsById.getValue("excerpt-1")

    GleanReadTheme {
        ExcerptDetailScreen(
            excerpt = excerpt,
            content = excerpt.content,
            thought = excerpt.thought,
            sourceTitle = excerpt.sourceTitle.orEmpty(),
            url = excerpt.url.orEmpty(),
            selectedTagNames = excerpt.tags,
            archiveNodeId = excerpt.archivedNodeId,
            archiveNodeTitle = excerpt.archivedNodeTitle,
            tagGroups = snapshot.tagGroups,
            breadcrumbs = listOf(
                KnowledgeTreeBreadcrumbUiModel(
                    title = "知识体系",
                    nodeId = null,
                ),
                KnowledgeTreeBreadcrumbUiModel(
                    title = "Compose Architecture",
                    nodeId = "node-1",
                ),
            ),
            destinations = emptyList(),
            isEditing = false,
            canSave = false,
            isTagPickerOpen = false,
            isMountPickerOpen = false,
            mountPickerCurrentNodeId = excerpt.archivedNodeId,
            searchSuggestions = { emptyList() },
            onBack = {},
            onCloseEditing = {},
            onStartEditing = {},
            onSave = {},
            onContentChange = {},
            onThoughtChange = {},
            onSourceTitleChange = {},
            onUrlChange = {},
            onOpenArchiveNode = {},
            onOpenLinkedTarget = {},
            onOpenTagPicker = {},
            onDismissTagPicker = {},
            onToggleTag = {},
            onOpenMountPicker = {},
            onDismissMountPicker = {},
            onNavigateMountPicker = {},
            onConfirmMountPicker = {},
        )
    }
}
