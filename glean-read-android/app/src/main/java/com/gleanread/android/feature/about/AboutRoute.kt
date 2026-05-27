package com.gleanread.android.feature.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun AboutRoute(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    AboutScreen(
        versionName = versionName,
        onBackClick = onBack,
    )
}
