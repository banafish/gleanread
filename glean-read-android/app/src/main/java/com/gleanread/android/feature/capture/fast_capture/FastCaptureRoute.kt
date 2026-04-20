package com.gleanread.android.feature.capture.fast_capture

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gleanread.android.app.appContainer
import com.gleanread.android.platform.page_context.CaptureSeed
import com.gleanread.android.platform.page_context.PageContextAccessibilityState

@Composable
fun FastCaptureRoute(
    captureSeed: CaptureSeed,
    onDismiss: () -> Unit,
    fastCaptureViewModel: FastCaptureViewModel = viewModel(
        factory = LocalContext.current.appContainer.fastCaptureViewModelFactory,
    ),
) {
    val context = LocalContext.current
    val uiState by fastCaptureViewModel.uiState.collectAsStateWithLifecycle()
    val isAccessibilityEnabled = remember(context) {
        PageContextAccessibilityState.isEnabled(context)
    }

    FastCaptureScreen(
        captureSeed = captureSeed,
        uiState = uiState,
        isAccessibilityEnabled = isAccessibilityEnabled,
        onDismiss = onDismiss,
        onOpenAccessibilitySettings = {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
            )
        },
        onSaveQuickExcerpt = { thought, url, tagNames ->
            fastCaptureViewModel.saveQuickExcerpt(
                content = captureSeed.content,
                thought = thought,
                url = url,
                sourceTitle = captureSeed.sourceTitle,
                tagNames = tagNames,
            )
        },
    )
}

