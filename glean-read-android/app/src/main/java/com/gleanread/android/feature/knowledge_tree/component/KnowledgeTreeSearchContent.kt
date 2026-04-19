package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gleanread.android.data.model.ExcerptUiModel
import com.gleanread.android.data.model.FlatNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.feature.knowledge_tree.model.buildKnowledgeTreePathText

@Composable
fun KnowledgeTreeSearchContent(
    modifier: Modifier = Modifier,
    snapshot: WorkspaceSnapshot,
    query: String,
    recentQueries: List<String>,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onOpenNode: (String) -> Unit,
) {
    val trimmedQuery = query.trim()
    val nodeResults = remember(snapshot, trimmedQuery) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            snapshot.flatNodes.values.filter { node ->
                node.title.contains(trimmedQuery, ignoreCase = true) ||
                    buildKnowledgeTreePathText(snapshot, node.id).contains(trimmedQuery, ignoreCase = true)
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

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (recentQueries.isNotEmpty()) {
                item {
                    SearchSectionTitle("最近搜索")
                }
                items(recentQueries) { recentQuery ->
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
                    SearchSectionTitle("节点结果")
                }
                items(nodeResults, key = { it.id }) { node ->
                    SearchResultCard(
                        title = node.title,
                        subtitle = buildKnowledgeTreePathText(snapshot, node.id),
                        typeLabel = "节点",
                        onClick = {
                            onSearchSubmit(trimmedQuery)
                            onOpenNode(node.id)
                        },
                    )
                }
            }
            if (excerptResults.isNotEmpty()) {
                item {
                    SearchSectionTitle("内容结果")
                }
                items(excerptResults, key = { it.id }) { excerpt ->
                    SearchResultCard(
                        title = excerpt.content,
                        subtitle = excerpt.archivedNodeId?.let { nodeId ->
                            "位于：${buildKnowledgeTreePathText(snapshot, nodeId)}"
                        } ?: "位于：Inbox",
                        typeLabel = "摘录",
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
