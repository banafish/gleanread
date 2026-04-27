package com.gleanread.android.feature.excerpts.feed

import android.net.Uri
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.core.model.ExcerptUiModel
import com.gleanread.android.core.ui.richtext.LinkAwareText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExcerptCard(
    excerpt: ExcerptUiModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isActionsRevealed: Boolean,
    onRevealActions: () -> Unit,
    onDismissActions: () -> Unit,
    onOpenAiSummary: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenExcerpt: (String) -> Unit,
) {
    if (isSelectionMode) {
        ExcerptCardSurface(
            excerpt = excerpt,
            isSelectionMode = true,
            isSelected = isSelected,
            onLongPress = onLongPress,
            onClick = onClick,
            onOpenNode = onOpenNode,
            onOpenExcerpt = onOpenExcerpt,
        )
        return
    }

    val density = LocalDensity.current
    val actionAreaWidthPx = with(density) { EXCERPT_CARD_ACTION_AREA_WIDTH.toPx() }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val coroutineScope = rememberCoroutineScope()
    var swipeOffsetPx by remember { mutableFloatStateOf(0f) }
    val latestOnRevealActions = rememberUpdatedState(onRevealActions)
    val latestOnDismissActions = rememberUpdatedState(onDismissActions)

    suspend fun animateSwipeTo(targetOffset: Float) {
        animate(
            initialValue = swipeOffsetPx,
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = EXCERPT_CARD_SWIPE_ANIMATION_MILLIS),
        ) { value, _ ->
            swipeOffsetPx = value.coerceIn(-actionAreaWidthPx, 0f)
        }
    }

    fun settleSwipe(shouldReveal: Boolean) {
        coroutineScope.launch {
            animateSwipeTo(if (shouldReveal) -actionAreaWidthPx else 0f)
            if (shouldReveal) {
                latestOnRevealActions.value()
            } else {
                latestOnDismissActions.value()
            }
        }
    }

    LaunchedEffect(actionAreaWidthPx) {
        swipeOffsetPx = swipeOffsetPx.coerceIn(
            minimumValue = -actionAreaWidthPx,
            maximumValue = 0f,
        )
    }

    LaunchedEffect(isActionsRevealed, actionAreaWidthPx) {
        val targetOffset = if (isActionsRevealed) {
            -actionAreaWidthPx
        } else {
            0f
        }
        if (abs(swipeOffsetPx - targetOffset) > EXCERPT_CARD_SWIPE_OFFSET_EPSILON) {
            animateSwipeTo(targetOffset)
        }
    }

    fun closeActions() {
        coroutineScope.launch {
            animateSwipeTo(0f)
            latestOnDismissActions.value()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        ExcerptCardActions(
            onOpenAiSummary = onOpenAiSummary,
            onDelete = onDelete,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(swipeOffsetPx.roundToInt(), 0)
                }
                .pointerInput(actionAreaWidthPx, touchSlop) {
                    detectExcerptCardSwipe(
                        actionAreaWidthPx = actionAreaWidthPx,
                        touchSlop = touchSlop,
                        currentSwipeOffset = { swipeOffsetPx },
                        onSwipeOffsetChange = { swipeOffsetPx = it },
                        onSettle = ::settleSwipe,
                    )
                },
        ) {
            ExcerptCardSurface(
                excerpt = excerpt,
                isSelectionMode = false,
                isSelected = isSelected,
                onLongPress = {
                    if (swipeOffsetPx < -EXCERPT_CARD_SWIPE_OFFSET_EPSILON) {
                        closeActions()
                    } else {
                        onLongPress()
                    }
                },
                onClick = {
                    if (swipeOffsetPx < -EXCERPT_CARD_SWIPE_OFFSET_EPSILON) {
                        closeActions()
                    } else {
                        onClick()
                    }
                },
                onOpenNode = onOpenNode,
                onOpenExcerpt = onOpenExcerpt,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExcerptCardSurface(
    excerpt: ExcerptUiModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    onOpenNode: (String) -> Unit,
    onOpenExcerpt: (String) -> Unit,
) {
    val sourceLabel = remember(excerpt.url, excerpt.sourceTitle) {
        excerptSourceLabel(
            url = excerpt.url,
            sourceTitle = excerpt.sourceTitle,
        )
    }
    val justNowLabel = stringResource(R.string.feed_time_just_now)
    val todayLabel = stringResource(R.string.feed_time_today)
    val yesterdayLabel = stringResource(R.string.feed_time_yesterday)
    val dayBeforeYesterdayLabel = stringResource(R.string.feed_time_day_before_yesterday)
    val createTimeLabel = remember(
        excerpt.createTime,
        justNowLabel,
        todayLabel,
        yesterdayLabel,
        dayBeforeYesterdayLabel,
    ) {
        excerptCreateTimeLabel(
            createTime = excerpt.createTime,
            justNowLabel = justNowLabel,
            todayLabel = todayLabel,
            yesterdayLabel = yesterdayLabel,
            dayBeforeYesterdayLabel = dayBeforeYesterdayLabel,
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = EXCERPT_CARD_ELEVATION,
                shape = EXCERPT_CARD_SHAPE,
                clip = false,
            )
            .clip(EXCERPT_CARD_SHAPE)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = EXCERPT_CARD_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (excerpt.archivedNodeTitle == null) {
                        StatusBadge(
                            text = stringResource(R.string.feed_status_inbox),
                            bg = MaterialTheme.colorScheme.errorContainer,
                            fg = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        StatusBadge(
                            text = excerpt.archivedNodeTitle,
                            bg = MaterialTheme.colorScheme.secondaryContainer,
                            fg = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    excerpt.tags.take(3).forEach { tag ->
                        Text(
                            text = "#$tag",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = if (isSelected) {
                            stringResource(R.string.feed_selection_selected)
                        } else {
                            stringResource(R.string.feed_selection_unselected)
                        },
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ExcerptCardPreviewText(
                rawText = excerpt.content,
                onLinkClick = { targetId ->
                    if (targetId != excerpt.id) {
                        if (targetId.startsWith("excerpt-")) {
                            onOpenExcerpt(targetId)
                        } else {
                            onOpenNode(targetId)
                        }
                    }
                },
                onClick = onClick,
                onLongClick = onLongPress,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (excerpt.thought.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        ExcerptCardPreviewText(
                            rawText = excerpt.thought,
                            onLinkClick = { targetId ->
                                if (targetId != excerpt.id) {
                                    if (targetId.startsWith("excerpt-")) {
                                        onOpenExcerpt(targetId)
                                    } else {
                                        onOpenNode(targetId)
                                    }
                                }
                            },
                            onClick = onClick,
                            onLongClick = onLongPress,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (sourceLabel != null || createTimeLabel.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sourceLabel != null) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = sourceLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (createTimeLabel.isNotBlank()) {
                        Text(
                            text = createTimeLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun PointerInputScope.detectExcerptCardSwipe(
    actionAreaWidthPx: Float,
    touchSlop: Float,
    currentSwipeOffset: () -> Float,
    onSwipeOffsetChange: (Float) -> Unit,
    onSettle: (Boolean) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var horizontalDrag = 0f
        var verticalDrag = 0f
        var isDraggingHorizontally = false

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break

            if (change.changedToUpIgnoreConsumed()) {
                if (isDraggingHorizontally) {
                    val shouldReveal = currentSwipeOffset() <=
                        -actionAreaWidthPx * EXCERPT_CARD_REVEAL_POSITIONAL_THRESHOLD_FRACTION
                    onSettle(shouldReveal)
                }
                break
            }

            val delta = change.positionChange()
            if (!isDraggingHorizontally) {
                horizontalDrag += delta.x
                verticalDrag += delta.y

                val absoluteHorizontal = abs(horizontalDrag)
                val absoluteVertical = abs(verticalDrag)
                val currentOffset = currentSwipeOffset()
                val canOpen = horizontalDrag < 0f && currentOffset > -actionAreaWidthPx
                val canClose = horizontalDrag > 0f && currentOffset < 0f
                val hasHorizontalIntent = (canOpen || canClose) &&
                    absoluteHorizontal > touchSlop &&
                    absoluteHorizontal > absoluteVertical * EXCERPT_CARD_HORIZONTAL_INTENT_RATIO
                val hasVerticalIntent = absoluteVertical > touchSlop &&
                    absoluteVertical > absoluteHorizontal * EXCERPT_CARD_VERTICAL_INTENT_RATIO

                when {
                    hasVerticalIntent -> break
                    hasHorizontalIntent -> {
                        isDraggingHorizontally = true
                        val slopOffset = if (horizontalDrag < 0f) -touchSlop else touchSlop
                        onSwipeOffsetChange(
                            (currentOffset + horizontalDrag - slopOffset)
                                .coerceIn(-actionAreaWidthPx, 0f),
                        )
                        change.consume()
                    }
                }
            } else {
                onSwipeOffsetChange(
                    (currentSwipeOffset() + delta.x).coerceIn(-actionAreaWidthPx, 0f),
                )
                change.consume()
            }
        }
    }
}

@Composable
private fun ExcerptCardPreviewText(
    rawText: String,
    onLinkClick: (String) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    maxLines: Int,
    overflow: TextOverflow,
) {
    if (rawText.contains(INLINE_LINK_MARKER)) {
        LinkAwareText(
            rawText = rawText,
            onLinkClick = onLinkClick,
            onClick = onClick,
            onLongClick = onLongClick,
            maxLines = maxLines,
            overflow = overflow,
        )
    } else {
        Text(
            text = rawText,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

@Composable
private fun ExcerptCardActions(
    onOpenAiSummary: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExcerptCardActionButton(
            contentDescription = stringResource(R.string.feed_mount_excerpt_action),
            icon = Icons.Default.AutoAwesome,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenAiSummary,
        )
        Spacer(Modifier.width(8.dp))
        ExcerptCardActionButton(
            contentDescription = stringResource(R.string.knowledge_tree_delete_action),
            icon = Icons.Default.Delete,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            onClick = onDelete,
        )
    }
}

@Composable
private fun ExcerptCardActionButton(
    contentDescription: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.width(EXCERPT_CARD_ACTION_BUTTON_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(EXCERPT_CARD_ACTION_BUTTON_SIZE)
                .clip(CircleShape)
                .background(containerColor),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(EXCERPT_CARD_ACTION_ICON_SIZE),
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    bg: Color,
    fg: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun excerptSourceLabel(
    url: String?,
    sourceTitle: String?,
): String? {
    return url.toDomainLabel()
        ?: sourceTitle?.takeIf { it.isNotBlank() }
}

private fun String?.toDomainLabel(): String? {
    val rawValue = this?.trim().orEmpty()
    if (rawValue.isBlank()) return null
    val host = runCatching { Uri.parse(rawValue).host }
        .getOrNull()
        ?: runCatching { Uri.parse("https://$rawValue").host }.getOrNull()
    return host
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
}

private fun excerptCreateTimeLabel(
    createTime: Long,
    justNowLabel: String,
    todayLabel: String,
    yesterdayLabel: String,
    dayBeforeYesterdayLabel: String,
): String {
    if (createTime <= 0L) return ""

    val nowInstant = Instant.now()
    if (createTime >= nowInstant.toEpochMilli() - ONE_HOUR_MILLIS) {
        return justNowLabel
    }

    val zoneId = ZoneId.systemDefault()
    val createdDate = runCatching {
        Instant.ofEpochMilli(createTime)
            .atZone(zoneId)
            .toLocalDate()
    }.getOrNull() ?: return ""
    val today = LocalDate.now(zoneId)
    val daysBetween = ChronoUnit.DAYS.between(createdDate, today)

    return when (daysBetween) {
        0L -> todayLabel
        1L -> yesterdayLabel
        2L -> dayBeforeYesterdayLabel
        else -> {
            if (createdDate.year == today.year) {
                "${createdDate.monthValue}月${createdDate.dayOfMonth}日"
            } else {
                "${createdDate.year}年${createdDate.monthValue}月${createdDate.dayOfMonth}日"
            }
        }
    }
}

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
private val EXCERPT_CARD_SHAPE = RoundedCornerShape(32.dp)
private val EXCERPT_CARD_ELEVATION = 2.dp
private val EXCERPT_CARD_ACTION_BUTTON_SIZE = 50.dp
private val EXCERPT_CARD_ACTION_ICON_SIZE = 26.dp
private val EXCERPT_CARD_ACTION_AREA_WIDTH = 128.dp
private const val EXCERPT_CARD_REVEAL_POSITIONAL_THRESHOLD_FRACTION = 0.62f
private const val EXCERPT_CARD_HORIZONTAL_INTENT_RATIO = 1.45f
private const val EXCERPT_CARD_VERTICAL_INTENT_RATIO = 0.85f
private const val EXCERPT_CARD_SWIPE_ANIMATION_MILLIS = 220
private const val EXCERPT_CARD_SWIPE_OFFSET_EPSILON = 0.5f
private const val INLINE_LINK_MARKER = "[["
