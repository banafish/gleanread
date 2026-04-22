package com.gleanread.android.feature.knowledge_tree

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.gleanread.android.feature.knowledge_tree.model.DeleteDialogUiState
import com.gleanread.android.feature.knowledge_tree.model.MoveNodeSheetUiState
import com.gleanread.android.feature.knowledge_tree.model.NodeActionTarget
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogType
import com.gleanread.android.feature.knowledge_tree.model.NodeDialogUiState

val ExpandedIdsSaver: Saver<Set<String>, Any> = listSaver(
    save = { state -> state.toList() },
    restore = { restored -> restored.filterIsInstance<String>().toSet() },
)

val NodeDialogUiStateSaver: Saver<NodeDialogUiState?, Any> = listSaver(
    save = { state ->
        state?.let {
            listOf(
                it.type.name,
                it.inputValue,
                it.parentNodeId.orEmpty(),
                it.parentNodeTitle.orEmpty(),
                it.targetNodeId.orEmpty(),
                it.targetNodeTitle.orEmpty(),
            )
        } ?: emptyList()
    },
    restore = { restored ->
        if (restored.isEmpty()) {
            null
        } else {
            NodeDialogUiState(
                type = NodeDialogType.valueOf(restored[0] as String),
                inputValue = restored[1] as String,
                parentNodeId = (restored[2] as String).ifBlank { null },
                parentNodeTitle = (restored[3] as String).ifBlank { null },
                targetNodeId = (restored[4] as String).ifBlank { null },
                targetNodeTitle = (restored[5] as String).ifBlank { null },
            )
        }
    },
)

val DeleteDialogUiStateSaver: Saver<DeleteDialogUiState?, Any> = listSaver(
    save = { state ->
        state?.let {
            listOf(
                it.target.nodeId,
                it.target.title,
                it.target.childCount,
                it.target.parentNodeId.orEmpty(),
                it.descendantCount,
            )
        } ?: emptyList()
    },
    restore = { restored ->
        if (restored.isEmpty()) {
            null
        } else {
            DeleteDialogUiState(
                target = NodeActionTarget(
                    nodeId = restored[0] as String,
                    title = restored[1] as String,
                    childCount = restored[2] as Int,
                    parentNodeId = (restored[3] as String).ifBlank { null },
                ),
                descendantCount = restored[4] as Int,
            )
        }
    },
)

val MoveNodeSheetUiStateSaver: Saver<MoveNodeSheetUiState?, Any> = listSaver(
    save = { state ->
        state?.let {
            listOf(
                it.targetNodeId,
                it.targetNodeTitle,
                it.sourceParentNodeId.orEmpty(),
                it.currentParentNodeId.orEmpty(),
            )
        } ?: emptyList()
    },
    restore = { restored ->
        if (restored.isEmpty()) {
            null
        } else {
            MoveNodeSheetUiState(
                targetNodeId = restored[0] as String,
                targetNodeTitle = restored[1] as String,
                sourceParentNodeId = (restored[2] as String).ifBlank { null },
                currentParentNodeId = (restored[3] as String).ifBlank { null },
            )
        }
    },
)
