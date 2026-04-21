package com.gleanread.android.feature.tags.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gleanread.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsTopBar(
    onToggleSearch: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tags_title),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.tags_search),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        windowInsets = WindowInsets(0),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tags_search_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    innerTextField()
                },
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        },
        actions = {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.tags_search_clear),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        windowInsets = WindowInsets(0),
    )
}
