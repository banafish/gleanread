package com.gleanread.android.feature.capture.fast_capture

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import com.gleanread.android.platform.page_context.CaptureSeedResolver
import com.gleanread.android.platform.page_context.PageContextStore
import com.gleanread.android.core.ui.theme.GleanReadTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gleanread.android.app.appContainer
import com.gleanread.android.data.appearance.ThemeMode
import com.gleanread.android.data.appearance.ThemeColor

class FastCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val captureSeed = CaptureSeedResolver(
            pageContextStore = PageContextStore(applicationContext),
        ).resolve(
            intent = intent,
            referrer = referrer,
        )

        setContent {
            SideEffect {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.setBackgroundBlurRadius(50)
                }
            }
            val themeMode by appContainer.appearancePreferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val themeColor by appContainer.appearancePreferencesRepository.themeColorFlow.collectAsState(initial = ThemeColor.DYNAMIC)
            GleanReadTheme(themeMode = themeMode, themeColor = themeColor) {
                FastCaptureRoute(
                    captureSeed = captureSeed,
                    onDismiss = ::finish,
                )
            }
        }
    }
}

