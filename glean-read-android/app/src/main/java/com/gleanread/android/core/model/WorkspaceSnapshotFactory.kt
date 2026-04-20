package com.gleanread.android.core.model

import com.gleanread.android.data.repository.WorkspaceLocalSnapshot

class WorkspaceSnapshotFactory {
    private val excerptUiMapper = ExcerptUiMapper()
    private val nodeProjectionFactory = NodeProjectionFactory()
    private val tagProjectionFactory = TagProjectionFactory()
    private val backlinkBuilder = BacklinkBuilder()
    private val graphBuilder = GraphBuilder()

    fun create(snapshot: WorkspaceLocalSnapshot): WorkspaceSnapshot {
        val nodeMap = snapshot.nodes.associateBy { it.id }
        val tagMap = snapshot.tags.associateBy { it.id }
        val tagNamesByExcerptId = snapshot.relations.groupBy { it.excerptId }
            .mapValues { (_, value) -> value.mapNotNull { tagMap[it.tagId]?.tagName }.sorted() }
        val excerptsUi = excerptUiMapper.map(snapshot.excerpts, nodeMap, tagNamesByExcerptId)
        val nodeProjection = nodeProjectionFactory.create(snapshot.nodes, snapshot.excerpts)
        val tagProjection = tagProjectionFactory.create(snapshot.tags)
        val backlinksByNodeId = backlinkBuilder.build(snapshot.nodes, snapshot.excerpts)
        val excerptsById = excerptsUi.associateBy { it.id }
        val graphByNodeId = graphBuilder.build(
            flatNodes = nodeProjection.flatNodes,
            excerptsById = excerptsById,
            backlinksByNodeId = backlinksByNodeId,
        )

        return WorkspaceSnapshot(
            isEmpty = snapshot.excerpts.isEmpty() && snapshot.nodes.isEmpty() && snapshot.tags.isEmpty(),
            excerpts = excerptsUi.sortedByDescending { it.createTime },
            treeRoots = nodeProjection.treeRoots,
            flatNodes = nodeProjection.flatNodes,
            excerptsById = excerptsById,
            tagGroups = tagProjection.tagGroups,
            backlinksByNodeId = backlinksByNodeId,
            graphByNodeId = graphByNodeId,
            suggestedTags = tagProjection.suggestedTags,
        )
    }
}
