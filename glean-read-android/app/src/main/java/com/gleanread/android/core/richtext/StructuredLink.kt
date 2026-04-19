package com.gleanread.android.core.richtext

enum class LinkSuggestionType {
    NODE,
    EXCERPT,
}

data class LinkSuggestion(
    val id: String,
    val title: String,
    val preview: String,
    val type: LinkSuggestionType,
)

data class ParsedInlineLink(
    val targetId: String,
    val title: String,
    val start: Int,
    val endExclusive: Int,
)

private val structuredLinkRegex = Regex("\\[\\[id:([^\\]|]+)\\|([^\\]]+)]]")
private val displayLinkRegex = Regex("\\[\\[([^\\]]+)]]")

fun extractStructuredLinks(text: String): List<ParsedInlineLink> {
    return structuredLinkRegex.findAll(text).map { match ->
        ParsedInlineLink(
            targetId = match.groupValues[1],
            title = match.groupValues[2],
            start = match.range.first,
            endExclusive = match.range.last + 1,
        )
    }.toList()
}

fun toDisplayInlineText(text: String): String {
    return structuredLinkRegex.replace(text) { match ->
        "[[${match.groupValues[2]}]]"
    }
}

fun currentInlineQuery(text: String, cursor: Int): String? {
    if (cursor < 0 || cursor > text.length) return null
    val beforeCursor = text.substring(0, cursor)
    val triggerIndex = beforeCursor.lastIndexOf("[[")
    if (triggerIndex < 0) return null
    val chunk = beforeCursor.substring(triggerIndex)
    if (chunk.contains("]]")) return null
    if (chunk.contains('\n')) return null
    return chunk.removePrefix("[[")
}

fun insertStructuredLink(text: String, cursor: Int, suggestion: LinkSuggestion): Pair<String, Int> {
    val beforeCursor = text.substring(0, cursor)
    val triggerIndex = beforeCursor.lastIndexOf("[[")
    val raw = "[[id:${suggestion.id}|${suggestion.title}]]"
    if (triggerIndex < 0) {
        return (text + raw) to (text.length + raw.length)
    }
    val updated = text.replaceRange(triggerIndex, cursor, raw)
    return updated to (triggerIndex + raw.length)
}

fun displayLinkRanges(text: String): List<IntRange> {
    return displayLinkRegex.findAll(text).map { it.range }.toList()
}
