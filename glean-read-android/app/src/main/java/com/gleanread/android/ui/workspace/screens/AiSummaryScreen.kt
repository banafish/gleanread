@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.data.model.LinkSuggestion
import com.gleanread.android.ui.CaptureUI

@Composable
fun AiSummaryRoute(
    uiState: WorkspaceUiState,
    searchSuggestions: suspend (String) -> List<LinkSuggestion>,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onSelectTargetNode: (String?) -> Unit,
    onSelectParentNode: (String?) -> Unit,
    onMarkdownChange: (String) -> Unit,
    onNewNodeTitleChange: (String) -> Unit,
) {
    var showNodePicker by rememberSaveable { mutableStateOf(false) }
    var createNewNode by rememberSaveable { mutableStateOf(false) }
    val draft = uiState.aiSummaryDraft
    val selectedExcerpts = draft.selectedExcerptIds.mapNotNull(uiState.snapshot.excerptsById::get)
    val selectedNodeTitle = draft.targetNodeId?.let { uiState.snapshot.flatNodes[it]?.title }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) { Text("✕ 取消") }
            Text("AI 整理助手", fontWeight = FontWeight.Bold)
            Button(
                onClick = onSave,
                enabled = draft.markdown.isNotBlank() && (draft.targetNodeId != null || draft.newNodeTitle.isNotBlank()),
            ) { Text("保存") }
        }
        Spacer(Modifier.height(16.dp))
        Text("✨ 总结大纲", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (draft.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            InlineLinkEditor(
                rawText = draft.markdown,
                placeholder = "输入 AI 大纲内容",
                onRawTextChange = onMarkdownChange,
                searchSuggestions = searchSuggestions,
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("🎯 挂载到知识树", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { showNodePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF8FAFC),
                contentColor = CaptureUI.Slate700
            ),
        ) {
            Text(
                selectedNodeTitle
                    ?: if (draft.newNodeTitle.isNotBlank()) "新建节点：${draft.newNodeTitle}" else "＋ 选择或创建目标节点 (必填)",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("📎 关联的知识摘录 (${selectedExcerpts.size}项)", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CaptureUI.Slate50)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedExcerpts.forEach { excerpt ->
                    Text(
                        "- ${excerpt.content}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = CaptureUI.Slate600,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showNodePicker) {
        NodePickerOverlay(
            snapshot = uiState.snapshot,
            createNewNode = createNewNode,
            draftTitle = draft.newNodeTitle,
            selectedTargetNodeId = draft.targetNodeId,
            selectedParentNodeId = draft.parentNodeId,
            onDismiss = { showNodePicker = false },
            onToggleCreate = { createNewNode = !createNewNode },
            onSelectTarget = {
                createNewNode = false
                onSelectTargetNode(it)
                showNodePicker = false
            },
            onSelectParent = {
                createNewNode = true
                onSelectTargetNode(null)
                onSelectParentNode(it)
            },
            onTitleChange = onNewNodeTitleChange,
        )
    }
}
