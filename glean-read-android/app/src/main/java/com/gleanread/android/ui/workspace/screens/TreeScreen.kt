@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.TreeNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.ui.CaptureUI

@Composable
fun TreeRoute(
    snapshot: WorkspaceSnapshot,
    onNodeClick: (String) -> Unit,
    onCreateRootNode: (String, (String) -> Unit) -> Unit,
) {
    var expandedIds by remember(snapshot.treeRoots) {
        mutableStateOf(snapshot.treeRoots.map { it.id }.toSet())
    }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newRootTitle by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🌲 知识体系", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Button(onClick = { showAddDialog = true }) { Text("＋ 根节点") }
            }
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    snapshot.treeRoots.forEach { node ->
                        TreeNodeRow(
                            node = node,
                            expandedIds = expandedIds,
                            onToggle = { id ->
                                expandedIds =
                                    if (expandedIds.contains(id)) expandedIds - id else expandedIds + id
                            },
                            onOpen = onNodeClick,
                        )
                    }
                }
                Spacer(Modifier.height(120.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新建根节点") },
            text = {
                OutlinedTextField(
                    value = newRootTitle,
                    onValueChange = { newRootTitle = it },
                    placeholder = { Text("输入根节点名称") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCreateRootNode(newRootTitle) {}
                    newRootTitle = ""
                    showAddDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            })
    }
}

@Composable
fun TreeNodeRow(
        node: TreeNodeUiModel,
        expandedIds: Set<String>,
        onToggle: (String) -> Unit,
        onOpen: (String) -> Unit,
        level: Int = 0,
    ) {
        val isExpanded = expandedIds.contains(node.id)
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (level * 18).dp, top = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (node.children.isNotEmpty()) if (isExpanded) "▼" else "▶" else "•",
                    modifier = Modifier.combinedClickable {
                        if (node.children.isNotEmpty()) onToggle(node.id)
                    },
                    color = CaptureUI.Slate400,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    node.title,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable { onOpen(node.id) },
                    color = CaptureUI.Slate800,
                    fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Medium,
                )
                Text("${node.count} 条", color = CaptureUI.Slate400, fontSize = 12.sp)
            }
            if (isExpanded) {
                node.children.forEach { child ->
                    TreeNodeRow(
                        node = child,
                        expandedIds = expandedIds,
                        onToggle = onToggle,
                        onOpen = onOpen,
                        level = level + 1,
                    )
                }
            }
        }
    }
