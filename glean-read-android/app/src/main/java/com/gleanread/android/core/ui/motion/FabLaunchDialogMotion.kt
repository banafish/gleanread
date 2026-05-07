package com.gleanread.android.core.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.fabLaunchDialogMotion(): Modifier {
    val visibleState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }
    val transition = rememberTransition(visibleState, label = "fab_launch_dialog")
    val scale by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = FabLaunchDialogScaleDurationMillis,
                easing = FastOutSlowInEasing,
            )
        },
        label = "fab_launch_dialog_scale",
    ) { isVisible ->
        if (isVisible) 1f else FabLaunchDialogInitialScale
    }
    val alpha by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = FabLaunchDialogAlphaDurationMillis,
                easing = FastOutSlowInEasing,
            )
        },
        label = "fab_launch_dialog_alpha",
    ) { isVisible ->
        if (isVisible) 1f else 0f
    }

    return graphicsLayer {
        this.alpha = alpha
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(1f, 1f)
    }
}

private const val FabLaunchDialogInitialScale = 0.88f
private const val FabLaunchDialogScaleDurationMillis = 220
private const val FabLaunchDialogAlphaDurationMillis = 140
