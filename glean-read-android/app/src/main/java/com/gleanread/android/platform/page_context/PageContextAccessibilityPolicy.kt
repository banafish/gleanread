package com.gleanread.android.platform.page_context

import android.view.accessibility.AccessibilityEvent

internal object PageContextAccessibilityPolicy {
    const val NotificationTimeoutMillis = 350L

    private const val InterestingContentChangeTypes =
        AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE

    private const val DuplicateSnapshotRefreshWindowMillis = 5_000L

    fun shouldProcessEvent(
        eventType: Int,
        contentChangeTypes: Int,
    ): Boolean {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                contentChangeTypes == 0 ||
                    contentChangeTypes and InterestingContentChangeTypes != 0
            }

            else -> false
        }
    }

    fun shouldSkipStoreWrite(
        previous: PageContextSnapshot?,
        current: PageContextSnapshot,
    ): Boolean {
        if (previous == null) return false

        val isSamePayload = previous.sourcePackage == current.sourcePackage &&
            previous.sourceTitle == current.sourceTitle &&
            previous.sourceUrl == current.sourceUrl
        if (!isSamePayload) return false

        return current.capturedAt - previous.capturedAt < DuplicateSnapshotRefreshWindowMillis
    }
}
