package com.gleanread.android.capture

import android.content.Intent
import android.net.Uri
import com.gleanread.android.UrlExtractor

data class CaptureSeed(
    val content: String,
    val url: String,
    val sourceTitle: String,
    val sourcePackage: String,
    val usedCachedUrl: Boolean,
    val usedCachedTitle: Boolean,
)

class CaptureSeedResolver(
    private val pageContextStore: PageContextStore,
) {
    fun resolve(
        intent: Intent?,
        referrer: Uri?,
        now: Long = System.currentTimeMillis(),
    ): CaptureSeed {
        val explicitContent = resolveExplicitContent(intent).trim()
        val explicitUrl = intent?.let(UrlExtractor::extract).orEmpty().trim()
        val explicitTitle = resolveExplicitTitle(intent, explicitContent, explicitUrl)
        val sourcePackage = resolveSourcePackage(intent, referrer)
        val snapshot = pageContextStore.readRecentSnapshot(
            expectedSourcePackage = sourcePackage,
            now = now,
        )

        val finalUrl = explicitUrl.ifBlank { snapshot?.sourceUrl.orEmpty() }
        val finalTitle = explicitTitle.ifBlank { snapshot?.sourceTitle.orEmpty() }
        val normalizedContent = when {
            explicitContent.isNotBlank() -> explicitContent
            finalTitle.isNotBlank() -> finalTitle
            else -> ""
        }

        return CaptureSeed(
            content = normalizedContent,
            url = finalUrl,
            sourceTitle = finalTitle,
            sourcePackage = sourcePackage.orEmpty(),
            usedCachedUrl = explicitUrl.isBlank() && finalUrl.isNotBlank(),
            usedCachedTitle = explicitTitle.isBlank() && finalTitle.isNotBlank(),
        )
    }

    private fun resolveExplicitContent(intent: Intent?): String {
        if (intent == null) return ""
        val action = intent.action.orEmpty()
        return when (action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
            }

            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                val url = UrlExtractor.extract(intent).orEmpty()
                if (text.trim() == url.trim() && PageContextTextRules.isMeaningfulShareSubject(subject)) {
                    subject
                } else {
                    text
                }
            }

            else -> {
                intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            }
        }
    }

    private fun resolveExplicitTitle(
        intent: Intent?,
        explicitContent: String,
        explicitUrl: String,
    ): String {
        val subject = intent?.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        return subject.takeUnless {
            it.isBlank() ||
                it == explicitContent ||
                it == explicitUrl ||
                !PageContextTextRules.isMeaningfulShareSubject(it)
        }.orEmpty()
    }

    private fun resolveSourcePackage(intent: Intent?, referrer: Uri?): String? {
        val referrerCandidates = listOfNotNull(
            referrer,
            intent?.getParcelableUriExtra(Intent.EXTRA_REFERRER),
            intent?.getStringExtra(Intent.EXTRA_REFERRER_NAME)?.let(Uri::parse),
        )

        return referrerCandidates
            .asSequence()
            .mapNotNull(::extractAndroidAppPackage)
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() }
    }

    @Suppress("DEPRECATION")
    private fun Intent.getParcelableUriExtra(name: String): Uri? {
        return getParcelableExtra(name)
    }

    private fun extractAndroidAppPackage(uri: Uri?): String? {
        return uri
            ?.takeIf { it.scheme == "android-app" }
            ?.host
    }
}
