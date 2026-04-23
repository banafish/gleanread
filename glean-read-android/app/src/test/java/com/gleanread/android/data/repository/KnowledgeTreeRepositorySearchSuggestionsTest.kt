package com.gleanread.android.data.repository

import com.gleanread.android.core.richtext.LinkSuggestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KnowledgeTreeRepositorySearchSuggestionsTest {
    @Test
    fun `empty inline query should still return local suggestions`() {
        val suggestions = searchSuggestionsForInlineQuery(
            query = "",
            nodeSuggestions = listOf(
                LocalSuggestionCandidate("node-1", "阅读方法", "知识树节点", LinkSuggestionType.NODE),
                LocalSuggestionCandidate("node-2", "时间管理", "知识树节点", LinkSuggestionType.NODE),
            ),
            excerptSuggestions = listOf(
                LocalSuggestionCandidate("excerpt-1", "Prompt Guide", "提示词工程的5个核心要素", LinkSuggestionType.EXCERPT),
            ),
        )

        assertFalse(suggestions.isEmpty())
        assertEquals("node-1", suggestions.first().id)
    }
}
