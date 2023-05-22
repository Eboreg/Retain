package us.huseli.retain.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import us.huseli.retain.Enums.Side

fun createOverscrollIndicatorOutline(size: Size, side: Side): Outline {
    val cornerRadius = CornerRadius(size.width, size.height / 2)
    val roundRect = when (side) {
        Side.LEFT -> RoundRect(rect = size.toRect(), topRight = cornerRadius, bottomRight = cornerRadius)
        Side.RIGHT -> RoundRect(rect = size.toRect(), topLeft = cornerRadius, bottomLeft = cornerRadius)
    }
    val path = Path().apply { addRoundRect(roundRect) }
    return Outline.Generic(path)
}

val OverscrollIndicatorLeftShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        createOverscrollIndicatorOutline(size, Side.LEFT)
}

val OverscrollIndicatorRightShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        createOverscrollIndicatorOutline(size, Side.RIGHT)
}

@Composable
fun OverscrollIndicator(
    modifier: Modifier = Modifier,
    scope: BoxScope,
    side: Side,
    isVisible: Boolean,
    maxWidth: Dp,
    color: Color,
    animationDuration: Int,
) {
    val shape = when (side) {
        Side.LEFT -> OverscrollIndicatorLeftShape
        Side.RIGHT -> OverscrollIndicatorRightShape
    }

    val width by animateDpAsState(
        targetValue = if (isVisible) maxWidth else 0.dp,
        animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing),
    )

    with(scope) {
        Box(
            modifier = modifier
                .width(width)
                .align(if (side == Side.LEFT) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                .background(color = color, shape = shape)
                .fillMaxHeight()
                .drawWithContent {
                    drawContent()
                    drawOutline(
                        outline = shape.createOutline(
                            size = size,
                            layoutDirection = layoutDirection,
                            density = Density(density, fontScale)
                        ),
                        color = color,
                        style = Stroke(pathEffect = PathEffect.cornerPathEffect(10f)),
                        alpha = color.alpha,
                    )
                }
        )
    }
}

@Composable
fun OverscrollIndicatorBox(
    modifier: Modifier = Modifier,
    indicatorWidth: Dp = 16.dp,
    indicatorColor: Color = Color.LightGray.copy(alpha = 0.3f),
    leftIndicatorVisible: Boolean = false,
    rightIndicatorVisible: Boolean = false,
    animationDuration: Int = 50,
    content: @Composable (BoxScope.() -> Unit),
) {
    Box(modifier = modifier) {
        content()
        OverscrollIndicator(
            scope = this,
            side = Side.LEFT,
            isVisible = leftIndicatorVisible,
            maxWidth = indicatorWidth,
            color = indicatorColor,
            animationDuration = animationDuration,
        )
        OverscrollIndicator(
            scope = this,
            side = Side.RIGHT,
            isVisible = rightIndicatorVisible,
            maxWidth = indicatorWidth,
            color = indicatorColor,
            animationDuration = animationDuration,
        )
    }
}
