package com.gleanread.android.data.repository

import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus

object SampleSeedData {
    fun create(
        now: Long,
        userId: String = LOCAL_USER_ID,
        deviceId: String? = null,
    ): SampleWorkspaceData {
        val ids = SampleIds.create()
        return SampleWorkspaceData(
            nodes = nodes(now, userId, deviceId, ids),
            tags = tags(now, userId, deviceId, ids),
            excerpts = excerpts(now, userId, deviceId, ids),
            excerptTags = excerptTags(now, userId, deviceId, ids),
        )
    }

    private fun nodes(
        now: Long,
        userId: String,
        deviceId: String?,
        ids: SampleIds,
    ): List<KnowledgeTreeNodeEntity> = listOf(
        node(
            id = ids.growthNodeId,
            userId = userId,
            parentId = null,
            title = "🧠 个人成长",
            outlineMarkdown = "围绕个人成长建立长期知识体系。",
            createTime = now - 80_000,
            updateTime = now - 30_000,
            deviceId = deviceId,
            sortOrder = 1,
        ),
        node(
            id = ids.timeNodeId,
            userId = userId,
            parentId = ids.growthNodeId,
            title = "⏱️ 时间管理",
            outlineMarkdown = "聚焦高频时间管理方法。",
            createTime = now - 70_000,
            updateTime = now - 30_000,
            deviceId = deviceId,
            sortOrder = 1,
        ),
        node(
            id = ids.readingNodeId,
            userId = userId,
            parentId = ids.growthNodeId,
            title = "📖 阅读方法",
            outlineMarkdown = "主题阅读是快速建立领域认知的核心方法。",
            createTime = now - 60_000,
            updateTime = now - 20_000,
            deviceId = deviceId,
            sortOrder = 2,
        ),
        node(
            id = ids.techNodeId,
            userId = userId,
            parentId = null,
            title = "💻 技术开发",
            outlineMarkdown = "记录技术栈、架构和产品实现经验。",
            createTime = now - 50_000,
            updateTime = now - 20_000,
            deviceId = deviceId,
            sortOrder = 2,
        ),
        node(
            id = ids.investNodeId,
            userId = userId,
            parentId = null,
            title = "💰 投资理财",
            outlineMarkdown = "整理投资框架和复盘笔记。",
            createTime = now - 40_000,
            updateTime = now - 10_000,
            deviceId = deviceId,
            sortOrder = 3,
        ),
    )

    private fun tags(
        now: Long,
        userId: String,
        deviceId: String?,
        ids: SampleIds,
    ): List<TagEntity> = listOf(
        tag(ids.aiTagId, userId, "AI/大模型", 18, now, deviceId),
        tag(ids.efficiencyTagId, userId, "效率工具", 12, now, deviceId),
        tag(ids.knowledgeTagId, userId, "知识管理", 15, now, deviceId),
        tag(ids.methodTagId, userId, "方法论", 10, now, deviceId),
        tag(ids.frontendTagId, userId, "技术/前端", 10, now, deviceId),
        tag(ids.backendTagId, userId, "技术/后端", 15, now, deviceId),
        tag(ids.psychologyTagId, userId, "阅读/心理学", 5, now, deviceId),
        tag(ids.randomTagId, userId, "无分类/随想", 8, now, deviceId),
        tag(ids.learningTagId, userId, "学习方法", 7, now, deviceId),
    )

    private fun excerpts(
        now: Long,
        userId: String,
        deviceId: String?,
        ids: SampleIds,
    ): List<ExcerptEntity> = listOf(
        excerpt(
            id = ids.ragExcerptId,
            userId = userId,
            content = "RAG技术的核心是将外部知识库与大模型结合，解决幻觉问题并提供实时数据。",
            url = "https://mp.weixin.qq.com/s/rag-demo",
            sourceTitle = "如何理解RAG架构",
            userThought = "这个可以用在我的知识库App搜索功能里。",
            treeNodeId = null,
            createTime = now - 35_000,
            updateTime = now - 35_000,
            deviceId = deviceId,
        ),
        excerpt(
            id = ids.promptExcerptId,
            userId = userId,
            content = "提示词工程的5个核心要素是角色、上下文、任务、格式要求和示例。",
            url = "https://example.com/prompt-guide",
            sourceTitle = "Prompt Engineering Guide",
            userThought = "整理成给新人看的清单会很有用。",
            treeNodeId = null,
            createTime = now - 34_000,
            updateTime = now - 34_000,
            deviceId = deviceId,
        ),
        excerpt(
            id = ids.zettelkastenExcerptId,
            userId = userId,
            content = "卡片盒笔记法强调原子化记录，每张卡片只写一个不可分割的概念。",
            url = null,
            sourceTitle = "《卡片笔记作法》",
            userThought = "我需要把旧笔记拆分成原子级别。",
            treeNodeId = null,
            createTime = now - 33_000,
            updateTime = now - 33_000,
            deviceId = deviceId,
        ),
        excerpt(
            id = ids.sq3rExcerptId,
            userId = userId,
            content = "SQ3R阅读法包含纵览、提问、阅读、背诵和复习。",
            url = null,
            sourceTitle = "阅读方法实践",
            userThought = "[[id:${ids.readingNodeId}|📖 阅读方法]] 里可以引用这一条。",
            treeNodeId = ids.readingNodeId,
            createTime = now - 32_000,
            updateTime = now - 32_000,
            deviceId = deviceId,
        ),
        excerpt(
            id = ids.feynmanExcerptId,
            userId = userId,
            content = "费曼技巧的核心就是用最简单的语言把复杂概念解释给外行听。",
            url = null,
            sourceTitle = "学习方法卡片",
            userThought = "适合和 [[id:${ids.readingNodeId}|📖 阅读方法]] 互相链接。",
            treeNodeId = ids.readingNodeId,
            createTime = now - 31_000,
            updateTime = now - 31_000,
            deviceId = deviceId,
        ),
    )

