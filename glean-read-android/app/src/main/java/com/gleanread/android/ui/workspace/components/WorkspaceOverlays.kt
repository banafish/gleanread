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
import com.gleanread.android.ui.TagPill
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay

@Composable
fun NodePickerOverlay(
    snapshot: WorkspaceSnapshot,
    createNewNode: Boolean,
    draftTitle: String,
    selectedTargetNodeId: String?,
    selectedParentNodeId: String?,
    onDismiss: () -> Unit,
    onToggleCreate: () -> Unit,
    onSelectTarget: (String) -> Unit,
    onSelectParent: (String) -> Unit,
    onTitleChange: (String) -> Unit,
) {
    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .combinedClickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CaptureBottomSheet(onDismiss = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).imePadding()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("选择目标节点", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = onToggleCreate) { Text(if (createNewNode) "选择现有节点" else "新建子节点") }
                }
                if (createNewNode) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = onTitleChange,
                        placeholder = { Text("输入新子节点标题") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selectedParentNodeId == null) "从下方选择父节点" else "父节点：${snapshot.flatNodes[selectedParentNodeId]?.title}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.treeRoots.forEach { node ->
                        NodePickerTreeRow(
                            node = node,
                            createNewNode = createNewNode,
                            selectedTargetNodeId = selectedTargetNodeId,
                            selectedParentNodeId = selectedParentNodeId,
                            onSelectTarget = onSelectTarget,
                            onSelectParent = onSelectParent,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun NodePickerTreeRow(
    node: TreeNodeUiModel,
    createNewNode: Boolean,
    selectedTargetNodeId: String?,
    selectedParentNodeId: String?,
    onSelectTarget: (String) -> Unit,
    onSelectParent: (String) -> Unit,
    level: Int = 0,
) {
    Column {
        val selected = if (createNewNode) node.id == selectedParentNodeId else node.id == selectedTargetNodeId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .combinedClickable {
                    if (createNewNode) onSelectParent(node.id) else onSelectTarget(node.id)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.AccountTree, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(node.title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        }
        node.children.forEach {
            NodePickerTreeRow(
                node = it,
                createNewNode = createNewNode,
                selectedTargetNodeId = selectedTargetNodeId,
                selectedParentNodeId = selectedParentNodeId,
                onSelectTarget = onSelectTarget,
                onSelectParent = onSelectParent,
                level = level + 1,
            )
        }
    }
}

@Composable
fun QuickCaptureOverlay(
    snapshot: WorkspaceSnapshot,
    draft: QuickCaptureDraft,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onThoughtChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onArchiveNodeSelect: (String?) -> Unit,
    onSave: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showArchivePicker by rememberSaveable { mutableStateOf(false) }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .combinedClickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CaptureBottomSheet(onDismiss = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("极速记录", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                InlineLinkEditor(
                    rawText = draft.content,
                    placeholder = "在此输入摘录、想法或粘贴链接...",
                    onRawTextChange = onContentChange,
                    searchSuggestions = searchSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    autoFocus = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("提示：输入 [[ 触发双向链接", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                InlineLinkEditor(
                    rawText = draft.thought,
                    placeholder = "补充你的想法（选填）",
                    onRawTextChange = onThoughtChange,
                    searchSuggestions = searchSuggestions,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft.url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text("添加来源 URL（选填）")
                        }
                    },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                Text("推荐标签", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.suggestedTags.forEach { tag ->
                        val selected = draft.selectedTags.contains(tag.fullName)
                        TagPill(label = tag.label, isSelected = selected) { onTagToggle(tag.fullName) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { showArchivePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            draft.archiveNodeId?.let { "${snapshot.flatNodes[it]?.title ?: "未归档 (Inbox)"}" } ?: "未归档 (Inbox)",
                            textAlign = TextAlign.Left,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSave()
                    },
                    enabled = draft.content.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("发送")
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        if (showArchivePicker) {
            ArchivePickerDialog(
                snapshot = snapshot,
                selectedNodeId = draft.archiveNodeId,
                onDismiss = { showArchivePicker = false },
                onSelect = {
                    onArchiveNodeSelect(it)
                    showArchivePicker = false
                }
            )
        }
    }
}

@Composable
fun ArchivePickerDialog(
    snapshot: WorkspaceSnapshot,
    selectedNodeId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择归档位置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = { onSelect(null) }) {
                    if (selectedNodeId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("未归档 (Inbox)")
                }
                snapshot.treeRoots.forEach { node ->
                    ArchiveNodeRow(node, selectedNodeId, onSelect)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
fun ArchiveNodeRow(node: TreeNodeUiModel, selectedNodeId: String?, onSelect: (String?) -> Unit, level: Int = 0) {
    Column {
        TextButton(onClick = { onSelect(node.id) }, modifier = Modifier.padding(start = (level * 16).dp)) {
            if (selectedNodeId == node.id) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(node.title, color = MaterialTheme.colorScheme.onSurface)
        }
        node.children.forEach { ArchiveNodeRow(it, selectedNodeId, onSelect, level + 1) }
    }
}

@Composable
fun ExcerptPreviewDialog(
    excerpt: ExcerptUiModel,
    onDismiss: () -> Unit,
    onOpenNode: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(excerpt.sourceTitle ?: "摘录预览", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinkAwareText(rawText = excerpt.content, onLinkClick = {})
                if (excerpt.thought.isNotBlank()) {
                    LinkAwareText(rawText = excerpt.thought, onLinkClick = {})
                }
                if (excerpt.archivedNodeTitle != null) {
                    Text("已归档到 ${excerpt.archivedNodeTitle}", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpenNode(excerpt.archivedNodeId) }) { Text("打开节点") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
