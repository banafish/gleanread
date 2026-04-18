package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun KnowledgeTreeLeadingIndicator(
    showActionIcon: Boolean,
    isExpanded: Boolean,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showActionIcon && onClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = contentDescription,
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun KnowledgeTreeNodeTitle(
    title: String,
    onClick: () -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = title,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = style,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
