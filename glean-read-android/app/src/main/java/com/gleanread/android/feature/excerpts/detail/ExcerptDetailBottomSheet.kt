package com.gleanread.android.feature.excerpts.detail

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.core.model.TagGroupUiModel
import com.gleanread.android.core.ui.component.CaptureBottomSheet
import com.gleanread.android.feature.knowledge_tree.component.KnowledgeTreeNodePickerBottomSheet
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeBreadcrumbUiModel
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeNodePickerDestinationUiModel

@Composable
fun ExcerptMountNodeBottomSheet(
    breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    destinations: List<KnowledgeTreeNodePickerDestinationUiModel>,
    currentNodeId: String?,
    selectedTargetNodeId: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToNode: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirmEnabled = currentNodeId != selectedTargetNodeId

    KnowledgeTreeNodePickerBottomSheet(
        title = stringResource(R.string.excerpt_detail_edit_location_title),
        breadcrumbs = breadcrumbs,
        destinations = destinations,
        helperText = stringResource(
            when {
                currentNodeId == selectedTargetNodeId -> R.string.excerpt_detail_edit_location_already_here
                currentNodeId == null -> R.string.excerpt_detail_edit_location_root_hint
                else -> R.string.excerpt_detail_edit_location_confirm_hint
            },
        ),
        emptyListText = stringResource(R.string.excerpt_detail_edit_location_empty),
        confirmEnabled = confirmEnabled,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onNavigateToNode = onNavigateToNode,
        modifier = modifier,
        headlineContent = {
            Card(
                onClick = { onNavigateToNode(null) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentNodeId == null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (currentNodeId == null) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.archive_picker_inbox),
                            color = if (currentNodeId == null) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.excerpt_detail_edit_location_inbox_hint),
                            color = if (currentNodeId == null) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (currentNodeId == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExcerptTagPickerBottomSheet(
    tagGroups: List<TagGroupUiModel>,
    selectedTagNames: List<String>,
    onDismiss: () -> Unit,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTagGroups = remember(tagGroups, searchQuery) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isEmpty()) {
            tagGroups
        } else {
            tagGroups.mapNotNull { group ->
                val filteredItems = group.items.filter { tag ->
                    tag.fullName.contains(trimmedQuery, ignoreCase = true) ||
                        tag.displayName.contains(trimmedQuery, ignoreCase = true) ||
                        tag.folder.contains(trimmedQuery, ignoreCase = true)
                }
                if (filteredItems.isEmpty()) {
                    null
                } else {
                    group.copy(items = filteredItems)
                }
            }
        }
    }

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.excerpt_detail_edit_tags),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(R.string.excerpt_detail_tags_search_placeholder))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.excerpt_detail_tags_search_placeholder),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.height(16.dp))
            if (selectedTagNames.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.excerpt_detail_tags_selected_title, selectedTagNames.size),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedTagNames.forEach { tagName ->
                            ExcerptTagPickerChip(
                                text = "#$tagName",
                                selected = true,
                                onClick = { onToggleTag(tagName) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (filteredTagGroups.isEmpty()) {
                Text(
                    text = stringResource(R.string.excerpt_detail_tags_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filteredTagGroups, key = { it.folder }) { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = group.folder,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                group.items.forEach { tag ->
                                    ExcerptTagPickerChip(
                                        text = "#${tag.displayName}",
                                        selected = selectedTagNames.contains(tag.fullName),
                                        onClick = { onToggleTag(tag.fullName) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExcerptTagPickerChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor,
                )
            }
        }
    }
}
