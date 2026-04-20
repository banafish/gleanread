package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gleanread.android.R
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState

@Composable
fun DeleteNodeDialog(
    state: DeleteDialogUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dialogTitle = stringResource(R.string.knowledge_tree_delete_node, state.target.title)
    val dialogBody = if (state.hasChildren) {
        stringResource(R.string.knowledge_tree_delete_with_children, state.descendantCount)
    } else {
        stringResource(R.string.knowledge_tree_delete_irreversible)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = { Text(dialogBody) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.knowledge_tree_delete_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
