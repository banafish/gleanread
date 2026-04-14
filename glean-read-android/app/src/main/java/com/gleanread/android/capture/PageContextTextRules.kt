package com.gleanread.android.capture

internal object PageContextTextRules {
    private val genericLabels = setOf(
        "including link:",
        "including text:",
        "sharing text",
        "shared text",
        "tap to see search results",
        "translate",
        "copy",
        "share",
        "select all",
        "包含链接:",
        "包含链接：",
        "包含文字:",
        "包含文字：",
        "分享文本",
        "已分享文本",
        "点按即可查看搜索结果",
        "翻译",
        "复制",
        "分享",
        "全选",
    )

    fun isMeaningfulShareSubject(text: String): Boolean {
        val normalized = normalize(text)
        return normalized.isNotBlank() && normalized !in genericLabels
    }

    fun isMeaningfulTitle(text: String): Boolean {
        val normalized = normalize(text)
        return normalized.isNotBlank() && normalized !in genericLabels
    }

    private fun normalize(text: String): String {
        return text.trim().lowercase()
    }
}
