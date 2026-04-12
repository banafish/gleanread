package com.gleanread.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URL

@Composable
fun RichExcerptCard(
    content: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.surfaceContainer
    val quoteTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = quoteTint,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content,
                fontSize = 15.sp,
                color = textColor,
                lineHeight = 22.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            SourceBadge(url = url)
        }
    }
}

@Composable
fun TagPill(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun CaptureBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetColor = MaterialTheme.colorScheme.surfaceContainerLow
    val handleColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = sheetColor,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SourceBadge(url: String) {
    val tintColor = MaterialTheme.colorScheme.onSurfaceVariant

    val host = remember(url) {
        try { URL(url).host.ifEmpty { url } } catch (e: Exception) { url }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Link,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(12.dp).rotate(-45f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = host.ifEmpty { "暂无来源链接" },
            fontSize = 12.sp,
            color = tintColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}