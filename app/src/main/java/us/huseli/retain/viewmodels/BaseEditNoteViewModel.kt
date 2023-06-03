package us.huseli.retain.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.ui.theme.getNoteColor
import java.util.UUID
import kotlin.math.max

class ChecklistItemExtended(
    item: ChecklistItem,
    val textFieldValue: MutableStateFlow<TextFieldValue> = MutableStateFlow(
        TextFieldValue(
            addNullChar(item.text),
            TextRange(1)
        )
    )
) : ChecklistItem(
    id = item.id,
    text = item.text,
    noteId = item.noteId,
    checked = item.checked,
    position = item.position,
) {
    override fun copy(text: String, checked: Boolean, position: Int): ChecklistItemExtended {
        return ChecklistItemExtended(super.copy(text, checked, position), textFieldValue).also {
            it.textFieldValue.value = it.textFieldValue.value.copy(text = addNullChar(text))
        }
    }
}

abstract class BaseEditNoteViewModel(
    context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    type: NoteType
) : ViewModel(), LogInterface {
    private val _currentCarouselId = MutableStateFlow(savedStateHandle.get<String?>(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID))
    private val _dirtyImages = mutableListOf<Image>()
    private val _images = MutableStateFlow<List<Image>>(emptyList())
    private val _originalImages = mutableListOf<Image>()
    private val _selectedImages = MutableStateFlow<List<String>>(emptyList())
    private val _trashedImages = MutableStateFlow<List<Image>>(emptyList())
    private var _isNew = true
    private val _deletedImageIds = mutableListOf<String>()

    protected val _deletedChecklistItemIds = mutableListOf<UUID>()
    protected val _checklistItems = MutableStateFlow<List<ChecklistItemExtended>>(emptyList())
    protected val _dirtyChecklistItems = mutableListOf<ChecklistItem>()
    protected val _noteId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_NOTE_ID)!!)
    protected val _originalChecklistItems = mutableListOf<ChecklistItemExtended>()

    private var _originalNote: Note = Note(type = type, id = _noteId)
    protected val _note = MutableStateFlow(_originalNote)

    val currentCarouselImage = _currentCarouselId.map { filename -> _images.value.find { it.filename == filename } }
    val deletedChecklistItemIds: List<UUID>
        get() = _deletedChecklistItemIds.toImmutableList()
    val deletedImageIds: List<String>
        get() = _deletedImageIds.toImmutableList()
    val dirtyChecklistItems: List<ChecklistItem>
        get() = _dirtyChecklistItems.toImmutableList()
    val dirtyImages: List<Image>
        get() = _dirtyImages.toImmutableList()
    val dirtyNote: Note?
        get() =
            if (
                _originalNote != _note.value ||
                (_isNew && (_dirtyChecklistItems.isNotEmpty() || _dirtyImages.isNotEmpty()))
            ) _note.value else null
    val images = _images.asStateFlow()
    val note = _note.asStateFlow()
    val noteColor = _note.map { getNoteColor(context, it.color) }
    val trashedImageCount = _trashedImages.map { it.size }
    val selectedImages = _selectedImages.asStateFlow()

    val appBarColor = noteColor.map {
        it.copy(
            red = max(it.red - 0.05f, 0f),
            green = max(it.green - 0.05f, 0f),
            blue = max(it.blue - 0.05f, 0f),
        )
    }

    init {
        viewModelScope.launch {
            repository.getCombo(_noteId)?.let { (note, checklistItems, images, _) ->
                _isNew = false
                _originalNote = note
                _note.value = note
                _originalImages.addAll(images)
                _images.value = images
                if (type == NoteType.CHECKLIST) {
                    _originalChecklistItems.addAll(checklistItems.map { ChecklistItemExtended(it) })
                    _checklistItems.value = _originalChecklistItems
                }
            }
        }
    }

    private fun addDirtyImage(image: Image) {
        _dirtyImages.removeIf { it.filename == image.filename }
        if (_originalImages.none { it == image }) _dirtyImages.add(image)
    }

    fun clearTrashedImages() {
        _trashedImages.value = emptyList()
    }

    fun deselectAllImages() {
        _selectedImages.value = emptyList()
    }

    fun insertImage(uri: Uri) = viewModelScope.launch {
        repository.uriToImage(uri, _noteId)?.let { image ->
            _images.value = _images.value.toMutableList().apply { add(image) }
            addDirtyImage(image)
            updateImagePositions()
        }
    }

    fun selectAllImages() {
        _selectedImages.value = _images.value.map { it.filename }
    }

    fun selectImage(filename: String) {
        _selectedImages.value = _selectedImages.value.toMutableList().apply { add(filename) }
    }

    fun setColor(value: String) {
        if (value != _note.value.color) _note.value = _note.value.copy(color = value)
    }

    fun setText(value: String) {
        if (value != _note.value.text) _note.value = _note.value.copy(text = value)
    }

    fun setTitle(value: String) {
        if (value != _note.value.title) _note.value = _note.value.copy(title = value)
    }

    fun toggleImageSelected(filename: String) {
        _selectedImages.value = _selectedImages.value.toMutableList().apply {
            if (contains(filename)) remove(filename)
            else add(filename)
        }
    }

    fun trashSelectedImages() {
        clearTrashedImages()
        _trashedImages.value = _images.value.filter { _selectedImages.value.contains(it.filename) }
        _deletedImageIds.addAll(_selectedImages.value)
        _images.value = _images.value.toMutableList().apply {
            removeAll(_trashedImages.value)
        }
        deselectAllImages()
    }

    fun undoTrashBitmapImages() = viewModelScope.launch {
        _images.value = _images.value.toMutableList().apply {
            _trashedImages.value.forEach { add(it.position, it) }
        }
        _deletedImageIds.removeAll(_trashedImages.value.map { it.filename })
        clearTrashedImages()
    }

    private fun updateImagePositions() {
        _images.value = _images.value.mapIndexed { index, image ->
            if (image.position != index) {
                image.copy(position = index).also { addDirtyImage(it) }
            } else image
        }
    }

    /** IMAGE CAROUSEL *******************************************************/
    fun gotoNextCarouselImage() {
        val currentImageIdx = _images.value.indexOfFirst { it.filename == _currentCarouselId.value }
        val newImageIdx = if (currentImageIdx >= _images.value.size - 1) 0 else currentImageIdx + 1
        _currentCarouselId.value = _images.value.getOrNull(newImageIdx)?.filename
    }

    fun gotoPreviousCarouselImage() {
        val currentImageIdx = _images.value.indexOfFirst { it.filename == _currentCarouselId.value }
        val newImageIdx = if (currentImageIdx == 0) _images.value.size - 1 else currentImageIdx - 1
        _currentCarouselId.value = _images.value.getOrNull(newImageIdx)?.filename
    }

    fun setCarouselImage(filename: String) {
        _currentCarouselId.value = filename
    }

    fun unsetCarouselImage() {
        _currentCarouselId.value = null
    }
}
