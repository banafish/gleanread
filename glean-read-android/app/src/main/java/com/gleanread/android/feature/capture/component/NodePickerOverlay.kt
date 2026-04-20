@file:OptIn(ExperimentalFoundationApi::class)

package com.gleanread.android.feature.capture.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.model.TreeNodeUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import com.gleanread.android.core.ui.component.CaptureBottomSheet

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
    BackHandler(onBack = onDismiss)

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.node_picker_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onToggleCreate) {
                    Text(
                        text = stringResource(
                            if (createNewNode) R.string.node_picker_choose_existing
                            else R.string.node_picker_create_new,
                        ),
                    )
                }
            }
            if (createNewNode) {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = onTitleChange,
                    placeholder = { Text(stringResource(R.string.node_picker_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (selectedParentNodeId == null) {
                        stringResource(R.string.node_picker_parent_hint)
                    } else {
                        stringResource(
                            R.string.node_picker_parent_selected,
                            snapshot.flatNodes[selectedParentNodeId]?.title.orEmpty(),
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun NodePickerTreeRow(
    node: TreeNodeUiModel,
    createNewNode: Boolean,
    selectedTargetNodeId: String?,
    selectedParentNodeId: String?,
    onSelectTarget: (String) -> Unit,
    onSelectParent: (String) -> Unit,
    level: Int = 0,
) {
    Column {
        val selected = if (createNewNode) {
            node.id == selectedParentNodeId
        } else {
            node.id == selectedTargetNodeId
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                )
                .combinedClickable {
                    if (createNewNode) {
                        onSelectParent(node.id)
                    } else {
                        onSelectTarget(node.id)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.AccountTree,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = node.title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
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
