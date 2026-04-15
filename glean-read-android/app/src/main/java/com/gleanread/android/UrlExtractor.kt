package com.gleanread.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle

object UrlExtractor {
    private val urlRegex = Regex("""https?://[^\s\u4e00-\u9fff"'<>]+""")

    fun extract(intent: Intent): String? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        val htmlText = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.trim().orEmpty()
        val dataString = intent.dataString?.trim().orEmpty()
        val stream = getUriFromIntent(intent)

        if (text.isValidUrl()) {
            return text
        }

        if (subject.isValidUrl()) {
            return subject
        }

        stream?.toString()?.trim()?.takeIf { it.isValidUrl() }?.let {
            return it
        }

        extractFromClipData(intent)?.let {
            return it
        }

        dataString.takeIf { it.isValidUrl() }?.let {
            return it
        }

        htmlText.extractFirstUrlCandidate()?.let {
            return it
        }

        extractFromExtras(intent.extras)?.let {
            return it
        }

        if (text.isNotEmpty()) {
            urlRegex.findAll(text).lastOrNull()?.value?.let { candidateUrl ->
                val trimmedText = text.trimEnd()
                if (trimmedText.endsWith(candidateUrl) ||
                    trimmedText.endsWith(candidateUrl.trimEnd('/', '.'))
                ) {
                    return candidateUrl
                }
            }

            text.extractFirstUrlCandidate()?.let {
                return it
            }
        }

        if (subject.isNotEmpty()) {
            subject.extractFirstUrlCandidate()?.let {
                return it
            }
        }

        return null
    }

    private fun extractFromClipData(intent: Intent): String? {
        val clipData = intent.clipData ?: return null
        for (index in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(index)
            item.uri?.toString()?.trim()?.takeIf { it.isValidUrl() }?.let { return it }
            item.text?.toString()?.trim()?.takeIf { it.isValidUrl() }?.let { return it }
            item.htmlText?.extractFirstUrlCandidate()?.let { return it }
            item.text?.toString()?.extractFirstUrlCandidate()?.let { return it }
        }
        return null
    }

    private fun extractFromExtras(extras: Bundle?): String? {
        val safeExtras = extras ?: return null
        for (key in safeExtras.keySet()) {
            val value = safeExtras.get(key) ?: continue
            when (value) {
                is String -> {
                    value.trim().takeIf { it.isValidUrl() }?.let { return it }
                    value.extractFirstUrlCandidate()?.let { return it }
                }

                is CharSequence -> {
                    value.toString().trim().takeIf { it.isValidUrl() }?.let { return it }
                    value.toString().extractFirstUrlCandidate()?.let { return it }
                }

                is Uri -> {
                    value.toString().trim().takeIf { it.isValidUrl() }?.let { return it }
                }
            }
        }
        return null
    }

    private fun String.extractFirstUrlCandidate(): String? {
        if (isBlank()) return null
        return urlRegex.find(this)?.value
    }

    @Suppress("DEPRECATION")
    private fun getUriFromIntent(intent: Intent): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.isValidUrl(): Boolean {
        return startsWith("http://") || startsWith("https://")
    }
}
