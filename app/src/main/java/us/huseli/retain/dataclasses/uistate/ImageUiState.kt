package us.huseli.retain.dataclasses.uistate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retain.dataclasses.entities.Image

@Stable
interface IImageUiState : Comparable<IImageUiState> {
    val filename: String
    val position: Int
    val isSelected: Boolean
    val width: Int?
    val height: Int?

    val dimensions: IntSize?
        get() {
            val width = this.width
            val height = this.height

            return if (width != null && height != null) IntSize(width, height) else null
        }

    val ratio: Float
        get() = dimensions?.let { it.width.toFloat() / it.height.toFloat() } ?: 0f

    override fun compareTo(other: IImageUiState): Int = position - other.position
}

@Stable
class MutableImageUiState(private var image: Image, isNew: Boolean = false) : IImageUiState {
    override var position by mutableIntStateOf(image.position)
    override val filename = image.filename
    override var isSelected by mutableStateOf(false)
    override val height = image.height
    override val width = image.width
    var isNew by mutableStateOf(isNew)

    val isChanged: Boolean
        get() = image.position != position

    fun clone() = MutableImageUiState(image = toImage())

    fun onSaved() {
        isNew = false
        image = toImage()
    }

    fun toImage() = image.copy(position = position)
}

suspend fun Collection<MutableImageUiState>.save(dbSaveFunc: suspend (Collection<Image>) -> Unit) {
    val states = filter { it.isNew || it.isChanged }

    if (states.isNotEmpty()) {
        dbSaveFunc(states.map { it.toImage() })
        withContext(Dispatchers.Main) { states.forEach { it.onSaved() } }
    }
}

fun Collection<MutableImageUiState>.clone() = map { it.clone() }
