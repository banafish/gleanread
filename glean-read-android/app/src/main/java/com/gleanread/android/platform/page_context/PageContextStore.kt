package com.gleanread.android.platform.page_context

import android.content.Context

class PageContextStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun save(snapshot: PageContextSnapshot) {
        preferences.edit()
            .putString(KEY_SOURCE_PACKAGE, snapshot.sourcePackage)
            .putString(KEY_SOURCE_TITLE, snapshot.sourceTitle)
            .putString(KEY_SOURCE_URL, snapshot.sourceUrl)
            .putLong(KEY_CAPTURED_AT, snapshot.capturedAt)
            .putString(KEY_CAPTURE_SOURCE, snapshot.captureSource)
            .putFloat(KEY_CONFIDENCE, snapshot.confidence)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun readRecentSnapshot(
        expectedSourcePackage: String?,
        now: Long = System.currentTimeMillis(),
    ): PageContextSnapshot? {
        val capturedAt = preferences.getLong(KEY_CAPTURED_AT, 0L)
        if (capturedAt <= 0L || now - capturedAt > PageContextSupport.CacheTtlMillis) {
            return null
        }

        val sourcePackage = preferences.getString(KEY_SOURCE_PACKAGE, null).orEmpty()
        if (sourcePackage.isBlank() || !matchesExpectedPackage(sourcePackage, expectedSourcePackage)) {
            return null
        }

        return PageContextSnapshot(
            sourcePackage = sourcePackage,
            sourceTitle = preferences.getString(KEY_SOURCE_TITLE, null).orEmpty(),
            sourceUrl = preferences.getString(KEY_SOURCE_URL, null).orEmpty(),
            capturedAt = capturedAt,
            captureSource = preferences.getString(KEY_CAPTURE_SOURCE, null)
                ?: PageContextSupport.AccessibilityCaptureSource,
            confidence = preferences.getFloat(KEY_CONFIDENCE, 0f),
        )
    }

    private fun matchesExpectedPackage(
        snapshotPackage: String,
        expectedSourcePackage: String?,
    ): Boolean {
        val normalizedExpected = expectedSourcePackage?.trim().orEmpty()
        return if (normalizedExpected.isNotEmpty()) {
            snapshotPackage == normalizedExpected
        } else {
            PageContextSupport.isSupportedPackage(snapshotPackage)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "page_context_cache"
        const val KEY_SOURCE_PACKAGE = "source_package"
        const val KEY_SOURCE_TITLE = "source_title"
        const val KEY_SOURCE_URL = "source_url"
        const val KEY_CAPTURED_AT = "captured_at"
        const val KEY_CAPTURE_SOURCE = "capture_source"
        const val KEY_CONFIDENCE = "confidence"
    }
}
