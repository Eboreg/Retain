package us.huseli.retain.viewmodels

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import java.util.UUID

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
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    type: NoteType
) : ViewModel(), LogInterface {
    private val _dirtyImages = mutableListOf<Image>()
    private val _images = MutableStateFlow<List<Image>>(emptyList())
    private val _originalImages = mutableListOf<Image>()
    private val _trashedImages = MutableStateFlow<List<Image>>(emptyList())
    private var _isNew = true
    private val _deletedImageIds = mutableListOf<String>()
    private val _imageAdded = MutableSharedFlow<ImageBitmap>()
    private val _selectedImages = MutableStateFlow<Set<String>>(emptySet())

    protected val _deletedChecklistItemIds = mutableListOf<UUID>()
    protected val _checklistItems = MutableStateFlow<List<ChecklistItemExtended>>(emptyList())
    protected val _dirtyChecklistItems = mutableListOf<ChecklistItem>()
    protected val _noteId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_NOTE_ID)!!)
    protected val _originalChecklistItems = mutableListOf<ChecklistItemExtended>()

    private var _originalNote: Note = Note(type = type, id = _noteId)
    protected val _note = MutableStateFlow(_originalNote)

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
    val trashedImageCount = _trashedImages.map { it.size }
    val selectedImages = _selectedImages.asStateFlow()
    val imageAdded = _imageAdded.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getCombo(_noteId)?.also { (note, checklistItems, images, _) ->
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
        _selectedImages.value = emptySet()
    }

    fun insertImage(uri: Uri) = viewModelScope.launch {
        repository.uriToImage(uri, _noteId)?.let { image ->
            _images.value = _images.value.toMutableList().apply { add(image) }
            addDirtyImage(image)
            updateImagePositions()
            image.imageBitmap.collect {
                if (it != null) _imageAdded.emit(it)
            }
        }
    }

    fun selectAllImages() {
        _selectedImages.value = _images.value.map { it.filename }.toSet()
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
        _selectedImages.value = _selectedImages.value.toMutableSet().apply {
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
}
