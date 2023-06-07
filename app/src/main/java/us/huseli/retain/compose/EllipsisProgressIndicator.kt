package us.huseli.retain.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Composable
fun EllipsisProgressIndicator(
    modifier: Modifier = Modifier,
    width: Dp,
    style: TextStyle = LocalTextStyle.current,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = -widthPx,
        targetValue = widthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Box(modifier = Modifier.width(width).clipToBounds()) {
        Text(
            text = "…",
            modifier = modifier.offset(x = with(density) { offset.toDp() }),
            style = style,
        )
    }
}
