package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

@Composable
fun RenameNodeDialog(
    state: NodeDialogUiState,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            OutlinedTextField(
                value = state.inputValue,
                onValueChange = onValueChange,
                placeholder = { Text(state.targetNodeTitle ?: "请输入节点名称") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.inputValue.isNotBlank(),
            ) { Text(state.confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
