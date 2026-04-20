package com.gleanread.android.core.model

import com.gleanread.android.core.richtext.extractStructuredLinks
import com.gleanread.android.core.richtext.toDisplayInlineText
import com.gleanread.android.data.local.ExcerptEntity
import com.gleanread.android.data.local.KnowledgeTreeNodeEntity

internal class BacklinkBuilder {
    fun build(
        nodes: List<KnowledgeTreeNodeEntity>,
        excerpts: List<ExcerptEntity>,
    ): Map<String, List<BacklinkUiModel>> {
        val backlinks = mutableMapOf<String, MutableList<BacklinkUiModel>>()

        nodes.forEach { node ->
            val text = node.outlineMarkdown.orEmpty()
            extractStructuredLinks(text).forEach { link ->
                backlinks.getOrPut(link.targetId) { mutableListOf() }.add(
                    BacklinkUiModel(
                        sourceId = node.id,
                        title = node.nodeTitle,
                        sourceType = BacklinkType.NODE,
                        snippet = toDisplay(text),
                    ),
                )
            }
        }

        excerpts.forEach { excerpt ->
            listOf(excerpt.content, excerpt.userThought.orEmpty()).forEach { text ->
                extractStructuredLinks(text).forEach { link ->
                    backlinks.getOrPut(link.targetId) { mutableListOf() }.add(
                        BacklinkUiModel(
                            sourceId = excerpt.id,
                            title = excerptTitle(excerpt),
                            sourceType = BacklinkType.EXCERPT,
                            snippet = toDisplay(text),
                        ),
                    )
                }
            }
        }

        return backlinks.mapValues { (_, items) -> items.distinctBy { it.sourceId } }
    }

    private fun excerptTitle(excerpt: ExcerptEntity): String {
        return excerpt.sourceTitle?.takeIf { it.isNotBlank() }
            ?: excerpt.content.take(18).trim() + if (excerpt.content.length > 18) "..." else ""
    }

    private fun toDisplay(text: String): String = toDisplayInlineText(text).take(80)
}
