package com.gleanread.android.feature.capture.fast_capture

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.R
import com.gleanread.android.platform.page_context.CaptureSeed
import com.gleanread.android.platform.page_context.PageContextSupport
import com.gleanread.android.core.ui.component.CaptureBottomSheet
import com.gleanread.android.core.ui.component.ContextHintCard
import com.gleanread.android.core.ui.component.RichExcerptCard
import com.gleanread.android.feature.capture.fast_capture.component.FastCaptureLinkMenuPopup
import com.gleanread.android.feature.capture.fast_capture.component.FastCaptureTagMenuPopup

private val StringSetStateSaver = listSaver<MutableState<Set<String>>, String>(
    save = { state -> state.value.toList() },
    restore = { restored -> mutableStateOf(restored.toSet()) },
)

@Composable
fun FastCaptureScreen(
    captureSeed: CaptureSeed,
    uiState: FastCaptureUiState,
    isAccessibilityEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSaveQuickExcerpt: (thought: String, url: String, tagNames: Set<String>) -> Unit,
) {
    val availableTags = uiState.availableTags
    val isSaving = uiState.isSaving
    var thought by rememberSaveable { mutableStateOf("") }
    var selectedTags by rememberSaveable(saver = StringSetStateSaver) {
        mutableStateOf(emptySet<String>())
    }
    var currentUrl by rememberSaveable { mutableStateOf(captureSeed.url) }
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val shouldShowBackfillPrompt = captureSeed.usedCachedTitle || captureSeed.usedCachedUrl
    val shouldShowAccessibilityPrompt = !shouldShowBackfillPrompt &&
        !isAccessibilityEnabled &&
        (captureSeed.sourcePackage.isBlank() || PageContextSupport.isSupportedPackage(captureSeed.sourcePackage)) &&
        (captureSeed.url.isBlank() || captureSeed.sourceTitle.isBlank())
    val collapsedSheetHeightFraction = if (shouldShowAccessibilityPrompt) 0.64f else 0.54f
    val sheetHeightFraction by animateFloatAsState(
        targetValue = if (isImeVisible) 0.9f else collapsedSheetHeightFraction,
        animationSpec = tween(durationMillis = 220),
        label = "capture_sheet_height",
    )

    var isInputFocused by rememberSaveable { mutableStateOf(false) }
    var showTagMenu by rememberSaveable { mutableStateOf(false) }
    var tagDraftSelection by rememberSaveable(saver = StringSetStateSaver) {
        mutableStateOf(emptySet<String>())
    }
    var tagSearchQuery by rememberSaveable { mutableStateOf("") }
    var showLinkMenu by rememberSaveable { mutableStateOf(false) }
    var tempLink by rememberSaveable { mutableStateOf("") }
    val shouldCompactCaptureLayout = isImeVisible || isInputFocused

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onDismiss()
        }
    }

    val targetBackgroundColor =
        if (isInputFocused || showTagMenu || showLinkMenu) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val targetBorderColor =
        if (isInputFocused || showTagMenu || showLinkMenu) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        }

    val containerBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        label = "capture_background_color",
    )
    val containerBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        label = "capture_border_color",
    )
    val selectedTagSummary = remember(selectedTags, availableTags) {
        availableTags.firstOrNull { it in selectedTags } ?: selectedTags.firstOrNull().orEmpty()
    }

    val dismissTagMenu = {
        showTagMenu = false
        tagDraftSelection = selectedTags
        tagSearchQuery = ""
    }
    val openTagMenu = {
        tagDraftSelection = selectedTags
        tagSearchQuery = ""
        showTagMenu = true
        showLinkMenu = false
    }
    val confirmTagMenu = {
        selectedTags = tagDraftSelection
        showTagMenu = false
        tagSearchQuery = ""
    }

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(sheetHeightFraction),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.fast_capture_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp,
                            fontSize = 17.sp,
                        ),
                    )
                }

                if (!shouldCompactCaptureLayout) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        RichExcerptCard(
                            content = captureSeed.content,
                            url = currentUrl,
                            sourceTitle = captureSeed.sourceTitle,
                        )
                        if (shouldShowAccessibilityPrompt) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ContextHintCard(
                                text = stringResource(R.string.fast_capture_accessibility_hint),
                                actionLabel = stringResource(R.string.fast_capture_accessibility_action),
                                onActionClick = onOpenAccessibilitySettings,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = if (isInputFocused || showTagMenu || showLinkMenu) 12.dp else 0.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            )
                            .background(containerBackgroundColor, RoundedCornerShape(24.dp))
                            .border(1.dp, containerBorderColor, RoundedCornerShape(24.dp)),
                    ) {
                        BasicTextField(
                            value = thought,
                            onValueChange = { thought = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                                .onFocusChanged {
                                    isInputFocused = it.isFocused
                                    if (it.isFocused) {
                                        dismissTagMenu()
                                        showLinkMenu = false
                                    }
                                },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (thought.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.fast_capture_thought_placeholder),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            fontSize = 15.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val tagMenuButtonActive = selectedTags.isNotEmpty() || showTagMenu
                                val tagMenuButtonTint =
                                    if (tagMenuButtonActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        if (showTagMenu) {
                                            dismissTagMenu()
                                        } else {
                                            openTagMenu()
                                        }
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalOffer,
                                            contentDescription = stringResource(R.string.fast_capture_tag_content_description),
                                            tint = tagMenuButtonTint,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        if (selectedTags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val tagText =
                                                if (selectedTags.size == 1) {
                                                    selectedTagSummary
                                                } else {
                                                    stringResource(
                                                        R.string.fast_capture_more_tags,
                                                        selectedTagSummary,
                                                    )
                                                }
                                            Text(
                                                text = tagText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tagMenuButtonTint,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 50.dp),
                                            )
                                        }
                                    }
                                }

                                val linkMenuButtonTint =
                                    if (showLinkMenu) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        showLinkMenu = !showLinkMenu
                                        dismissTagMenu()
                                        if (showLinkMenu) {
                                            tempLink = currentUrl
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = stringResource(R.string.fast_capture_link_content_description),
                                        tint = linkMenuButtonTint,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(22.dp),
                                    )
                                }
                            }

                            val thoughtNotEmpty = thought.isNotEmpty()
                            val saveButtonBackgroundColor = MaterialTheme.colorScheme.primary
                            val saveButtonTextColor = MaterialTheme.colorScheme.onPrimary

                            Surface(
                                onClick = {
                                    if (!isSaving) {
                                        onSaveQuickExcerpt(
                                            thought,
                                            currentUrl,
                                            selectedTags,
                                        )
                                    }
                                },
                                shape = CircleShape,
                                color = saveButtonBackgroundColor,
                                shadowElevation = if (thoughtNotEmpty) 6.dp else 2.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = saveButtonTextColor,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.fast_capture_save_continue),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = saveButtonTextColor,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = saveButtonTextColor,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showTagMenu || showLinkMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.14f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            dismissTagMenu()
                            showLinkMenu = false
                        },
                )
            }

            FastCaptureTagMenuPopup(
                showTagMenu = showTagMenu,
                availableTags = availableTags,
                selectedTags = tagDraftSelection,
                tagQuery = tagSearchQuery,
                onTagQueryChange = { tagSearchQuery = it },
                onTagSelected = { tag ->
                    tagDraftSelection =
                        if (tagDraftSelection.contains(tag)) {
                            tagDraftSelection - tag
                        } else {
                            tagDraftSelection + tag
                        }
                },
                onDismiss = dismissTagMenu,
                onConfirm = confirmTagMenu,
            )

            FastCaptureLinkMenuPopup(
                showLinkMenu = showLinkMenu,
                tempLink = tempLink,
                onTempLinkChange = { tempLink = it },
                onSaveLink = {
                    if (tempLink.trim().isNotEmpty()) {
                        currentUrl = tempLink
                    }
                    showLinkMenu = false
                },
            )
        }
    }
}

