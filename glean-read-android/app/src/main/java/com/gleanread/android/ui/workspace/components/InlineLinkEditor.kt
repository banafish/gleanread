@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gleanread.android.data.model.BacklinkType
import com.gleanread.android.data.model.BacklinkUiModel
import com.gleanread.android.data.model.ExcerptUiModel
import com.gleanread.android.data.model.FlatNodeUiModel
import com.gleanread.android.data.model.GraphNodeKind
import com.gleanread.android.data.model.GraphUiModel
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.data.model.TagGroupUiModel
import com.gleanread.android.data.model.TreeNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.data.model.buildInlineAnnotatedString
import com.gleanread.android.data.model.currentInlineQuery
import com.gleanread.android.data.model.insertStructuredLink
import com.gleanread.android.ui.CaptureBottomSheet
import com.gleanread.android.ui.CaptureUI
import com.gleanread.android.ui.TagPill
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.itemsIndexed
@Composable
fun LinkAwareText(rawText: String, onLinkClick: (String) -> Unit) {
    val annotated = remember(rawText) { buildInlineAnnotatedString(rawText) }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(color = CaptureUI.Slate800, lineHeight = 22.sp),
        onClick = { offset ->
            annotated.getStringAnnotations("inline-link", offset, offset).firstOrNull()?.let {
                onLinkClick(it.item)
            }
        },
    )
}

@Composable
fun InlineLinkEditor(
    rawText: String,
    placeholder: String,
    onRawTextChange: (String) -> Unit,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    modifier: Modifier = Modifier,
    minLines: Int = 4,
    autoFocus: Boolean = false,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(rawText, selection = TextRange(rawText.length))) }
    var suggestions by remember { mutableStateOf<List<LinkSuggestion>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(rawText) {
        if (rawText != fieldValue.text) {
            fieldValue = fieldValue.copy(text = rawText, selection = TextRange(rawText.length))
        }
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { next ->
                fieldValue = next
                onRawTextChange(next.text)
                val query = currentInlineQuery(next.text, next.selection.start)
                searchJob?.cancel()
                if (query != null) {
                    searchJob = scope.launch {
                        delay(300)
                        suggestions = searchSuggestions(query)
                    }
                } else {
                    suggestions = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (!state.isFocused) suggestions = emptyList()
                },
            minLines = minLines,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.clickable(enabled = false) {},
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "联想结果",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        color = CaptureUI.Slate400,
                        fontSize = 12.sp,
                    )
                    suggestions.forEach { suggestion ->
                        TextButton(onClick = {
                            val (raw, cursor) = insertStructuredLink(fieldValue.text, fieldValue.selection.start, suggestion)
                            fieldValue = TextFieldValue(raw, selection = TextRange(cursor))
                            onRawTextChange(raw)
                            suggestions = emptyList()
                        }) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (suggestion.type.name == "NODE") "🌲 ${suggestion.title}" else "📎 ${suggestion.title}",
                                    color = CaptureUI.Slate800,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    suggestion.preview,
                                    color = CaptureUI.Slate400,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
