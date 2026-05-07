package com.gleanread.android.feature.excerpts

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal const val NEW_EXCERPT_SHARED_BOUNDS_ID = "new-excerpt"

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
