package us.huseli.retain.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.viewmodels.ImageCarouselViewModel

@Composable
fun ImageCarouselScreen(
    modifier: Modifier = Modifier,
    viewModel: ImageCarouselViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    val images by viewModel.images.collectAsStateWithLifecycle(emptyList())
    val currentImage by viewModel.currentImage.collectAsStateWithLifecycle(null)
    val currentImageBitmap by viewModel.currentImageBitmap.collectAsStateWithLifecycle(null)

    RetainScaffold { innerPadding ->
        currentImage?.let { image ->
            currentImageBitmap?.let { imageBitmap ->
                ImageCarousel(
                    modifier = modifier.padding(innerPadding),
                    image = image,
                    imageBitmap = imageBitmap,
                    multiple = images.size > 1,
                    onClose = onClose,
                    onSwipeLeft = { viewModel.gotoNext() },
                    onSwipeRight = { viewModel.gotoPrevious() },
                )
            }
        }
    }
}
