@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.gleanread.android.feature.tags

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.core.model.TagGroupUiModel
import com.gleanread.android.core.model.TagUiModel
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.core.ui.sync.WorkspacePullToRefreshBox
import com.gleanread.android.core.ui.theme.GleanReadTheme
import com.gleanread.android.feature.tags.component.DeleteTagsDialog
import com.gleanread.android.feature.tags.component.TagsSearchTopBar
import com.gleanread.android.feature.tags.component.TagsTopBar

private const val CollapsedTagRowCount = 5
private val TagGroupShape = RoundedCornerShape(24.dp)
private val TagChipShape = RoundedCornerShape(16.dp)
private val TagChipSpacing = 8.dp
private val TagChipHorizontalPadding = 12.dp
private val TagChipVerticalPadding = 8.dp
private val TagChipCheckIconSize = 14.dp
private val TagChipCheckSpacing = 6.dp
private val TagsContentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 120.dp)

@Composable
fun TagsScreen(
    tagGroups: List<TagGroupUiModel>,
    isSearchVisible: Boolean,
    searchQuery: String,
    selectedTagIds: Set<String>,
    pendingDeleteTags: List<TagUiModel>,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLongPressTag: (String) -> Unit,
    onToggleTagSelection: (String) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onConfirmDeleteDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectionMode = selectedTagIds.isNotEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (isSearchVisible) {
                TagsSearchTopBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = onToggleSearch,
                )
            } else {
                TagsTopBar(onToggleSearch = onToggleSearch)
            }
        },
    ) { innerPadding ->
        WorkspacePullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (tagGroups.isEmpty() && searchQuery.isNotBlank()) {
                TagsSearchEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = TagsContentPadding,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(tagGroups, key = { it.folder }) { group ->
                        TagGroupCard(
                            group = group,
                            selectedTagIds = selectedTagIds,
                            isSelectionMode = isSelectionMode,
                            onLongPressTag = onLongPressTag,
                            onToggleTagSelection = onToggleTagSelection,
                        )
                    }
                }
            }
        }
    }

    if (pendingDeleteTags.isNotEmpty()) {
        DeleteTagsDialog(
            tags = pendingDeleteTags,
            onDismiss = onDismissDeleteDialog,
            onConfirm = onConfirmDeleteDialog,
        )
    }
}

@Composable
private fun TagsSearchEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.tags_search_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TagGroupCard(
    group: TagGroupUiModel,
    selectedTagIds: Set<String>,
    isSelectionMode: Boolean,
    onLongPressTag: (String) -> Unit,
    onToggleTagSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(group.folder) { mutableStateOf(false) }
    val chipTextStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = TagGroupShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${group.folder} (${group.count})",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(10.dp))
            TagChipRows(
                tags = group.items,
                selectedTagIds = selectedTagIds,
                chipTextStyle = chipTextStyle,
                expanded = expanded,
                isSelectionMode = isSelectionMode,
                onExpandedChange = { expanded = it },
                onLongPressTag = onLongPressTag,
                onToggleTagSelection = onToggleTagSelection,
            )
        }
    }
}