    private fun excerptTags(
        now: Long,
        userId: String,
        deviceId: String?,
        ids: SampleIds,
    ): List<ExcerptTagEntity> = listOf(
        relation(ids.ragExcerptId, ids.aiTagId, userId, now, deviceId),
        relation(ids.ragExcerptId, ids.efficiencyTagId, userId, now, deviceId),
        relation(ids.promptExcerptId, ids.aiTagId, userId, now, deviceId),
        relation(ids.zettelkastenExcerptId, ids.knowledgeTagId, userId, now, deviceId),
        relation(ids.zettelkastenExcerptId, ids.methodTagId, userId, now, deviceId),
        relation(ids.sq3rExcerptId, ids.learningTagId, userId, now, deviceId),
        relation(ids.feynmanExcerptId, ids.learningTagId, userId, now, deviceId),
    )

    private fun node(
        id: String,
        userId: String,
        parentId: String?,
        title: String,
        outlineMarkdown: String,
        createTime: Long,
        updateTime: Long,
        deviceId: String?,
        sortOrder: Long,
    ) = KnowledgeTreeNodeEntity(
        id = id,
        userId = userId,
        parentId = parentId,
        nodeTitle = title,
        outlineMarkdown = outlineMarkdown,
        createTime = createTime,
        updateTime = updateTime,
        deviceId = deviceId,
        syncStatus = SyncStatus.PENDING_CREATE,
        localDirtyTime = updateTime,
        sortOrder = sortOrder,
    )

    private fun tag(
        id: String,
        userId: String,
        tagName: String,
        heatWeight: Int,
        now: Long,
        deviceId: String?,
    ) = TagEntity(
        id = id,
        userId = userId,
        tagName = tagName,
        colorIcon = null,
        heatWeight = heatWeight,
        createTime = now,
        updateTime = now,
        deviceId = deviceId,
        syncStatus = SyncStatus.PENDING_CREATE,
        localDirtyTime = now,
    )

    private fun excerpt(
        id: String,
        userId: String,
        content: String,
        url: String?,
        sourceTitle: String?,
        userThought: String?,
        treeNodeId: String?,
        createTime: Long,
        updateTime: Long,
        deviceId: String?,
    ) = ExcerptEntity(
        id = id,
        userId = userId,
        content = content,
        url = url,
        sourceTitle = sourceTitle,
        userThought = userThought,
        treeNodeId = treeNodeId,
        createTime = createTime,
        updateTime = updateTime,
        deviceId = deviceId,
        syncStatus = SyncStatus.PENDING_CREATE,
        localDirtyTime = updateTime,
    )

    private fun relation(
        excerptId: String,
        tagId: String,
        userId: String,
        now: Long,
        deviceId: String?,
    ) = ExcerptTagEntity(
        id = EntityIdGenerator.newRelationId(),
        userId = userId,
        excerptId = excerptId,
        tagId = tagId,
        createTime = now,
        updateTime = now,
        deviceId = deviceId,
        syncStatus = SyncStatus.PENDING_CREATE,
        localDirtyTime = now,
    )
}

data class SampleWorkspaceData(
    val nodes: List<KnowledgeTreeNodeEntity>,
    val tags: List<TagEntity>,
    val excerpts: List<ExcerptEntity>,
    val excerptTags: List<ExcerptTagEntity>,
)

private data class SampleIds(
    val growthNodeId: String,
    val timeNodeId: String,
    val readingNodeId: String,
    val techNodeId: String,
    val investNodeId: String,
    val aiTagId: String,
    val efficiencyTagId: String,
    val knowledgeTagId: String,
    val methodTagId: String,
    val frontendTagId: String,
    val backendTagId: String,
    val psychologyTagId: String,
    val randomTagId: String,
    val learningTagId: String,
    val ragExcerptId: String,
    val promptExcerptId: String,
    val zettelkastenExcerptId: String,
    val sq3rExcerptId: String,
    val feynmanExcerptId: String,
) {
    companion object {
        fun create(): SampleIds = SampleIds(
            growthNodeId = EntityIdGenerator.newNodeId(),
            timeNodeId = EntityIdGenerator.newNodeId(),
            readingNodeId = EntityIdGenerator.newNodeId(),
            techNodeId = EntityIdGenerator.newNodeId(),
            investNodeId = EntityIdGenerator.newNodeId(),
            aiTagId = EntityIdGenerator.newTagId(),
            efficiencyTagId = EntityIdGenerator.newTagId(),
            knowledgeTagId = EntityIdGenerator.newTagId(),
            methodTagId = EntityIdGenerator.newTagId(),
            frontendTagId = EntityIdGenerator.newTagId(),
            backendTagId = EntityIdGenerator.newTagId(),
            psychologyTagId = EntityIdGenerator.newTagId(),
            randomTagId = EntityIdGenerator.newTagId(),
            learningTagId = EntityIdGenerator.newTagId(),
            ragExcerptId = EntityIdGenerator.newDraftExcerptId(),
            promptExcerptId = EntityIdGenerator.newDraftExcerptId(),
            zettelkastenExcerptId = EntityIdGenerator.newDraftExcerptId(),
            sq3rExcerptId = EntityIdGenerator.newDraftExcerptId(),
            feynmanExcerptId = EntityIdGenerator.newDraftExcerptId(),
        )
    }
}
