package us.huseli.retain.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import okhttp3.internal.toImmutableList
import us.huseli.retain.R
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.ImageWithBitmap

class ImageIterator(
    private val objects: List<ImageWithBitmap>,
    private val maxRows: Int = Int.MAX_VALUE
) : Iterator<List<ImageWithBitmap>> {
    private var currentIndex = 0
    private var currentRow = 0

    override fun hasNext() = currentIndex < objects.size && currentRow < maxRows

    override fun next(): List<ImageWithBitmap> {
        val result = mutableListOf<ImageWithBitmap>()
        var collectedRatio = 0f

        for (i in currentIndex until objects.size) {
            if (collectedRatio > 0 && collectedRatio + objects[i].image.ratio > 5) break
            result.add(objects[i])
            collectedRatio += objects[i].image.ratio
            currentIndex++
            if (currentIndex == 1) break
        }

        currentRow++
        return result.toImmutableList()
    }
}

@Composable
fun NoteImageGrid(
    modifier: Modifier = Modifier,
    imagesWithBitmap: List<ImageWithBitmap>,
    showDeleteButton: Boolean,
    maxRows: Int = Int.MAX_VALUE,
    secondaryRowHeight: Dp,
    onImageClick: ((Int) -> Unit)? = null,
    onDeleteButtonClick: ((Image) -> Unit)? = null,
) {
    val imageWithBitmapLists = ImageIterator(
        objects = imagesWithBitmap,
        maxRows = maxRows
    )

    imageWithBitmapLists.asSequence().forEachIndexed { index, sublist ->
        var rowModifier = modifier.fillMaxWidth()
        if (index > 0) rowModifier = rowModifier.heightIn(max = secondaryRowHeight)

        Row(modifier = rowModifier) {
            sublist.forEachIndexed { sublistIndex, (image, bitmap) ->
                Box(
                    modifier = Modifier.weight(if (sublist.size == 1) 1f else image.ratio)
                ) {
                    var imageModifier = Modifier.fillMaxWidth()
                    if (onImageClick != null) imageModifier = imageModifier.clickable {
                        onImageClick.invoke(index + sublistIndex)
                    }

                    Image(
                        bitmap = bitmap,
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null,
                        modifier = imageModifier,
                    )
                    if (showDeleteButton) {
                        FilledTonalIconButton(
                            onClick = { onDeleteButtonClick?.invoke(image) },
                            modifier = Modifier.align(Alignment.TopEnd).scale(0.75f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.delete_image),
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}