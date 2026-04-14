package com.gleanread.android.capture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PageContextAccessibilityService : AccessibilityService() {
    private lateinit var pageContextStore: PageContextStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        pageContextStore = PageContextStore(this)
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        val packageName = safeEvent.packageName?.toString().orEmpty()
        if (!PageContextSupport.isSupportedPackage(packageName)) return

        val roots = linkedSetOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let(roots::add)
        safeEvent.source?.let(roots::add)
        windows.orEmpty()
            .mapNotNull { it.root }
            .forEach(roots::add)
        if (roots.isEmpty()) return

        val snapshot = PageContextNodeExtractor.extract(
            roots = roots,
            event = safeEvent,
            sourcePackage = packageName,
        ) ?: return

        pageContextStore.save(snapshot)
    }

    override fun onInterrupt() = Unit
}

private object PageContextNodeExtractor {
    private val urlRegex = Regex("""https?://[^\s]+""")
    private val hostRegex = Regex("""(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(?:/[^\s]*)?""")

    fun extract(
        roots: Iterable<AccessibilityNodeInfo>,
        event: AccessibilityEvent,
        sourcePackage: String,
    ): PageContextSnapshot? {
        val nodes = mutableListOf<NodeText>()
        roots.forEach { root -> collectNodes(root, nodes) }

        val title = extractTitle(sourcePackage, event, nodes)
        val url = extractUrl(sourcePackage, event, nodes)
        if (title.isBlank() && url.isBlank()) return null

        val confidence = when {
            title.isNotBlank() && url.isNotBlank() -> 0.95f
            else -> 0.7f
        }

        return PageContextSnapshot(
            sourcePackage = sourcePackage,
            sourceTitle = title,
            sourceUrl = url,
            capturedAt = System.currentTimeMillis(),
            captureSource = PageContextSupport.AccessibilityCaptureSource,
            confidence = confidence,
        )
    }

    private fun extractTitle(
        sourcePackage: String,
        event: AccessibilityEvent,
        nodes: List<NodeText>,
    ): String {
        val eventDescription = event.contentDescription?.toString().orEmpty().trim()
        if (PageContextTextRules.isMeaningfulTitle(eventDescription) && !looksLikeUrl(eventDescription)) {
            return eventDescription
        }

        val eventTexts = event.text.orEmpty()
            .map { it.toString().trim() }
            .filter { PageContextTextRules.isMeaningfulTitle(it) && !looksLikeUrl(it) }

        val nodeTitle = nodes
            .asSequence()
            .filter { it.isTitleCandidate(sourcePackage) && PageContextTextRules.isMeaningfulTitle(it.text) }
            .sortedByDescending { it.titleScore(sourcePackage) }
            .map { it.text }
            .firstOrNull()

        return nodeTitle
            ?: eventTexts.firstOrNull()
            ?: nodes.asSequence()
                .filter { PageContextTextRules.isMeaningfulTitle(it.text) && !looksLikeUrl(it.text) }
                .sortedByDescending { it.titleScore(sourcePackage) }
                .map { it.text }
                .firstOrNull()
            .orEmpty()
    }

