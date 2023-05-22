package us.huseli.retain.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import us.huseli.retain.Enums.Side
import us.huseli.retain.R
import us.huseli.retain.data.entities.BitmapImage
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ImageCarousel(
    modifier: Modifier = Modifier,
    bitmapImage: BitmapImage,
    multiple: Boolean,
    onClose: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val screenWidth = with(LocalDensity.current) { context.resources.configuration.screenWidthDp.dp.toPx() }
    var maxHeightDp by remember { mutableStateOf(0.dp) }
    val maxHeight = with(LocalDensity.current) { maxHeightDp.toPx() }
    val originalImageHeight =
        bitmapImage.imageBitmap.height * (screenWidth / bitmapImage.imageBitmap.width)
    var slideFrom by remember { mutableStateOf<Side?>(null) }
    var slideTo by remember(bitmapImage) { mutableStateOf<Side?>(null) }
    val offset by remember { mutableStateOf(Animatable(0f)) }

    LaunchedEffect(slideFrom, bitmapImage) {
        if (slideFrom == Side.LEFT) {
            offset.snapTo(-screenWidth)
            offset.animateTo(0f, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
        } else if (slideFrom == Side.RIGHT) {
            offset.snapTo(screenWidth)
            offset.animateTo(0f, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
        }
    }

    LaunchedEffect(slideTo, bitmapImage) {
        if (slideTo == Side.LEFT) {
            offset.animateTo(-screenWidth, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
            slideFrom = Side.RIGHT
            onSwipeLeft()
        } else if (slideTo == Side.RIGHT) {
            offset.animateTo(screenWidth, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
            slideFrom = Side.LEFT
            onSwipeRight()
        }
    }

    var isTransformable by remember(bitmapImage) { mutableStateOf(false) }
    var overScroll by remember(bitmapImage) { mutableStateOf(0f) }
    var scale by remember(bitmapImage) { mutableStateOf(1f) }
    var panX by remember(bitmapImage) { mutableStateOf(0f) }
    var panY by remember(bitmapImage) { mutableStateOf(0f) }
    val maxY = abs((maxHeight / 2) - ((originalImageHeight * scale) / 2))
    val maxX = abs(screenWidth * (1 - scale) / 2)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (isTransformable) {
            if (scale * zoomChange >= 1f) scale *= zoomChange

            if (abs(panX + panChange.x) <= maxX) {
                panX += panChange.x
                overScroll = 0f
            } else {
                overScroll += panChange.x
                if (panX + panChange.x > maxX) panX = maxX
                else if (panX + panChange.x < -maxX) panX = -maxX
            }

            if (overScroll < -500f && multiple) slideTo = Side.LEFT
            else if (overScroll > 500f && multiple) slideTo = Side.RIGHT

            if (originalImageHeight * scale > maxHeight) {
                if (abs(panY + panChange.y) <= maxY) panY += panChange.y
                else if (panY + panChange.y > maxY) panY = maxY
                else if (panY + panChange.y < -maxY) panY = -maxY
            }
        }
    }

    if (!transformableState.isTransformInProgress) {
        isTransformable = true
        overScroll = 0f
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().zIndex(1f)
    ) {
        maxHeightDp = this.maxHeight

        // Backdrop:
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        )

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .clickable(interactionSource = interactionSource, indication = null) { onClose() }
        ) {
            OverscrollIndicatorBox(
                leftIndicatorVisible = overScroll > 0f,
                rightIndicatorVisible = overScroll < 0f,
            ) {
                Image(
                    bitmap = bitmapImage.imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = panX, translationY = panY)
                        .clickable(enabled = false) {}
                        .absoluteOffset { IntOffset(offset.value.roundToInt(), 0) }
                )
            }
        }

        FilledTonalIconButton(
            modifier = Modifier.padding(start = 8.dp, top = 12.dp).width(40.dp),
            onClick = onClose
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.go_back),
                tint = Color.LightGray
            )
        }
    }
}
