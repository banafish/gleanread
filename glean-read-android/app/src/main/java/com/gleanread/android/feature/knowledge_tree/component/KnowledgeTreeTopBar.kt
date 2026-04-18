package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeTreeTopBar(
    title: String,
    onToggleSearch: () -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onBack: (() -> Unit)? = null,
    showTreeIcon: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showTreeIcon) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("展开全部") },
                    onClick = {
                        menuExpanded = false
                        onExpandAll()
                    },
                )
                DropdownMenuItem(
                    text = { Text("收起全部") },
                    onClick = {
                        menuExpanded = false
                        onCollapseAll()
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        windowInsets = WindowInsets(0),
    )
}
