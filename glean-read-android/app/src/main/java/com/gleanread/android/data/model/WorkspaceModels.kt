package com.gleanread.android.data.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.gleanread.android.ui.CaptureUI

const val LOCAL_USER_ID = "local-user"

enum class SyncStatus(val code: Int) {
    SYNCED(0),
    PENDING_CREATE(1),
    PENDING_UPDATE(2),
    PENDING_DELETE(3);

    companion object {
        fun bump(current: Int): Int {
            return if (current == PENDING_CREATE.code) PENDING_CREATE.code else PENDING_UPDATE.code
        }
    }
}

enum class LinkSuggestionType {
    NODE,
    EXCERPT,
}

enum class BacklinkType {
    NODE,
    EXCERPT,
}

enum class GraphNodeKind {
    CURRENT_NODE,
    LINKED_NODE,
    BACKLINK_NODE,
    EXCERPT,
}

data class ExcerptUiModel(
    val id: String,
    val content: String,
    val thought: String,
    val url: String?,
    val sourceTitle: String?,
    val tags: List<String>,
    val archivedNodeId: String?,
    val archivedNodeTitle: String?,
    val createTime: Long,
)

data class FlatNodeUiModel(
    val id: String,
    val parentId: String?,
    val title: String,
    val outlineMarkdown: String,
    val excerptIds: List<String>,
    val excerptCount: Int,
)

data class TreeNodeUiModel(
    val id: String,
    val title: String,
    val count: Int,
    val children: List<TreeNodeUiModel>,
)

data class TagUiModel(
    val id: String,
    val folder: String,
    val displayName: String,
    val fullName: String,
    val heatWeight: Int,
)

data class TagGroupUiModel(
    val folder: String,
    val count: Int,
    val items: List<TagUiModel>,
)

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

data class BacklinkUiModel(
    val sourceId: String,
    val title: String,
    val sourceType: BacklinkType,
    val snippet: String,
)

data class GraphUiNode(
    val id: String,
    val title: String,
    val kind: GraphNodeKind,
)

data class GraphUiEdge(
    val fromId: String,
    val toId: String,
)

data class GraphUiModel(
    val nodes: List<GraphUiNode>,
    val edges: List<GraphUiEdge>,
)

data class SuggestedTagUiModel(
    val fullName: String,
    val label: String,
)

data class WorkspaceSnapshot(
    val isEmpty: Boolean,
    val excerpts: List<ExcerptUiModel>,
    val treeRoots: List<TreeNodeUiModel>,
    val flatNodes: Map<String, FlatNodeUiModel>,
    val excerptsById: Map<String, ExcerptUiModel>,
    val tagGroups: List<TagGroupUiModel>,
    val backlinksByNodeId: Map<String, List<BacklinkUiModel>>,
    val graphByNodeId: Map<String, GraphUiModel>,
    val suggestedTags: List<SuggestedTagUiModel>,
) {
    companion object {
        val Empty = WorkspaceSnapshot(
            isEmpty = true,
            excerpts = emptyList(),
            treeRoots = emptyList(),
            flatNodes = emptyMap(),
            excerptsById = emptyMap(),
            tagGroups = emptyList(),
            backlinksByNodeId = emptyMap(),
            graphByNodeId = emptyMap(),
            suggestedTags = emptyList(),
        )
    }
}

data class OutlineDraft(
    val title: String,
    val markdown: String,
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

fun buildInlineAnnotatedString(rawText: String): AnnotatedString {
    val structuredLinks = extractStructuredLinks(rawText)
    val display = toDisplayInlineText(rawText)
    val builder = AnnotatedString.Builder(display)
    var searchStart = 0

    structuredLinks.forEach { link ->
        val token = "[[${link.title}]]"
        val start = display.indexOf(token, startIndex = searchStart)
        if (start >= 0) {
            val end = start + token.length
            builder.addStyle(
                SpanStyle(
                    color = CaptureUI.Indigo600,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                    fontSize = 15.sp,
                ),
                start,
                end,
            )
            builder.addStringAnnotation("inline-link", link.targetId, start, end)
            searchStart = end
        }
    }

    displayLinkRegex.findAll(display).forEach { match ->
        builder.addStyle(
            SpanStyle(
                color = CaptureUI.Indigo600,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                fontSize = 15.sp,
            ),
            match.range.first,
            match.range.last + 1,
        )
    }

    return builder.toAnnotatedString()
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

fun excerptTitleFallback(excerpt: ExcerptUiModel): String {
    return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
        ?: excerpt.content.take(18).trim() + if (excerpt.content.length > 18) "..." else ""
}
