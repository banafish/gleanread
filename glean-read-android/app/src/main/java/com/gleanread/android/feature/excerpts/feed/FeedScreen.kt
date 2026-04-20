@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.feature.excerpts.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.model.WorkspacePreviewData
import com.gleanread.android.ui.theme.GleanReadTheme

@Composable
fun FeedScreen(
    filteredExcerpts: List<ExcerptUiModel>,
    searchQuery: String,
    showInboxOnly: Boolean,
    isSelectionMode: Boolean,
    selectedExcerptIds: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleInboxFilter: () -> Unit,
    onOpenAiSummary: () -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onOpenNode: (String) -> Unit,
    onPreviewExcerpt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item(key = "feed_search") {
            FeedSearchBar(
                searchQuery = searchQuery,
                showInboxOnly = showInboxOnly,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onToggleInboxFilter = onToggleInboxFilter,
            )
        }

        items(items = filteredExcerpts, key = { it.id }) { excerpt ->
            ExcerptCard(
                excerpt = excerpt,
                isSelectionMode = isSelectionMode,
                isSelected = selectedExcerptIds.contains(excerpt.id),
                onLongPress = { onLongPress(excerpt.id) },
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(excerpt.id)
                    }
                },
                onOpenNode = onOpenNode,
                onPreviewExcerpt = onPreviewExcerpt,
            )
            Spacer(Modifier.height(14.dp))
        }

        item(key = "feed_ai_recommendation") {
            AiRecommendationCard(onOpenAiSummary = onOpenAiSummary)
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun FeedSearchBar(
    searchQuery: String,
    showInboxOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleInboxFilter: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.feed_search_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.feed_search_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.feed_clear_search_content_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )

        FilledTonalIconButton(
            onClick = onToggleInboxFilter,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (showInboxOnly) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (showInboxOnly) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
        ) {
            Icon(
                painter = rememberFilterPainter(showInboxOnly),
                contentDescription = stringResource(R.string.feed_filter_inbox_content_description),
                tint = Color.Unspecified,
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.feed_selection_hint),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun rememberFilterPainter(showInboxOnly: Boolean) = rememberVectorPainter(
    ImageVector.Builder(
        name = "Funnel",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(
                if (showInboxOnly) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            lineTo(14f, 13f)
            lineTo(14f, 19f)
            lineTo(10f, 21f)
            lineTo(10f, 13f)
            close()
        }
    }.build(),
)

@Composable
private fun AiRecommendationCard(
    onOpenAiSummary: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.feed_ai_recommendation_title),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.feed_ai_recommendation_title),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.feed_ai_recommendation_body),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {}) {
                    Text(stringResource(R.string.feed_ai_recommendation_ignore))
                }
                Button(onClick = onOpenAiSummary) {
                    Text(stringResource(R.string.feed_ai_recommendation_action))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    GleanReadTheme {
        FeedScreen(
            filteredExcerpts = WorkspacePreviewData.snapshot().excerpts,
            searchQuery = "",
            showInboxOnly = false,
            isSelectionMode = false,
            selectedExcerptIds = emptySet(),
            onSearchQueryChange = {},
            onClearSearch = {},
            onToggleInboxFilter = {},
            onOpenAiSummary = {},
            onLongPress = {},
            onToggleSelection = {},
            onOpenNode = {},
            onPreviewExcerpt = {},
        )
    }
}
