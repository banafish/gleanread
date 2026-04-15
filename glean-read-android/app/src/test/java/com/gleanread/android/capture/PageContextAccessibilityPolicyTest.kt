package com.gleanread.android.capture

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageContextAccessibilityPolicyTest {
    @Test
    fun `window state changes are always processed`() {
        assertTrue(
            PageContextAccessibilityPolicy.shouldProcessEvent(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                contentChangeTypes = 0,
            ),
        )
    }

    @Test
    fun `content changes only process relevant updates`() {
        assertTrue(
            PageContextAccessibilityPolicy.shouldProcessEvent(
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT,
            ),
        )
        assertTrue(
            PageContextAccessibilityPolicy.shouldProcessEvent(
                eventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                contentChangeTypes = 0,
            ),
        )
        assertFalse(
            PageContextAccessibilityPolicy.shouldProcessEvent(
                eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
                contentChangeTypes = 0,
            ),
        )
    }

    @Test
    fun `duplicate snapshots inside refresh window skip store writes`() {
        val previous = snapshot(
            title = "page title",
            url = "https://example.com/article",
            capturedAt = 1_000L,
        )
        val current = snapshot(
            title = "page title",
            url = "https://example.com/article",
            capturedAt = 4_000L,
        )

        assertTrue(
            PageContextAccessibilityPolicy.shouldSkipStoreWrite(
                previous = previous,
                current = current,
            ),
        )
    }

    @Test
    fun `changed snapshots or stale refreshes still write`() {
        assertFalse(
            PageContextAccessibilityPolicy.shouldSkipStoreWrite(
                previous = snapshot(
                    title = "old title",
                    url = "https://example.com/article",
                    capturedAt = 1_000L,
                ),
                current = snapshot(
                    title = "new title",
                    url = "https://example.com/article",
                    capturedAt = 2_000L,
                ),
            ),
        )
        assertFalse(
            PageContextAccessibilityPolicy.shouldSkipStoreWrite(
                previous = snapshot(
                    title = "page title",
                    url = "https://example.com/article",
                    capturedAt = 1_000L,
                ),
                current = snapshot(
                    title = "page title",
                    url = "https://example.com/article",
                    capturedAt = 7_000L,
                ),
            ),
        )
    }

    private fun snapshot(
        title: String,
        url: String,
        capturedAt: Long,
    ) = PageContextSnapshot(
        sourcePackage = PageContextSupport.ChromePackage,
        sourceTitle = title,
        sourceUrl = url,
        capturedAt = capturedAt,
        captureSource = PageContextSupport.AccessibilityCaptureSource,
        confidence = 0.95f,
    )
}
