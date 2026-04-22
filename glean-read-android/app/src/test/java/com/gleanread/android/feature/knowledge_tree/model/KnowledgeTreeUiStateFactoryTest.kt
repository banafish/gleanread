package com.gleanread.android.feature.knowledge_tree.model

import com.gleanread.android.core.model.FlatNodeUiModel
import com.gleanread.android.core.model.TreeNodeUiModel
import com.gleanread.android.core.model.WorkspaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val KNOWLEDGE_TREE_ROOT_TITLE = "知识体系"

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
            rootTitle = KNOWLEDGE_TREE_ROOT_TITLE,
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
            rootTitle = KNOWLEDGE_TREE_ROOT_TITLE,
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

    @Test
    fun `move sheet hides target subtree from available destinations`() {
        val snapshot = buildMoveSnapshot()

        val uiState = buildMoveNodeBottomSheetUiModel(
            snapshot = snapshot,
            state = MoveNodeSheetUiState(
                targetNodeId = "child",
                targetNodeTitle = "child",
                sourceParentNodeId = "root",
                currentParentNodeId = null,
            ),
            rootTitle = KNOWLEDGE_TREE_ROOT_TITLE,
        )

        requireNotNull(uiState)

        assertTrue(uiState.confirmEnabled)
        assertEquals(listOf(KNOWLEDGE_TREE_ROOT_TITLE), uiState.breadcrumbs.map { it.title })
        assertEquals(listOf("root", "archive"), uiState.destinations.map { it.nodeId })
    }

    @Test
    fun `move sheet keeps current parent path and disables no-op confirmation`() {
        val snapshot = buildMoveSnapshot()

        val uiState = buildMoveNodeBottomSheetUiModel(
            snapshot = snapshot,
            state = MoveNodeSheetUiState(
                targetNodeId = "child",
                targetNodeTitle = "child",
                sourceParentNodeId = "root",
                currentParentNodeId = "root",
            ),
            rootTitle = KNOWLEDGE_TREE_ROOT_TITLE,
        )

        requireNotNull(uiState)

        assertFalse(uiState.confirmEnabled)
        assertEquals(
            listOf(KNOWLEDGE_TREE_ROOT_TITLE, "root"),
            uiState.breadcrumbs.map { it.title },
        )
        assertEquals(listOf("sibling"), uiState.destinations.map { it.nodeId })
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

    private fun buildMoveSnapshot(): WorkspaceSnapshot {
        val flatNodes = listOf(
            FlatNodeUiModel(
                id = "root",
                parentId = null,
                title = "root",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 0,
                childNodeIds = listOf("child", "sibling"),
            ),
            FlatNodeUiModel(
                id = "child",
                parentId = "root",
                title = "child",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 1,
                childNodeIds = listOf("grandchild"),
            ),
            FlatNodeUiModel(
                id = "grandchild",
                parentId = "child",
                title = "grandchild",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 0,
                childNodeIds = emptyList(),
            ),
            FlatNodeUiModel(
                id = "sibling",
                parentId = "root",
                title = "sibling",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 2,
                childNodeIds = emptyList(),
            ),
            FlatNodeUiModel(
                id = "archive",
                parentId = null,
                title = "archive",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 0,
                childNodeIds = emptyList(),
            ),
        ).associateBy { it.id }

        return WorkspaceSnapshot(
            isEmpty = false,
            excerpts = emptyList(),
            treeRoots = listOf(
                TreeNodeUiModel(
                    id = "root",
                    title = "root",
                    count = 0,
                    children = listOf(
                        TreeNodeUiModel(
                            id = "child",
                            title = "child",
                            count = 1,
                            children = listOf(
                                TreeNodeUiModel(
                                    id = "grandchild",
                                    title = "grandchild",
                                    count = 0,
                                    children = emptyList(),
                                ),
                            ),
                        ),
                        TreeNodeUiModel(
                            id = "sibling",
                            title = "sibling",
                            count = 2,
                            children = emptyList(),
                        ),
                    ),
                ),
                TreeNodeUiModel(
                    id = "archive",
                    title = "archive",
                    count = 0,
                    children = emptyList(),
                ),
            ),
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
