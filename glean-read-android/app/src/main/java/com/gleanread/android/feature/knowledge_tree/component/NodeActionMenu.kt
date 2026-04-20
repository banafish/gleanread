package com.gleanread.android.feature.knowledge_tree.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gleanread.android.R

@Composable
fun NodeActionMenu(
    onEnterBranch: () -> Unit,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val actionsContentDescription = stringResource(R.string.knowledge_tree_node_actions)
    val enterBranchLabel = stringResource(R.string.knowledge_tree_enter_branch)
    val addChildLabel = stringResource(R.string.knowledge_tree_add_child_node)
    val renameLabel = stringResource(R.string.knowledge_tree_rename_action)
    val deleteLabel = stringResource(R.string.knowledge_tree_delete_action)

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = actionsContentDescription,
            )
        }
        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp)),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(enterBranchLabel) },
                    onClick = {
                        expanded = false
                        onEnterBranch()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(addChildLabel) },
                    onClick = {
                        expanded = false
                        onAddChild()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(renameLabel) },
                    onClick = {
                        expanded = false
                        onRename()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(deleteLabel) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}
