package us.huseli.retain.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import us.huseli.retain.dataclasses.entities.Image
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(
    modifier: Modifier = Modifier,
    image: Image,
    imageBitmap: ImageBitmap,
    multiple: Boolean,
    onClose: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
) {
    var isTransformable by remember(image) { mutableStateOf(false) }
    var maxHeightDp by remember { mutableStateOf(0.dp) }
    var overScroll by remember(image) { mutableFloatStateOf(0f) }
    var panX by remember(image) { mutableFloatStateOf(0f) }
    var panY by remember(image) { mutableFloatStateOf(0f) }
    var scale by remember(image) { mutableFloatStateOf(1f) }
    var slideFrom by remember { mutableStateOf<Side?>(null) }
    var slideTo by remember(image) { mutableStateOf<Side?>(null) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val offset by remember { mutableStateOf(Animatable(0f)) }
    val containerHeight = with(density) { maxHeightDp.toPx() }
    val containerWidth = remember { with(density) { context.resources.configuration.screenWidthDp.dp.toPx() } }
    val containerRatio = if (containerHeight > 0f) containerWidth / containerHeight else 0f

    val originalImageHeight =
        if (image.ratio <= containerRatio) containerHeight
        else imageBitmap.height * (containerWidth / imageBitmap.width)
    val maxX = abs(containerWidth * (1 - scale) / 2)
    val maxY = abs((containerHeight / 2) - ((originalImageHeight * scale) / 2))

    LaunchedEffect(slideFrom, image) {
        if (slideFrom == Side.LEFT) {
            offset.snapTo(-containerWidth)
            offset.animateTo(0f, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
        } else if (slideFrom == Side.RIGHT) {
            offset.snapTo(containerWidth)
            offset.animateTo(0f, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
        }
    }

    LaunchedEffect(slideTo, image) {
        if (slideTo == Side.LEFT) {
            offset.animateTo(-containerWidth, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
            slideFrom = Side.RIGHT
            onSwipeLeft()
        } else if (slideTo == Side.RIGHT) {
            offset.animateTo(containerWidth, animationSpec = tween(easing = LinearEasing, durationMillis = 100))
            slideFrom = Side.LEFT
            onSwipeRight()
        }
    }

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

            if (originalImageHeight * scale > containerHeight) {
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
        modifier = modifier.fillMaxSize().zIndex(1f)
    ) {
        maxHeightDp = this.maxHeight

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
        ) {
            OverscrollIndicatorBox(
                leftIndicatorVisible = overScroll > 100f,
                rightIndicatorVisible = overScroll < -100f,
            ) {
                val imageModifier =
                    if (containerRatio > image.ratio) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = if (containerRatio > image.ratio) ContentScale.FillHeight else ContentScale.FillWidth,
                    modifier = imageModifier
                        .align(Alignment.Center)
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = panX, translationY = panY)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                            onDoubleClick = {
                                scale = 1f
                                panX = 0f
                                panY = 0f
                            },
                            indication = null,
                        )
                        .absoluteOffset { IntOffset(offset.value.roundToInt(), 0) }
                )
            }
        }

        FilledTonalIconButton(
            modifier = Modifier.padding(start = 8.dp, top = 12.dp).width(40.dp),
            onClick = onClose
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Sharp.ArrowBack,
                contentDescription = stringResource(R.string.go_back),
                tint = Color.LightGray
            )
        }
    }
}
