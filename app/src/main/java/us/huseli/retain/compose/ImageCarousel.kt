package us.huseli.retain.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import us.huseli.retain.R
import us.huseli.retain.data.entities.ImageWithBitmap
import kotlin.math.abs

@Composable
fun ImageCarousel(
    modifier: Modifier = Modifier,
    images: List<ImageWithBitmap>,
    startIndex: Int = 0,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val currentIndex by rememberSaveable { mutableStateOf(startIndex) }
    var screenWidth by remember { mutableStateOf(0f) }
    with(LocalDensity.current) {
        screenWidth = context.resources.configuration.screenWidthDp.dp.toPx()
    }
    var maxHeightDp by remember { mutableStateOf(0.dp) }
    val maxHeight = with(LocalDensity.current) { maxHeightDp.toPx() }
    val originalImageHeight =
        images[currentIndex].imageBitmap.height * (screenWidth / images[currentIndex].imageBitmap.width)

    var scale by rememberSaveable { mutableStateOf(1f) }
    var panX by rememberSaveable { mutableStateOf(0f) }
    var panY by rememberSaveable { mutableStateOf(0f) }
    val maxY = abs((maxHeight / 2) - ((originalImageHeight * scale) / 2))
    val maxX = abs(screenWidth * (1 - scale) / 2)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (scale * zoomChange >= 1f) scale *= zoomChange

        if (abs(panX + panChange.x) <= maxX) panX += panChange.x
        else if (panX + panChange.x > maxX) panX = maxX
        else if (panX + panChange.x < -maxX) panX = -maxX

        if (originalImageHeight * scale > maxHeight) {
            if (abs(panY + panChange.y) <= maxY) panY += panChange.y
            else if (panY + panChange.y > maxY) panY = maxY
            else if (panY + panChange.y < -maxY) panY = -maxY
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().zIndex(1f)
    ) {
        maxHeightDp = this.maxHeight

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)))

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .fillMaxSize()
                .transformable(state = transformableState)
        ) {
            Image(
                bitmap = images[currentIndex].imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = panX, translationY = panY)
            )
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