package com.gleanread.android.feature.knowledge_tree.model

import com.gleanread.android.core.model.FlatNodeUiModel
import com.gleanread.android.core.model.TreeNodeUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeTreeUiStateFactoryTest {
    @Test
    fun `home state keeps title clicks on detail even when deeper descendants are hidden`() {
        val snapshot = buildLinearSnapshot(
            "root",
            "child",
            "grandchild",
            "greatgrandchild",
        )

        val state = buildKnowledgeTreeHomeUiState(
            snapshot = snapshot,
            expandedIds = setOf("root", "child"),
        )

        val rootCard = state.rootCards.single()
        val child = rootCard.previewItems.single()
        val grandchild = child.visibleChildren.single()

        assertTrue(rootCard.canExpand)
        assertEquals(NodeDestination.Detail("child"), child.titleDestination)
        assertEquals(NodeDestination.Detail("grandchild"), grandchild.titleDestination)
        assertTrue(grandchild.showEnterBranch)
    }

    @Test
    fun `branch state keeps title clicks on detail while preserving enter-branch affordance`() {
        val snapshot = buildLinearSnapshot(
            "root",
            "child",
            "grandchild",
            "greatgrandchild",
            "fifth",
            "sixth",
        )

        val state = buildKnowledgeTreeBranchUiState(
            snapshot = snapshot,
            nodeId = "child",
            expandedIds = setOf("grandchild", "greatgrandchild"),
        )

        requireNotNull(state)

        val thirdLevel = state.items.single()
        val fourthLevel = thirdLevel.visibleChildren.single()
        val fifthLevel = fourthLevel.visibleChildren.single()

        assertEquals(
            listOf(KNOWLEDGE_TREE_ROOT_TITLE, "root", "child"),
            state.breadcrumbTitles,
        )
        assertFalse(state.isEmpty)
        assertEquals("grandchild", thirdLevel.title)
        assertEquals("greatgrandchild", fourthLevel.title)
        assertEquals("fifth", fifthLevel.title)
        assertTrue(thirdLevel.canExpand)
        assertTrue(fourthLevel.canExpand)
        assertTrue(fifthLevel.showEnterBranch)
        assertEquals(NodeDestination.Detail("grandchild"), thirdLevel.titleDestination)
        assertEquals(NodeDestination.Detail("greatgrandchild"), fourthLevel.titleDestination)
        assertEquals(NodeDestination.Detail("fifth"), fifthLevel.titleDestination)
    }

    @Test
    fun `branch node titles stay on detail when subtree fits current page`() {
        val snapshot = buildLinearSnapshot(
            "root",
            "child",
            "grandchild",
            "greatgrandchild",
            "fifth",
        )

        val state = buildKnowledgeTreeBranchUiState(
            snapshot = snapshot,
            nodeId = "child",
            expandedIds = setOf("grandchild", "greatgrandchild"),
        )

        requireNotNull(state)

        val thirdLevel = state.items.single()
        val fourthLevel = thirdLevel.visibleChildren.single()
        val fifthLevel = fourthLevel.visibleChildren.single()

        assertEquals(NodeDestination.Detail("grandchild"), thirdLevel.titleDestination)
        assertEquals(NodeDestination.Detail("greatgrandchild"), fourthLevel.titleDestination)
        assertEquals(NodeDestination.Detail("fifth"), fifthLevel.titleDestination)
        assertFalse(fifthLevel.showEnterBranch)
    }

    private fun buildLinearSnapshot(vararg nodeIds: String): WorkspaceSnapshot {
        val ids = nodeIds.toList()
        val flatNodes = ids.mapIndexed { index, nodeId ->
            val childId = ids.getOrNull(index + 1)
            nodeId to FlatNodeUiModel(
                id = nodeId,
                parentId = ids.getOrNull(index - 1),
                title = nodeId,
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = index + 1,
                childNodeIds = listOfNotNull(childId),
            )
        }.toMap()

        return WorkspaceSnapshot(
            isEmpty = false,
            excerpts = emptyList(),
            treeRoots = listOf(buildTree(ids, 0)),
            flatNodes = flatNodes,
            excerptsById = emptyMap(),
            tagGroups = emptyList(),
            backlinksByNodeId = emptyMap(),
            graphByNodeId = emptyMap(),
            suggestedTags = emptyList(),
        )
    }

    private fun buildTree(
        nodeIds: List<String>,
        index: Int,
    ): TreeNodeUiModel {
        val nodeId = nodeIds[index]
        return TreeNodeUiModel(
            id = nodeId,
            title = nodeId,
            count = index + 1,
            children = nodeIds.getOrNull(index + 1)?.let {
                listOf(buildTree(nodeIds, index + 1))
            } ?: emptyList(),
        )
    }
}
