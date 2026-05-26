package com.gleanread.android.data.repository

import com.gleanread.android.data.model.OutlineDraft

data class AiExcerptInput(
    val content: String,
    val userThought: String? = null,
    val sourceTitle: String? = null,
    val url: String? = null,
)

fun interface OutlineGenerator {
    suspend fun generate(excerpts: List<AiExcerptInput>): OutlineDraft
}

class LocalOutlineGenerator : OutlineGenerator {
    override suspend fun generate(excerpts: List<AiExcerptInput>): OutlineDraft {
        val topic = excerpts.firstOrNull()?.content
            ?.split(' ', '，', '。', '、')
            ?.firstOrNull { it.isNotBlank() }
            ?.take(TOPIC_MAX_LENGTH)
            ?: DEFAULT_TOPIC

        val bullets = excerpts.take(BULLET_MAX_COUNT).mapIndexed { index, item ->
            val text = item.content
            "${index + 1}. ${text.take(BULLET_TEXT_MAX_LENGTH).trim()}${if (text.length > BULLET_TEXT_MAX_LENGTH) "..." else ""}"
        }

        return OutlineDraft(
            title = topic,
            markdown = buildString {
                appendLine("这组摘录围绕【$topic】展开，可先整理为以下结构：")
                bullets.forEach { appendLine(it) }
                appendLine()
                appendLine("可进一步补充关联节点、关键摘录与行动建议。")
            }.trim(),
        )
    }

    companion object {
        private const val TOPIC_MAX_LENGTH = 12
        private const val BULLET_MAX_COUNT = 5
        private const val BULLET_TEXT_MAX_LENGTH = 42
        private const val DEFAULT_TOPIC = "知识整理"
    }
}
