package us.huseli.retain.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.ui.theme.getNoteColor
import java.util.UUID
import kotlin.math.max

abstract class BaseEditNoteViewModel(
    context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    type: NoteType
) : ViewModel(), LogInterface {
    protected val _bitmapImages = MutableStateFlow<List<BitmapImage>>(emptyList())
    private val _trashedBitmapImages = MutableStateFlow<List<BitmapImage>>(emptyList())
    protected var _isNew = true
    private val _currentCarouselId = MutableStateFlow(
        savedStateHandle.get<String?>(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID)
    )

    protected val _noteId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_NOTE_ID)!!)
    protected val _note = MutableStateFlow(Note(type = type))
    protected var _isDirty = false

    val title = _note.map { it.title }
    val text = _note.map { it.text }
    val colorIdx = _note.map { it.colorIdx }
    val showChecked = _note.map { it.showChecked }
    val bitmapImages = _bitmapImages.asStateFlow()
    val trashedBitmapImages = _trashedBitmapImages.asStateFlow()
    val currentCarouselImage = _currentCarouselId.map { filename ->
        _bitmapImages.value.find { it.image.filename == filename }
    }
    val noteColor = _note.map { getNoteColor(context, it.colorIdx) }
    val appBarColor = noteColor.map {
        it.copy(
            red = max(it.red - 0.05f, 0f),
            green = max(it.green - 0.05f, 0f),
            blue = max(it.blue - 0.05f, 0f),
        )
    }

    open val combo: NoteCombo
        get() = NoteCombo(_note.value, emptyList(), _bitmapImages.value.map { it.image })

    val shouldSave: Boolean
        get() = _isDirty && (!_isNew || isEmpty())

    fun clearTrashBitmapImages() {
        _trashedBitmapImages.value = emptyList()
    }

    fun deleteImage(bitmapImage: BitmapImage) {
        val index = _bitmapImages.value.indexOfFirst { it.image.filename == bitmapImage.image.filename }

        if (index > -1) {
            _bitmapImages.value = _bitmapImages.value.toMutableList().apply { removeAt(index) }
            _isDirty = true
            _trashedBitmapImages.value = listOf(bitmapImage)
        }
    }

    fun insertImage(uri: Uri) = viewModelScope.launch {
        repository.uriToBitmapImage(uri, _noteId)?.let { bitmapImage ->
            _bitmapImages.value = _bitmapImages.value.toMutableList().apply { add(bitmapImage) }
            updateImagePositions()
            _isDirty = true
        }
    }

    open fun isEmpty(): Boolean =
        _note.value.text.isBlank() && _note.value.title.isBlank() && _bitmapImages.value.isEmpty()

    fun setColorIdx(value: Int) {
        if (value != _note.value.colorIdx) {
            _note.value = _note.value.copy(colorIdx = value)
            _isDirty = true
        }
    }

    fun setText(value: String) {
        if (value != _note.value.text) {
            _note.value = _note.value.copy(text = value)
            _isDirty = true
        }
    }

    fun setTitle(value: String) {
        if (value != _note.value.title) {
            _note.value = _note.value.copy(title = value)
            _isDirty = true
        }
    }

    fun undoTrashBitmapImages() = viewModelScope.launch {
        _bitmapImages.value = _bitmapImages.value.toMutableList().apply {
            addAll(_trashedBitmapImages.value)
            sortBy { it.image.position }
        }
        clearTrashBitmapImages()
    }

    private fun updateImagePositions() {
        _bitmapImages.value = _bitmapImages.value.mapIndexed { index, bitmapImage ->
            bitmapImage.copy(image = bitmapImage.image.copy(position = index))
        }
    }

    /** IMAGE CAROUSEL *******************************************************/
    fun setCarouselImage(image: Image) {
        _currentCarouselId.value = image.filename
    }

    fun unsetCarouselImage() {
        _currentCarouselId.value = null
    }

    fun gotoPreviousCarouselImage() {
        val currentImageIdx = _bitmapImages.value.indexOfFirst { it.image.filename == _currentCarouselId.value }
        val newImageIdx = if (currentImageIdx == 0) _bitmapImages.value.size - 1 else currentImageIdx - 1
        _currentCarouselId.value = _bitmapImages.value.getOrNull(newImageIdx)?.image?.filename
    }

    fun gotoNextCarouselImage() {
        val currentImageIdx = _bitmapImages.value.indexOfFirst { it.image.filename == _currentCarouselId.value }
        val newImageIdx = if (currentImageIdx >= _bitmapImages.value.size - 1) 0 else currentImageIdx + 1
        _currentCarouselId.value = _bitmapImages.value.getOrNull(newImageIdx)?.image?.filename
    }
}
