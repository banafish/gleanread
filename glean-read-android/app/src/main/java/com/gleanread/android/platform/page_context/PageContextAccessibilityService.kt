package com.gleanread.android.platform.page_context

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
            flags = flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS.inv()
            notificationTimeout = PageContextAccessibilityPolicy.NotificationTimeoutMillis
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        if (!PageContextAccessibilityPolicy.shouldProcessEvent(safeEvent.eventType, safeEvent.contentChangeTypes)) {
            return
        }

        val packageName = safeEvent.packageName?.toString().orEmpty()
        if (!PageContextSupport.isSupportedPackage(packageName)) return

        val roots = buildList {
            safeEvent.source
                ?.takeIf { it.packageName?.toString().orEmpty() == packageName }
                ?.let(::add)
            rootInActiveWindow
                ?.takeIf { it.packageName?.toString().orEmpty() == packageName }
                ?.let(::add)
        }
        if (roots.isEmpty()) return

        val previousSnapshot = pageContextStore.readRecentSnapshot(
            expectedSourcePackage = packageName,
            now = System.currentTimeMillis(),
        )
        val snapshot = PageContextNodeExtractor.extract(
            roots = roots,
            event = safeEvent,
            sourcePackage = packageName,
        ) ?: return
        val mergedSnapshot = mergeWithRecentSnapshot(
            previous = previousSnapshot,
            current = snapshot,
        )
        if (PageContextAccessibilityPolicy.shouldSkipStoreWrite(previousSnapshot, mergedSnapshot)) {
            return
        }
        pageContextStore.save(mergedSnapshot)
    }

    override fun onInterrupt() = Unit

    private fun mergeWithRecentSnapshot(
        previous: PageContextSnapshot?,
        current: PageContextSnapshot,
    ): PageContextSnapshot {
        if (previous == null) return current
        if (current.capturedAt - previous.capturedAt > MERGE_WINDOW_MILLIS) return current

        val mergedUrl = current.sourceUrl.ifBlank { previous.sourceUrl }
        val mergedTitle = chooseBetterTitle(
            currentTitle = current.sourceTitle,
            previousTitle = previous.sourceTitle,
        )
        val mergedConfidence = when {
            mergedTitle.isNotBlank() && mergedUrl.isNotBlank() -> 0.95f
            mergedTitle.isNotBlank() || mergedUrl.isNotBlank() -> maxOf(current.confidence, previous.confidence, 0.7f)
            else -> current.confidence
        }

        return if (mergedTitle == current.sourceTitle && mergedUrl == current.sourceUrl) {
            current
        } else {
            current.copy(
                sourceTitle = mergedTitle,
                sourceUrl = mergedUrl,
                confidence = mergedConfidence,
            )
        }
    }

    private fun chooseBetterTitle(
        currentTitle: String,
        previousTitle: String,
    ): String {
        if (currentTitle.isBlank()) return previousTitle
        if (previousTitle.isBlank()) return currentTitle

        val currentScore = currentTitle.titleQualityScore()
        val previousScore = previousTitle.titleQualityScore()
        return if (currentScore + 12 >= previousScore) {
            currentTitle
        } else {
            previousTitle
        }
    }

    private companion object {
        const val MERGE_WINDOW_MILLIS = 15_000L
    }
}

private object PageContextNodeExtractor {
    private val urlRegex = Regex("""https?://[^\s]+""")
    private val hostRegex = Regex("""(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(?:/[^\s]*)?""")
    private const val MAX_SCAN_DEPTH = 24
    private const val MAX_VISITED_NODES = 400
    private const val MAX_COLLECTED_NODES = 180

