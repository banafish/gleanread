package com.gleanread.android.feature.fast_capture

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import com.gleanread.android.capture.CaptureSeedResolver
import com.gleanread.android.capture.PageContextStore
import com.gleanread.android.ui.theme.GleanReadTheme

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
            GleanReadTheme {
                FastCaptureRoute(
                    captureSeed = captureSeed,
                    onDismiss = ::finish,
                )
            }
        }
    }
}
