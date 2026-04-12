@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.data.model.buildInlineAnnotatedString
import com.gleanread.android.data.model.currentInlineQuery
import com.gleanread.android.data.model.insertStructuredLink
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LinkAwareText(rawText: String, onLinkClick: (String) -> Unit) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(rawText, linkColor) { buildInlineAnnotatedString(rawText, linkColor) }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        ),
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
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                rawText,
                selection = TextRange(rawText.length)
            )
        )
    }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "联想结果",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    suggestions.forEach { suggestion ->
                        TextButton(onClick = {
                            val (raw, cursor) = insertStructuredLink(
                                fieldValue.text,
                                fieldValue.selection.start,
                                suggestion
                            )
                            fieldValue = TextFieldValue(raw, selection = TextRange(cursor))
                            onRawTextChange(raw)
                            suggestions = emptyList()
                        }) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (suggestion.type.name == "NODE") Icons.Default.AccountTree else Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = suggestion.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Text(
                                    suggestion.preview,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
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
