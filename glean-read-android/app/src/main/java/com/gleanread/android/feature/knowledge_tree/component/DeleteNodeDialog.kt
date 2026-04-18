package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState

@Composable
fun DeleteNodeDialog(
    state: DeleteDialogUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除“${state.target.title}”？") },
        text = {
            if (state.hasChildren) {
                Text(
                    "该节点下还有 ${state.descendantCount} 个子节点。\n删除后整棵子树都会被移除。"
                )
            } else {
                Text("删除后不可恢复。")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
