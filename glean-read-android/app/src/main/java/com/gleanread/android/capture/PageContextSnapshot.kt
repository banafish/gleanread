package com.gleanread.android.capture

data class PageContextSnapshot(
    val sourcePackage: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val capturedAt: Long,
    val captureSource: String,
    val confidence: Float,
)

object PageContextSupport {
    const val AccessibilityCaptureSource = "accessibility"
    const val CacheTtlMillis = 60_000L

    const val ChromePackage = "com.android.chrome"
    const val FirefoxPackage = "org.mozilla.firefox"
    const val EdgePackage = "com.microsoft.emmx"
    const val SamsungInternetPackage = "com.sec.android.app.sbrowser"
    const val WeChatPackage = "com.tencent.mm"

    private val supportedPackages = setOf(
        ChromePackage,
        FirefoxPackage,
        EdgePackage,
        SamsungInternetPackage,
        WeChatPackage,
    )

    fun isSupportedPackage(packageName: String): Boolean {
        return supportedPackages.contains(packageName)
    }
}
