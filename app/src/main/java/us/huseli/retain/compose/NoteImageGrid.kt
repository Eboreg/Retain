package us.huseli.retain.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import okhttp3.internal.toImmutableList
import us.huseli.retain.data.entities.Image

class ImageIterator(
    private val objects: List<Image>,
    private val maxRows: Int = Int.MAX_VALUE
) : Iterator<List<Image>> {
    private var currentIndex = 0
    private var currentRow = 0

    override fun hasNext() = currentIndex < objects.size && currentRow < maxRows

    override fun next(): List<Image> {
        val result = mutableListOf<Image>()
        var collectedRatio = 0f

        for (i in currentIndex until objects.size) {
            if (collectedRatio > 0 && collectedRatio + objects[i].ratio > 5) break
            result.add(objects[i])
            collectedRatio += objects[i].ratio
            currentIndex++
            if (currentIndex == 1) break
        }

        currentRow++
        return result.toImmutableList()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteImageGrid(
    modifier: Modifier = Modifier,
    images: List<Image>,
    maxRows: Int = Int.MAX_VALUE,
    secondaryRowHeight: Dp,
    onImageClick: ((String) -> Unit)? = null,
    onImageLongClick: ((String) -> Unit)? = null,
    selectedImages: Set<String>? = null,
) {
    val imageLists = ImageIterator(
        objects = images,
        maxRows = maxRows
    )

    imageLists.asSequence().forEachIndexed { index, sublist ->
        var rowModifier = modifier.fillMaxWidth()
        if (index > 0) rowModifier = rowModifier.heightIn(max = secondaryRowHeight)

        Row(modifier = rowModifier) {
            sublist.forEach { image ->
                val imageBitmap by image.imageBitmap.collectAsStateWithLifecycle(null)

                Box(
                    modifier = Modifier
                        .weight(if (sublist.size == 1) 1f else image.ratio)
                        .aspectRatio(image.ratio)
                ) {
                    var imageModifier = Modifier.fillMaxWidth()

                    if (onImageClick != null || onImageLongClick != null) {
                        imageModifier = imageModifier.combinedClickable(
                            onClick = { onImageClick?.invoke(image.filename) },
                            onLongClick = { onImageLongClick?.invoke(image.filename) },
                        )
                    }

                    if (selectedImages?.contains(image.filename) == true)
                        imageModifier = imageModifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))

                    imageBitmap?.let {
                        Image(
                            bitmap = it,
                            contentScale = ContentScale.FillWidth,
                            contentDescription = null,
                            modifier = imageModifier,
                        )
                    } ?: kotlin.run {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).widthIn(max = 20.dp).aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}
