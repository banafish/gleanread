package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gleanread.android.feature.knowledge_tree.model.KnowledgeTreeBreadcrumbUiModel

@Composable
fun BreadcrumbBar(
    breadcrumbs: List<KnowledgeTreeBreadcrumbUiModel>,
    onNavigateToBreadcrumb: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val root = breadcrumbs.firstOrNull() ?: return
    val remaining = breadcrumbs.drop(1)
    val remainingScrollState = rememberScrollState()

    LaunchedEffect(remaining.size, remainingScrollState.maxValue) {
        remainingScrollState.scrollTo(remainingScrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CompactBreadcrumbButton(
            crumb = root,
            enabled = remaining.isNotEmpty(),
            onNavigateToBreadcrumb = onNavigateToBreadcrumb,
        )
        if (remaining.isNotEmpty()) {
            CompactBreadcrumbChevron()
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(remainingScrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    remaining.forEachIndexed { index, crumb ->
                        val isLast = index == remaining.lastIndex
                        CompactBreadcrumbButton(
                            crumb = crumb,
                            enabled = !isLast,
                            onNavigateToBreadcrumb = onNavigateToBreadcrumb,
                        )
                        if (!isLast) {
                            CompactBreadcrumbChevron()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBreadcrumbButton(
    crumb: KnowledgeTreeBreadcrumbUiModel,
    enabled: Boolean,
    onNavigateToBreadcrumb: (String?) -> Unit,
) {
    Text(
        text = crumb.title,
        modifier = Modifier
            .widthIn(max = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onNavigateToBreadcrumb(crumb.nodeId) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
}

@Composable
private fun CompactBreadcrumbChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
    )
}
