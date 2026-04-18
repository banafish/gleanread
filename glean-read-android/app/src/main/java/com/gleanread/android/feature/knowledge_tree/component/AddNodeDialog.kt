package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

@Composable
fun AddNodeDialog(
    state: NodeDialogUiState,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column {
                state.parentNodeTitle?.let { parentTitle ->
                    Text("父节点：$parentTitle")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = state.inputValue,
                    onValueChange = onValueChange,
                    placeholder = { Text("请输入节点名称") },
                    singleLine = true,
                )
            }
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
