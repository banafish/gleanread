package com.gleanread.android.feature.knowledge_tree.model

import com.gleanread.android.data.model.FlatNodeUiModel
import com.gleanread.android.data.model.WorkspaceSnapshot
import com.gleanread.android.data.model.TreeNodeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeTreeUiStateFactoryTest {
    @Test
    fun `home state exposes third-level branch entry when deeper nodes exist`() {
        val snapshot = buildSnapshot()

        val state = buildKnowledgeTreeHomeUiState(
            snapshot = snapshot,
            expandedIds = setOf("root", "child"),
        )

        val rootCard = state.rootCards.single()
        val child = rootCard.previewItems.single()
        val grandchild = child.visibleChildren.single()

        assertTrue(rootCard.canExpand)
        assertTrue(child.canExpand)
        assertTrue(grandchild.showEnterBranch)
        assertEquals(NodeDestination.Detail("grandchild"), grandchild.detailDestination)
        assertEquals(NodeDestination.Branch("grandchild"), grandchild.branchDestination)
    }

    @Test
    fun `branch state builds breadcrumb and local preview`() {
        val snapshot = buildSnapshot()

        val state = buildKnowledgeTreeBranchUiState(
            snapshot = snapshot,
            nodeId = "child",
            expandedIds = setOf("grandchild"),
        )

        requireNotNull(state)

        assertEquals(
            listOf("知识体系", "根节点", "二级节点"),
            state.breadcrumbTitles,
        )
        assertFalse(state.isEmpty)
        assertEquals("三级节点", state.items.single().title)
        assertEquals("四级节点", state.items.single().visibleChildren.single().title)
    }

    private fun buildSnapshot(): WorkspaceSnapshot {
        val flatNodes = mapOf(
            "root" to FlatNodeUiModel(
                id = "root",
                parentId = null,
                title = "根节点",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 1,
                childNodeIds = listOf("child"),
            ),
            "child" to FlatNodeUiModel(
                id = "child",
                parentId = "root",
                title = "二级节点",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 2,
                childNodeIds = listOf("grandchild"),
            ),
            "grandchild" to FlatNodeUiModel(
                id = "grandchild",
                parentId = "child",
                title = "三级节点",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 3,
                childNodeIds = listOf("greatgrandchild"),
            ),
            "greatgrandchild" to FlatNodeUiModel(
                id = "greatgrandchild",
                parentId = "grandchild",
                title = "四级节点",
                outlineMarkdown = "",
                excerptIds = emptyList(),
                excerptCount = 4,
                childNodeIds = emptyList(),
            ),
        )

        return WorkspaceSnapshot(
            isEmpty = false,
            excerpts = emptyList(),
            treeRoots = listOf(
                TreeNodeUiModel(
                    id = "root",
                    title = "根节点",
                    count = 1,
                    children = listOf(
                        TreeNodeUiModel(
                            id = "child",
                            title = "二级节点",
                            count = 2,
                            children = listOf(
                                TreeNodeUiModel(
                                    id = "grandchild",
                                    title = "三级节点",
                                    count = 3,
                                    children = listOf(
                                        TreeNodeUiModel(
                                            id = "greatgrandchild",
                                            title = "四级节点",
                                            count = 4,
                                            children = emptyList(),
                                        )
                                    ),
                                )
                            ),
                        )
                    ),
                )
            ),
            flatNodes = flatNodes,
            excerptsById = emptyMap(),
            tagGroups = emptyList(),
            backlinksByNodeId = emptyMap(),
            graphByNodeId = emptyMap(),
            suggestedTags = emptyList(),
        )
    }
}
