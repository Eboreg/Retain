package us.huseli.retain.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.retain.ImageIterator
import us.huseli.retain.interfaces.IImage
import us.huseli.retain.viewmodels.ImageViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteImageGrid(
    viewModel: ImageViewModel = hiltViewModel(),
    images: List<IImage>,
    maxRows: Int = Int.MAX_VALUE,
    secondaryRowHeight: Dp,
    onImageClick: ((String) -> Unit)? = null,
    onImageLongClick: ((String) -> Unit)? = null,
) {
    val imageLists = ImageIterator(objects = images, maxRows = maxRows)
    val selectedImages = images.filter { it.isSelected }.map { it.filename }

    imageLists.asSequence().forEachIndexed { index, sublist ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (index > 0) secondaryRowHeight else secondaryRowHeight * 2)
        ) {
            sublist.forEach { image ->
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(image.filename) {
                    imageBitmap = viewModel.getImageBitmap(image.filename)
                }

                Box(modifier = Modifier.weight(if (sublist.size == 1) 1f else image.ratio)) {
                    imageBitmap?.let {
                        Image(
                            bitmap = it,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (onImageClick != null || onImageLongClick != null) Modifier.combinedClickable(
                                        onClick = { onImageClick?.invoke(image.filename) },
                                        onLongClick = { onImageLongClick?.invoke(image.filename) },
                                    ) else Modifier
                                )
                                .then(
                                    if (selectedImages.contains(image.filename))
                                        Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
                                    else Modifier
                                ),
                        )
                    } ?: kotlin.run {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Sharp.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(secondaryRowHeight).padding(5.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