    fun extract(
        roots: Iterable<AccessibilityNodeInfo>,
        event: AccessibilityEvent,
        sourcePackage: String,
    ): PageContextSnapshot? {
        val nodes = mutableListOf<NodeText>()
        roots.forEach { root -> collectNodes(root, nodes) }
        val relevantNodes = nodes.filterNot(NodeText::belongsToShareSheet)

        val title = extractTitle(sourcePackage, event, relevantNodes.ifEmpty { nodes })
        val url = extractUrl(sourcePackage, event, relevantNodes.ifEmpty { nodes })
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

        val nodeUrl = prioritized.asSequence()
            .mapNotNull { node ->
                normalizeUrl(node.text)
                    .ifBlank { normalizeUrl(node.contentDescription) }
                    .takeIf { it.isNotBlank() }
            }
            .firstOrNull()

        return nodeUrl.orEmpty().ifBlank {
            eventCandidates.asSequence()
                .map(::normalizeUrl)
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
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
        val pending = ArrayDeque<NodeVisit>()
        pending.addLast(NodeVisit(node = node, depth = 0))
        var visitedCount = 0

        while (
            pending.isNotEmpty() &&
            visitedCount < MAX_VISITED_NODES &&
            destination.size < MAX_COLLECTED_NODES
        ) {
            val current = pending.removeFirst()
            visitedCount += 1

            if (current.depth > MAX_SCAN_DEPTH) continue

            val bounds = Rect()
            current.node.getBoundsInScreen(bounds)

            val viewId = current.node.viewIdResourceName.orEmpty()
            val text = current.node.text?.toString().orEmpty().trim()
            val contentDescription = current.node.contentDescription?.toString().orEmpty().trim()
            if (viewId.isNotBlank() || text.isNotBlank() || contentDescription.isNotBlank()) {
                destination += NodeText(
                    viewId = viewId,
                    text = text,
                    contentDescription = contentDescription,
                    className = current.node.className?.toString().orEmpty(),
                    boundsTop = bounds.top,
                )
            }

            if (current.depth == MAX_SCAN_DEPTH) continue

            for (index in 0 until current.node.childCount) {
                current.node.getChild(index)?.let { child ->
                    pending.addLast(NodeVisit(node = child, depth = current.depth + 1))
                }
            }
        }
    }

    private data class NodeVisit(
        val node: AccessibilityNodeInfo,
        val depth: Int,
    )

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
        if (normalizedClass.contains("webview")) score += 60
        if (boundsTop in 120..320) score += 40 else if (boundsTop in 0..420) score += 20
        if (text.length in 12..96) score += 30 else if (text.length in 8..120) score += 20 else if (text.length in 5..140) score += 10
        if (text.any(Char::isDigit)) score += 8
        if (text.contains(' ') || text.contains('（') || text.contains('(') || text.contains('：') || text.contains(':') || text.contains('丨') || text.contains('|')) score += 8
        if (normalizedId.contains("url") || normalizedId.contains("address")) score -= 80
        if (looksLikeNavigationLabel()) score -= 40
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

    fun belongsToShareSheet(): Boolean {
        val normalizedId = viewId.lowercase()
        val normalizedText = text.lowercase()
        val normalizedDesc = contentDescription.lowercase()
        return normalizedId.contains("intentresolver") ||
            normalizedId.contains("chooser") ||
            normalizedId.contains("resolver_list") ||
            normalizedText == "sharing text" ||
            normalizedText == "including link:" ||
            normalizedText == "copy without link" ||
            normalizedDesc == "copy text"
    }

    private fun looksLikeNavigationLabel(): Boolean {
        return text in navigationLabels || contentDescription in navigationLabels
    }

    private companion object {
        val navigationLabels = setOf(
            "首页",
            "上一级",
            "下一页",
            "返回",
            "菜单",
            "home",
            "back",
            "next",
            "menu",
        )
    }
}

private fun String.titleQualityScore(): Int {
    if (isBlank()) return Int.MIN_VALUE
    var score = length.coerceAtMost(80)
    if (any(Char::isDigit)) score += 8
    if (contains(' ') || contains('（') || contains('(') || contains('：') || contains(':') || contains('-')) score += 8
    if (contains('.') || contains('/')) score -= 12
    return score
}
