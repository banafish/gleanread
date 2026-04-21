package com.gleanread.android.core.model

object WorkspacePreviewData {
    fun snapshot(): WorkspaceSnapshot {
        val now = System.currentTimeMillis()
        val excerptOne = ExcerptUiModel(
            id = "excerpt-1",
            content = "Compose state should be hoisted to keep screens easier to test.",
            thought = "This note should eventually live under the architecture node.",
            url = "https://example.com/compose-state",
            sourceTitle = "Compose Notes",
            tags = listOf("compose", "state"),
            archivedNodeId = "node-1",
            archivedNodeTitle = "Compose Architecture",
            createTime = now - 45_000L,
        )
        val excerptTwo = ExcerptUiModel(
            id = "excerpt-2",
            content = "Tag flows work better when search and filter state are kept stable.",
            thought = "",
            url = null,
            sourceTitle = "Tag Guide",
            tags = listOf("tags"),
            archivedNodeId = null,
            archivedNodeTitle = null,
            createTime = now - 2 * 24 * 60 * 60 * 1_000L,
        )
        val rootNode = TreeNodeUiModel(
            id = "node-1",
            title = "Compose Architecture",
            count = 1,
            children = listOf(
                TreeNodeUiModel(
                    id = "node-2",
                    title = "State Management",
                    count = 0,
                    children = emptyList(),
                ),
            ),
        )
        val flatNodes = mapOf(
            "node-1" to FlatNodeUiModel(
                id = "node-1",
                parentId = null,
                title = "Compose Architecture",
                outlineMarkdown = "State -> UI",
                excerptIds = listOf("excerpt-1"),
                excerptCount = 1,
                childNodeIds = listOf("node-2"),
            ),
            "node-2" to FlatNodeUiModel(
                id = "node-2",
                parentId = "node-1",
                title = "State Management",
                outlineMarkdown = "Remember vs rememberSaveable",
                excerptIds = emptyList(),
                excerptCount = 0,
                childNodeIds = emptyList(),
            ),
        )
        return WorkspaceSnapshot(
            isEmpty = false,
            excerpts = listOf(excerptOne, excerptTwo),
            treeRoots = listOf(rootNode),
            flatNodes = flatNodes,
            excerptsById = listOf(excerptOne, excerptTwo).associateBy { it.id },
            tagGroups = listOf(
                TagGroupUiModel(
                    folder = "Notes",
                    count = 3,
                    items = listOf(
                        TagUiModel(
                            id = "tag-1",
                            folder = "Notes",
                            displayName = "compose",
                            fullName = "Notes/compose",
                            heatWeight = 2,
                        ),
                        TagUiModel(
                            id = "tag-2",
                            folder = "Notes",
                            displayName = "state",
                            fullName = "Notes/state",
                            heatWeight = 1,
                        ),
                    ),
                ),
            ),
            backlinksByNodeId = mapOf(
                "node-1" to listOf(
                    BacklinkUiModel(
                        sourceId = "excerpt-1",
                        title = "Compose Notes",
                        sourceType = BacklinkType.EXCERPT,
                        snippet = "State should be hoisted...",
                    ),
                ),
            ),
            graphByNodeId = mapOf(
                "node-1" to GraphUiModel(
                    nodes = listOf(
                        GraphUiNode("node-1", "Compose Architecture", GraphNodeKind.CURRENT_NODE),
                        GraphUiNode("node-2", "State Management", GraphNodeKind.LINKED_NODE),
                        GraphUiNode("excerpt-1", "Compose Notes", GraphNodeKind.EXCERPT),
                    ),
                    edges = listOf(
                        GraphUiEdge("node-1", "node-2"),
                        GraphUiEdge("node-1", "excerpt-1"),
                    ),
                ),
            ),
            suggestedTags = listOf(
                SuggestedTagUiModel(fullName = "Notes/compose", label = "#compose"),
                SuggestedTagUiModel(fullName = "Notes/state", label = "#state"),
            ),
        )
    }
}