@Composable
private fun TagChipRows(
    tags: List<TagUiModel>,
    selectedTagIds: Set<String>,
    chipTextStyle: TextStyle,
    expanded: Boolean,
    isSelectionMode: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLongPressTag: (String) -> Unit,
    onToggleTagSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxChipWidth = maxWidth
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val chipHorizontalPaddingPx = with(density) { TagChipHorizontalPadding.roundToPx() }
        val chipSpacingPx = with(density) { TagChipSpacing.roundToPx() }
        val chipCheckContentWidthPx = with(density) {
            (TagChipCheckIconSize + TagChipCheckSpacing).roundToPx()
        }
        val tagRows = remember(
            tags,
            selectedTagIds,
            chipTextStyle,
            textMeasurer,
            maxWidthPx,
            chipHorizontalPaddingPx,
            chipSpacingPx,
            chipCheckContentWidthPx,
        ) {
            buildTagChipRows(
                tags = tags,
                selectedTagIds = selectedTagIds,
                maxWidthPx = maxWidthPx,
                textMeasurer = textMeasurer,
                chipTextStyle = chipTextStyle,
                chipHorizontalPaddingPx = chipHorizontalPaddingPx,
                chipSpacingPx = chipSpacingPx,
                chipSelectedContentWidthPx = chipCheckContentWidthPx,
            )
        }
        val hasOverflow = tagRows.size > CollapsedTagRowCount
        val visibleRows = if (expanded || !hasOverflow) tagRows else tagRows.take(CollapsedTagRowCount)

        Column(verticalArrangement = Arrangement.spacedBy(TagChipSpacing)) {
            visibleRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TagChipSpacing),
                ) {
                    row.forEach { tag ->
                        TagChip(
                            tag = tag,
                            chipTextStyle = chipTextStyle,
                            maxWidth = maxChipWidth,
                            isSelected = selectedTagIds.contains(tag.id),
                            isSelectionMode = isSelectionMode,
                            onLongPress = { onLongPressTag(tag.id) },
                            onClick = { onToggleTagSelection(tag.id) },
                        )
                    }
                }
            }
            if (hasOverflow) {
                TextButton(
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            if (expanded) R.string.tags_collapse else R.string.tags_expand_more,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: TagUiModel,
    chipTextStyle: TextStyle,
    maxWidth: Dp,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clip(TagChipShape)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onClick()
                    }
                },
                onLongClick = onLongPress,
            ),
        shape = TagChipShape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TagChipHorizontalPadding,
                vertical = TagChipVerticalPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tagChipLabel(tag),
                color = contentColor,
                style = chipTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelected) {
                Spacer(Modifier.width(TagChipCheckSpacing))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(TagChipCheckIconSize),
                    tint = contentColor,
                )
            }
        }
    }
}

private fun buildTagChipRows(
    tags: List<TagUiModel>,
    selectedTagIds: Set<String>,
    maxWidthPx: Int,
    textMeasurer: TextMeasurer,
    chipTextStyle: TextStyle,
    chipHorizontalPaddingPx: Int,
    chipSpacingPx: Int,
    chipSelectedContentWidthPx: Int,
): List<List<TagUiModel>> {
    if (tags.isEmpty()) return emptyList()
    if (maxWidthPx <= 0) return listOf(tags)

    val rows = mutableListOf<MutableList<TagUiModel>>()
    var currentRow = mutableListOf<TagUiModel>()
    var currentWidth = 0

    tags.forEach { tag ->
        val chipWidth = measureTagChipWidth(
            tag = tag,
            isSelected = selectedTagIds.contains(tag.id),
            textMeasurer = textMeasurer,
            chipTextStyle = chipTextStyle,
            chipHorizontalPaddingPx = chipHorizontalPaddingPx,
            chipSelectedContentWidthPx = chipSelectedContentWidthPx,
        ).coerceAtMost(maxWidthPx)

        if (currentRow.isNotEmpty() && currentWidth + chipSpacingPx + chipWidth > maxWidthPx) {
            rows += currentRow
            currentRow = mutableListOf()
            currentWidth = 0
        }

        if (currentRow.isNotEmpty()) {
            currentWidth += chipSpacingPx
        }
        currentRow += tag
        currentWidth += chipWidth
    }

    if (currentRow.isNotEmpty()) {
        rows += currentRow
    }

    return rows
}

private fun measureTagChipWidth(
    tag: TagUiModel,
    isSelected: Boolean,
    textMeasurer: TextMeasurer,
    chipTextStyle: TextStyle,
    chipHorizontalPaddingPx: Int,
    chipSelectedContentWidthPx: Int,
): Int {
    val textWidth = textMeasurer.measure(
        text = AnnotatedString(tagChipLabel(tag)),
        style = chipTextStyle,
        maxLines = 1,
    ).size.width
    return textWidth +
        chipHorizontalPaddingPx * 2 +
        if (isSelected) chipSelectedContentWidthPx else 0
}

private fun tagChipLabel(tag: TagUiModel): String = "#${tag.displayName} ${tag.heatWeight}"

@Preview(showBackground = true)
@Composable
private fun TagsScreenPreview() {
    GleanReadTheme {
        TagsScreen(
            tagGroups = WorkspacePreviewData.snapshot().tagGroups,
            isSearchVisible = false,
            searchQuery = "",
            selectedTagIds = emptySet(),
            pendingDeleteTags = emptyList(),
            onToggleSearch = {},
            onSearchQueryChange = {},
            onLongPressTag = {},
            onToggleTagSelection = {},
            onDismissDeleteDialog = {},
            isRefreshing = false,
            onRefresh = {},
            onConfirmDeleteDialog = {},
        )
    }
}