    private fun extractUrl(
        sourcePackage: String,
        event: AccessibilityEvent,
        nodes: List<NodeText>,
    ): String {
        val prioritized = when (sourcePackage) {
            PageContextSupport.WeChatPackage -> {
                nodes.filter { it.isHighConfidenceWeChatUrlNode() } + nodes
            }

            else -> {
                nodes.filter { it.isHighConfidenceBrowserUrlNode() } + nodes
            }
        }

        val eventCandidates = buildList {
            addAll(event.text.orEmpty().map { it.toString() })
            event.contentDescription?.toString()?.let(::add)
        }

        return prioritized.firstNotNullOfOrNull { node ->
            normalizeUrl(node.text).ifBlank { normalizeUrl(node.contentDescription) }
        }.orEmpty().ifBlank {
            eventCandidates.firstNotNullOfOrNull(::normalizeUrl).orEmpty()
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        urlRegex.find(trimmed)?.value?.let { return it.trimEnd('.', ',', ';') }

        val hostMatch = hostRegex.find(trimmed)?.value.orEmpty()
        return when {
            hostMatch.startsWith("mp.weixin.qq.com") -> "https://$hostMatch"
            looksLikeBrowserHost(hostMatch) -> "https://$hostMatch"
            else -> ""
        }
    }

    private fun looksLikeUrl(value: String): Boolean {
        return value.startsWith("http://") || value.startsWith("https://") || looksLikeBrowserHost(value)
    }

    private fun looksLikeBrowserHost(value: String): Boolean {
        val trimmed = value.trim().trimEnd('/')
        return trimmed.contains('.') &&
            !trimmed.contains(' ') &&
            !trimmed.startsWith("android.view")
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        destination: MutableList<NodeText>,
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        destination += NodeText(
            viewId = node.viewIdResourceName.orEmpty(),
            text = node.text?.toString().orEmpty().trim(),
            contentDescription = node.contentDescription?.toString().orEmpty().trim(),
            className = node.className?.toString().orEmpty(),
            boundsTop = bounds.top,
        )

        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectNodes(child, destination)
            }
        }
    }
}

private data class NodeText(
    val viewId: String,
    val text: String,
    val contentDescription: String,
    val className: String,
    val boundsTop: Int,
) {
    fun isHighConfidenceBrowserUrlNode(): Boolean {
        val normalizedId = viewId.lowercase()
        return normalizedId.contains("url") ||
            normalizedId.contains("search_box") ||
            normalizedId.contains("location") ||
            normalizedId.contains("omnibox") ||
            normalizedId.contains("address")
    }

    fun isHighConfidenceWeChatUrlNode(): Boolean {
        val normalizedId = viewId.lowercase()
        return normalizedId.contains("url") ||
            normalizedId.contains("address") ||
            normalizedId.contains("toolbar") ||
            text.contains("mp.weixin.qq.com") ||
            contentDescription.contains("mp.weixin.qq.com")
    }

    fun isTitleCandidate(sourcePackage: String): Boolean {
        if (text.isBlank()) return false
        if (boundsTop !in 0..520) return false
        if (looksLikeActionLabel()) return false

        return when (sourcePackage) {
            PageContextSupport.ChromePackage,
            PageContextSupport.FirefoxPackage,
            PageContextSupport.EdgePackage,
            PageContextSupport.SamsungInternetPackage,
            PageContextSupport.WeChatPackage,
            -> isHighConfidenceTitleNode() || boundsTop in 80..420

            else -> isHighConfidenceTitleNode()
        }
    }

    fun titleScore(sourcePackage: String): Int {
        val normalizedId = viewId.lowercase()
        val normalizedClass = className.lowercase()

        var score = 0
        if (normalizedId.contains("title")) score += 120
        if (normalizedId.contains("header")) score += 80
        if (normalizedId.contains("toolbar")) score += 40
        if (normalizedClass.contains("textview")) score += 20
        if (boundsTop in 120..320) score += 40 else if (boundsTop in 0..420) score += 20
        if (text.length in 8..64) score += 20 else if (text.length in 5..100) score += 10
        if (normalizedId.contains("url") || normalizedId.contains("address")) score -= 80
        if (sourcePackage == PageContextSupport.ChromePackage && boundsTop in 140..320) score += 20
        return score
    }

    private fun isHighConfidenceTitleNode(): Boolean {
        val normalizedId = viewId.lowercase()
        val normalizedClass = className.lowercase()
        return boundsTop in 0..400 && (
            normalizedId.contains("title") ||
                normalizedId.contains("toolbar") ||
                normalizedId.contains("header") ||
                normalizedClass.contains("textview")
            )
    }

    private fun looksLikeActionLabel(): Boolean {
        return PageContextTextRules.isMeaningfulTitle(text).not() ||
            PageContextTextRules.isMeaningfulTitle(contentDescription).not() &&
            contentDescription.isNotBlank() &&
            text.isBlank()
    }
}
