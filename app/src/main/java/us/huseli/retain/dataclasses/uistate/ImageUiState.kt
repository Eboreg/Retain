package us.huseli.retain.dataclasses.uistate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.interfaces.IImage

@Stable
class ImageUiState(private var image: Image, isNew: Boolean = false) : IImage {
    override var position by mutableIntStateOf(image.position)
    override val filename = image.filename
    override var isSelected by mutableStateOf(false)
    override val height = image.height
    override val width = image.width
    var isNew by mutableStateOf(isNew)

    val isChanged: Boolean
        get() = image.position != position

    fun clone() = ImageUiState(image = toImage())

    override fun equals(other: Any?): Boolean = other is IImage &&
        other.position == position &&
        other.filename == filename

    override fun hashCode(): Int = 31 * position + filename.hashCode()

    fun onSaved() {
        isNew = false
        image = toImage()
    }

    fun toImage() = image.copy(position = position)
}

suspend fun Collection<ImageUiState>.save(dbSaveFunc: suspend (Collection<Image>) -> Unit) {
    val states = filter { it.isNew || it.isChanged }

    if (states.isNotEmpty()) {
        dbSaveFunc(states.map { it.toImage() })
        withContext(Dispatchers.Main) { states.forEach { it.onSaved() } }
    }
}

fun Collection<ImageUiState>.clone() = map { it.clone() }
