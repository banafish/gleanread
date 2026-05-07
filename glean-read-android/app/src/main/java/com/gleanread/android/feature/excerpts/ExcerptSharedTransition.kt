package com.gleanread.android.feature.excerpts

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.excerptContainerSharedBounds(
    excerptId: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
        return this
    }

    return with(sharedTransitionScope) {
        this@excerptContainerSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = excerptContainerSharedTransitionKey(excerptId)),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

private fun excerptContainerSharedTransitionKey(excerptId: String): String {
    return "excerpt-container-$excerptId"
}
