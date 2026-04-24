package com.gleanread.android.data.repository

import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.ExcerptTagEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity
import com.gleanread.android.data.local.TagEntity
import com.gleanread.android.data.model.LOCAL_USER_ID
import com.gleanread.android.data.model.SyncStatus

object SampleSeedData {
    fun nodes(now: Long): List<KnowledgeTreeNodeEntity> = listOf(
        KnowledgeTreeNodeEntity("node-growth", LOCAL_USER_ID, null, "🧠 个人成长", "围绕个人成长建立长期知识体系。", now - 80_000, now - 30_000, syncStatus = SyncStatus.SYNCED),
        KnowledgeTreeNodeEntity("node-time", LOCAL_USER_ID, "node-growth", "⏱️ 时间管理", "聚焦高频时间管理方法。", now - 70_000, now - 30_000, syncStatus = SyncStatus.SYNCED),
        KnowledgeTreeNodeEntity("node-reading", LOCAL_USER_ID, "node-growth", "📖 阅读方法", "主题阅读是快速建立领域认知的核心方法。", now - 60_000, now - 20_000, syncStatus = SyncStatus.SYNCED),
        KnowledgeTreeNodeEntity("node-tech", LOCAL_USER_ID, null, "💻 技术开发", "记录技术栈、架构和产品实现经验。", now - 50_000, now - 20_000, syncStatus = SyncStatus.SYNCED),
        KnowledgeTreeNodeEntity("node-invest", LOCAL_USER_ID, null, "💰 投资理财", "整理投资框架和复盘笔记。", now - 40_000, now - 10_000, syncStatus = SyncStatus.SYNCED),
    )

    fun tags(now: Long): List<TagEntity> = listOf(
        tag("tag-ai", "AI/大模型", 18, now),
        tag("tag-eff", "效率工具", 12, now),
        tag("tag-knowledge", "知识管理", 15, now),
        tag("tag-method", "方法论", 10, now),
        tag("tag-tech-front", "技术/前端", 10, now),
        tag("tag-tech-back", "技术/后端", 15, now),
        tag("tag-read-psy", "阅读/心理学", 5, now),
        tag("tag-random", "无分类/随想", 8, now),
        tag("tag-learn", "学习方法", 7, now),
    )

    fun excerpts(now: Long): List<ExcerptEntity> = listOf(
        ExcerptEntity(
            id = "excerpt-rag",
            userId = LOCAL_USER_ID,
            content = "RAG技术的核心是将外部知识库与大模型结合，解决幻觉问题并提供实时数据。",
            url = "https://mp.weixin.qq.com/s/rag-demo",
            sourceTitle = "如何理解RAG架构",
            userThought = "这个可以用在我的知识库App搜索功能里。",
            treeNodeId = null,
            createTime = now - 35_000,
            updateTime = now - 35_000,
            syncStatus = SyncStatus.SYNCED,
        ),
        ExcerptEntity(
            id = "excerpt-prompt",
            userId = LOCAL_USER_ID,
            content = "提示词工程的5个核心要素是角色、上下文、任务、格式要求和示例。",
            url = "https://example.com/prompt-guide",
            sourceTitle = "Prompt Engineering Guide",
            userThought = "整理成给新人看的清单会很有用。",
            treeNodeId = null,
            createTime = now - 34_000,
            updateTime = now - 34_000,
            syncStatus = SyncStatus.SYNCED,
        ),
        ExcerptEntity(
            id = "excerpt-zk",
            userId = LOCAL_USER_ID,
            content = "卡片盒笔记法强调原子化记录，每张卡片只写一个不可分割的概念。",
            url = null,
            sourceTitle = "《卡片笔记作法》",
            userThought = "我需要把旧笔记拆分成原子级别。",
            treeNodeId = null,
            createTime = now - 33_000,
            updateTime = now - 33_000,
            syncStatus = SyncStatus.SYNCED,
        ),
        ExcerptEntity(
            id = "excerpt-sq3r",
            userId = LOCAL_USER_ID,
            content = "SQ3R阅读法包含纵览、提问、阅读、背诵和复习。",
            url = null,
            sourceTitle = "阅读方法实践",
            userThought = "[[id:node-reading|📖 阅读方法]] 里可以引用这一条。",
            treeNodeId = "node-reading",
            createTime = now - 32_000,
            updateTime = now - 32_000,
            syncStatus = SyncStatus.SYNCED,
        ),
        ExcerptEntity(
            id = "excerpt-feynman",
            userId = LOCAL_USER_ID,
            content = "费曼技巧的核心就是用最简单的语言把复杂概念解释给外行听。",
            url = null,
            sourceTitle = "学习方法卡片",
            userThought = "适合和 [[id:node-reading|📖 阅读方法]] 互相链接。",
            treeNodeId = "node-reading",
            createTime = now - 31_000,
            updateTime = now - 31_000,
            syncStatus = SyncStatus.SYNCED,
        ),
    )

    fun excerptTags(now: Long): List<ExcerptTagEntity> = listOf(
        relation("rel-1", "excerpt-rag", "tag-ai", now),
        relation("rel-2", "excerpt-rag", "tag-eff", now),
        relation("rel-3", "excerpt-prompt", "tag-ai", now),
        relation("rel-4", "excerpt-zk", "tag-knowledge", now),
        relation("rel-5", "excerpt-zk", "tag-method", now),
        relation("rel-6", "excerpt-sq3r", "tag-learn", now),
        relation("rel-7", "excerpt-feynman", "tag-learn", now),
    )

    private fun tag(id: String, tagName: String, heatWeight: Int, now: Long) = TagEntity(
        id = id,
        userId = LOCAL_USER_ID,
        tagName = tagName,
        colorIcon = null,
        heatWeight = heatWeight,
        createTime = now,
        updateTime = now,
        syncStatus = SyncStatus.SYNCED,
    )

    private fun relation(id: String, excerptId: String, tagId: String, now: Long) = ExcerptTagEntity(
        id = id,
        userId = LOCAL_USER_ID,
        excerptId = excerptId,
        tagId = tagId,
        createTime = now,
        updateTime = now,
        syncStatus = SyncStatus.SYNCED,
    )
}
