package us.huseli.retain.compose

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun SweepLoadingOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()

    val startX by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart,
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        startX + 0f to Color.Transparent,
                        startX + 0.2f to Color.White,
                        startX + 0.4f to Color.Transparent,
                    ),
                ),
                alpha = 0.2f,
            )
    )
}
