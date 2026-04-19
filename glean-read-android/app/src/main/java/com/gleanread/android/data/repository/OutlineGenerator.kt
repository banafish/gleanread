package com.gleanread.android.data.repository

fun interface OutlineGenerator {
    suspend fun generate(excerpts: List<String>): OutlineDraft
}

class LocalOutlineGenerator : OutlineGenerator {
    override suspend fun generate(excerpts: List<String>): OutlineDraft {
        val topic = excerpts.firstOrNull()
            ?.split(' ', '，', '。', '、')
            ?.firstOrNull { it.isNotBlank() }
            ?.take(12)
            ?: "知识整理"

        val bullets = excerpts.take(5).mapIndexed { index, text ->
            "${index + 1}. ${text.take(42).trim()}${if (text.length > 42) "..." else ""}"
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
}
