package com.gleanread.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceModelsTest {
    @Test
    fun `currentInlineQuery keeps empty query after trigger`() {
        assertEquals("", currentInlineQuery("[[", 2))
        assertEquals("", currentInlineQuery("正文 [[", 5))
    }

    @Test
    fun `currentInlineQuery returns null after closing token`() {
        assertNull(currentInlineQuery("[[已完成]]", 8))
    }

    @Test
    fun `buildInlineAnnotatedString annotates repeated visible titles with different targets`() {
        val annotated = buildInlineAnnotatedString("[[id:node-1|阅读方法]] 和 [[id:node-2|阅读方法]]")

        val first = annotated.getStringAnnotations("inline-link", 0, 0).firstOrNull()
        val secondTitleStart = annotated.text.lastIndexOf("[[阅读方法]]")
        val second = annotated.getStringAnnotations("inline-link", secondTitleStart, secondTitleStart).firstOrNull()

        assertEquals("node-1", first?.item)
        assertEquals("node-2", second?.item)
    }

    @Test
    fun `buildInlineAnnotatedString still styles plain links when structured links exist`() {
        val annotated = buildInlineAnnotatedString("[[id:node-1|阅读方法]] 和 [[稍后整理]]")
        val plainStart = annotated.text.indexOf("[[稍后整理]]")

        assertTrue(annotated.spanStyles.any { range ->
            range.start <= plainStart && range.end >= plainStart + "[[稍后整理]]".length
        })
    }
}
