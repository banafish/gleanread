package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

@Composable
fun RenameNodeDialog(
    state: NodeDialogUiState,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    // 1. 判断当前是否为深色模式
    val isDark = isSystemInDarkTheme()

    // 2. 根据深浅色模式动态计算绝对对比度背景色
    val inputBackgroundColor = if (isDark) {
        Color.White.copy(alpha = 0.1f) // 暗色模式下叠加一层微弱的纯白
    } else {
        Color.Black.copy(alpha = 0.06f) // 亮色模式下叠加一层微弱的纯黑
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = state.inputValue,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            text = state.targetNodeTitle ?: "请输入节点名称",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        // 3. 应用计算好的绝对对比度底色
                        focusedContainerColor = inputBackgroundColor,
                        unfocusedContainerColor = inputBackgroundColor,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
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
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
