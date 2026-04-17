package com.gleanread.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.capture.CaptureSeedResolver
import com.gleanread.android.capture.PageContextAccessibilityState
import com.gleanread.android.capture.PageContextStore
import com.gleanread.android.capture.PageContextSupport
import com.gleanread.android.data.local.WorkspaceDatabase
import com.gleanread.android.data.repository.WorkspaceRepository
import com.gleanread.android.ui.CaptureBottomSheet
import com.gleanread.android.ui.ContextHintCard
import com.gleanread.android.ui.RichExcerptCard
import com.gleanread.android.ui.theme.GleanReadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FastCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val captureSeed = CaptureSeedResolver(
            pageContextStore = PageContextStore(applicationContext),
        ).resolve(
            intent = intent,
            referrer = referrer,
        )

        setContent {
            SideEffect {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.setBackgroundBlurRadius(50)
                }
            }
            GleanReadTheme {
                CaptureDialogV2(
                    initialSharedContent = captureSeed.content,
                    initialUrl = captureSeed.url,
                    initialSourceTitle = captureSeed.sourceTitle,
                    sourcePackage = captureSeed.sourcePackage,
                    usedCachedUrl = captureSeed.usedCachedUrl,
                    usedCachedTitle = captureSeed.usedCachedTitle,
                    onDismiss = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptureDialogV2(
    initialSharedContent: String,
    initialUrl: String,
    initialSourceTitle: String,
    sourcePackage: String,
    usedCachedUrl: Boolean,
    usedCachedTitle: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        WorkspaceRepository(WorkspaceDatabase.get(context))
    }
    val availableTagsFlow = remember(repository) { repository.observeAvailableTagNames() }
    val availableTags by availableTagsFlow.collectAsState(initial = emptyList())
    var thought by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val isAccessibilityEnabled = remember(context) {
        PageContextAccessibilityState.isEnabled(context)
    }
    val shouldShowBackfillPrompt = usedCachedTitle || usedCachedUrl
    val shouldShowAccessibilityPrompt = !shouldShowBackfillPrompt &&
        !isAccessibilityEnabled &&
        (sourcePackage.isBlank() || PageContextSupport.isSupportedPackage(sourcePackage)) &&
        (initialUrl.isBlank() || initialSourceTitle.isBlank())
    val collapsedSheetHeightFraction = if (shouldShowAccessibilityPrompt) 0.64f else 0.54f
    val sheetHeightFraction by animateFloatAsState(
        targetValue = if (isImeVisible) 0.9f else collapsedSheetHeightFraction,
        animationSpec = tween(durationMillis = 220),
        label = "capture_sheet_height",
    )

    var isSaving by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showLinkMenu by remember { mutableStateOf(false) }
    var tempLink by remember { mutableStateOf("") }
    val shouldCompactCaptureLayout = isImeVisible || isInputFocused

    val targetBgColor =
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

    val containerBgColor by animateColorAsState(targetValue = targetBgColor, label = "bg_color")
    val containerBorderColor by animateColorAsState(targetValue = targetBorderColor, label = "border_color")

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(sheetHeightFraction),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "极速摘录",
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
                            content = initialSharedContent,
                            url = currentUrl,
                            sourceTitle = initialSourceTitle,
                        )
                        if (shouldShowAccessibilityPrompt) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ContextHintCard(
                                text = "当前分享缺少标题或链接。开启辅助识别后，下次可尝试从浏览器或公众号页面自动补齐来源。",
                                actionLabel = "去开启",
                                onActionClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK,
                                        ),
                                    )
                                },
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
                            .background(containerBgColor, RoundedCornerShape(24.dp))
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
                                        showTagMenu = false
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
                                            text = "此刻你的想法是...",
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
                                val tagMenuBtnActive = selectedTags.isNotEmpty() || showTagMenu
                                val tagMenuBtnTint =
                                    if (tagMenuBtnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        showTagMenu = !showTagMenu
                                        showLinkMenu = false
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalOffer,
                                            contentDescription = "Tags",
                                            tint = tagMenuBtnTint,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        if (selectedTags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val tagText = if (selectedTags.size == 1) selectedTags.first() else "${selectedTags.first()}等"
                                            Text(
                                                text = tagText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tagMenuBtnTint,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 50.dp),
                                            )
                                        }
                                    }
                                }

                                val linkMenuBtnTint =
                                    if (showLinkMenu) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        showLinkMenu = !showLinkMenu
                                        showTagMenu = false
                                        if (showLinkMenu) tempLink = currentUrl
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = "Link",
                                        tint = linkMenuBtnTint,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(22.dp),
                                    )
                                }
                            }

                            val thoughtNotEmpty = thought.isNotEmpty()
                            val saveBtnBg = MaterialTheme.colorScheme.primary
                            val saveBtnTextColor = MaterialTheme.colorScheme.onPrimary

                            Surface(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    repository.saveQuickExcerpt(
                                                        content = initialSharedContent,
                                                        thought = thought,
                                                        url = currentUrl,
                                                        sourceTitle = initialSourceTitle,
                                                        tagNames = selectedTags.toList(),
                                                        archiveNodeId = null,
                                                    )
                                                }
                                                onDismiss()
                                            } catch (_: Exception) {
                                                isSaving = false
                                            }
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = saveBtnBg,
                                shadowElevation = if (thoughtNotEmpty) 6.dp else 2.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = saveBtnTextColor,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Text(
                                            text = "保存并继续",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = saveBtnTextColor,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = saveBtnTextColor,
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            showTagMenu = false
                            showLinkMenu = false
                        },
                )
            }

            TagMenuPopup(
                showTagMenu = showTagMenu,
                availableTags = availableTags,
                selectedTags = selectedTags,
                onTagSelected = { tag ->
                    selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                },
                onClearTags = { selectedTags = emptySet() },
            )

            LinkMenuPopup(
                showLinkMenu = showLinkMenu,
                tempLink = tempLink,
                onTempLinkChange = { tempLink = it },
                onSaveLink = {
                    if (tempLink.trim().isNotEmpty()) currentUrl = tempLink
                    showLinkMenu = false
                },
            )
        }
    }
}

