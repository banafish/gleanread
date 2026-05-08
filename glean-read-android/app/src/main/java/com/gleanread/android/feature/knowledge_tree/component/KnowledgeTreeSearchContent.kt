package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreePathText

@Composable
fun KnowledgeTreeSearchContent(
    modifier: Modifier = Modifier,
    snapshot: WorkspaceSnapshot,
    query: String,
    recentQueries: List<String>,
    rootTitle: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onOpenNode: (String) -> Unit,
) {
    val trimmedQuery = query.trim()
    val nodeResults = remember(snapshot, trimmedQuery, rootTitle) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            snapshot.flatNodes.values.filter { node ->
                node.title.contains(trimmedQuery, ignoreCase = true) ||
                    buildKnowledgeTreePathText(snapshot, node.id, rootTitle)
                        .contains(trimmedQuery, ignoreCase = true)
            }.take(8)
        }
    }
    val excerptResults = remember(snapshot, trimmedQuery) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            snapshot.excerpts.filter { excerpt ->
                excerpt.content.contains(trimmedQuery, ignoreCase = true) ||
                    excerpt.thought.contains(trimmedQuery, ignoreCase = true)
            }.take(8)
        }
    }
    val recentSearchTitle = stringResource(R.string.knowledge_tree_recent_search)
    val nodeResultsTitle = stringResource(R.string.knowledge_tree_node_results)
    val excerptResultsTitle = stringResource(R.string.knowledge_tree_search_results)
    val nodeTypeLabel = stringResource(R.string.knowledge_tree_result_node)
    val excerptTypeLabel = stringResource(R.string.knowledge_tree_result_excerpt)
    val inboxLabel = stringResource(R.string.knowledge_tree_inbox)

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (recentQueries.isNotEmpty()) {
                item {
                    SearchSectionTitle(recentSearchTitle)
                }
                items(recentQueries, key = { it }) { recentQuery ->
                    Text(
                        text = recentQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onQueryChange(recentQuery)
                                onSearchSubmit(recentQuery)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            if (nodeResults.isNotEmpty()) {
                item {
                    SearchSectionTitle(nodeResultsTitle)
                }
                items(nodeResults, key = { it.id }) { node ->
                    SearchResultCard(
                        title = node.title,
                        subtitle = buildKnowledgeTreePathText(snapshot, node.id, rootTitle),
                        typeLabel = nodeTypeLabel,
                        onClick = {
                            onSearchSubmit(trimmedQuery)
                            onOpenNode(node.id)
                        },
                    )
                }
            }
            if (excerptResults.isNotEmpty()) {
                item {
                    SearchSectionTitle(excerptResultsTitle)
                }
                items(excerptResults, key = { it.id }) { excerpt ->
                    val subtitle = if (excerpt.archivedNodeId != null) {
                        stringResource(
                            R.string.knowledge_tree_result_location,
                            buildKnowledgeTreePathText(snapshot, excerpt.archivedNodeId, rootTitle),
                        )
                    } else {
                        stringResource(R.string.knowledge_tree_result_location, inboxLabel)
                    }

                    SearchResultCard(
                        title = excerpt.content,
                        subtitle = subtitle,
                        typeLabel = excerptTypeLabel,
                        onClick = {
                            onSearchSubmit(trimmedQuery)
                            excerpt.archivedNodeId?.let(onOpenNode)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(
    title: String,
) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SearchResultCard(
    title: String,
    subtitle: String,
    typeLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = typeLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
