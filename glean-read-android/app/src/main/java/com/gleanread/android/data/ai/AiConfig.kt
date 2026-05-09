package com.gleanread.android.data.ai

data class AiConfig(
    val baseUrl: String = "",
    val token: String = "",
    val model: String = "",
) {
    val isComplete: Boolean
        get() = normalizedBaseUrl().isNotBlank() &&
            token.isNotBlank() &&
            model.isNotBlank()

    fun normalizedBaseUrl(): String {
        return normalizeAiBaseUrl(baseUrl)
    }

    fun chatCompletionsUrl(): String {
        return buildOpenAiChatCompletionsUrl(baseUrl)
    }
}

internal fun normalizeAiBaseUrl(value: String): String {
    var normalized = value.trim()
    if (normalized.isBlank()) return ""
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
        normalized = "https://$normalized"
    }
    normalized = normalized.trimEnd('/')
    if (normalized.endsWith("/v1", ignoreCase = true)) {
        normalized = normalized.dropLast(3).trimEnd('/')
    }
    return normalized
}

internal fun buildOpenAiChatCompletionsUrl(baseUrl: String): String {
    val normalized = normalizeAiBaseUrl(baseUrl)
    return if (normalized.isBlank()) {
        ""
    } else {
        "$normalized/v1/chat/completions"
    }
}