@Composable
private fun PopupTagPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bgColor)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.TagMenuPopup(
    showTagMenu: Boolean,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagSelected: (String) -> Unit,
    onClearTags: () -> Unit,
) {
    AnimatedVisibility(
        visible = showTagMenu,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200), transformOrigin = TransformOrigin(0f, 1f)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150), transformOrigin = TransformOrigin(0f, 1f)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 52.dp)
            .offset(x = (-12).dp)
            .graphicsLayer { clip = false },
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .graphicsLayer { clip = false },
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (availableTags.isEmpty()) {
                            Text(
                                text = "本地标签库还是空的，先去工作区创建几个标签吧。",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                availableTags.forEach { tag ->
                                    PopupTagPill(
                                        label = tag,
                                        isSelected = selectedTags.contains(tag),
                                        onClick = { onTagSelected(tag) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "选择分类标签",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selectedTags.isNotEmpty()) {
                            Text(
                                text = "清空",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onClearTags() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LinkMenuPopup(
    showLinkMenu: Boolean,
    tempLink: String,
    onTempLinkChange: (String) -> Unit,
    onSaveLink: () -> Unit,
) {
    AnimatedVisibility(
        visible = showLinkMenu,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200), transformOrigin = TransformOrigin(0f, 1f)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150), transformOrigin = TransformOrigin(0f, 1f)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 52.dp)
            .offset(x = (-12).dp)
            .graphicsLayer { clip = false },
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .graphicsLayer { clip = false },
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val inputBg = MaterialTheme.colorScheme.surface
                    val inputBorder = MaterialTheme.colorScheme.outlineVariant

                    BasicTextField(
                        value = tempLink,
                        onValueChange = onTempLinkChange,
                        modifier = Modifier
                            .weight(1f)
                            .background(inputBg, RoundedCornerShape(8.dp))
                            .border(1.dp, inputBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSaveLink() }),
                        decorationBox = { inner ->
                            if (tempLink.isEmpty()) {
                                Text(
                                    text = "http://...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = { onSaveLink() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}


