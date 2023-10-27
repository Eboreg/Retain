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

data class ChecklistItemFlow(
    val item: ChecklistItem,
    val id: UUID = item.id,
    val checked: MutableStateFlow<Boolean> = MutableStateFlow(item.checked),
    val position: MutableStateFlow<Int> = MutableStateFlow(item.position),
    val textFieldValue: MutableStateFlow<TextFieldValue> = MutableStateFlow(
        TextFieldValue(item.text, TextRange(0))
    )
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ChecklistItemFlow ->
                other.position.value == position.value &&
                    other.checked.value == checked.value &&
                    other.id == id &&
                    other.textFieldValue.value.text == textFieldValue.value.text

            is ChecklistItem ->
                other.id == id &&
                    other.position == position.value &&
                    other.checked == checked.value &&
                    other.text == textFieldValue.value.text

            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + checked.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + textFieldValue.hashCode()
        return result
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
    protected val _checklistItems = MutableStateFlow<List<ChecklistItemFlow>>(emptyList())
    protected val _dirtyChecklistItems = mutableListOf<ChecklistItemFlow>()
    protected val _noteId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_NOTE_ID)!!)
    protected val _originalChecklistItems = mutableListOf<ChecklistItem>()

    private var _originalNote: Note = Note(type = type, id = _noteId)
    protected val _note = MutableStateFlow(_originalNote)
    private val _textFieldValue = MutableStateFlow(TextFieldValue(_note.value.text))

    val deletedChecklistItemIds: List<UUID>
        get() = _deletedChecklistItemIds.toImmutableList()
    val deletedImageIds: List<String>
        get() = _deletedImageIds.toImmutableList()
    val dirtyChecklistItems: List<ChecklistItem>
        get() = _dirtyChecklistItems.map {
            it.item.copy(text = it.textFieldValue.value.text, checked = it.checked.value, position = it.position.value)
        }
    val dirtyImages: List<Image>
        get() = _dirtyImages.toImmutableList()
    val dirtyNote: Note?
        get() {
            _note.value = _note.value.copy(text = _textFieldValue.value.text)
            return if (
                _originalNote != _note.value ||
                (_isNew && (_dirtyChecklistItems.isNotEmpty() || _dirtyImages.isNotEmpty()))
            ) _note.value else null
        }
    val images = _images.asStateFlow()
    val note = _note.asStateFlow()
    val trashedImageCount = _trashedImages.map { it.size }
    val selectedImages = _selectedImages.asStateFlow()
    val imageAdded = _imageAdded.asSharedFlow()
    val textFieldValue = _textFieldValue.asStateFlow()

    init {
        viewModelScope.launch {
            @Suppress("Destructure")
            repository.getCombo(_noteId)?.also { combo ->
                _isNew = false
                _originalNote = combo.note
                _note.value = combo.note
                _textFieldValue.value = TextFieldValue(combo.note.text)
                _originalImages.addAll(combo.images)
                _images.value = combo.images
                if (type == NoteType.CHECKLIST) {
                    _originalChecklistItems.addAll(combo.checklistItems)
                    _checklistItems.value = combo.checklistItems.map { ChecklistItemFlow(it) }
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

    fun moveCursorLast() {
        _textFieldValue.value = _textFieldValue.value.copy(selection = TextRange(_textFieldValue.value.text.length))
    }

    fun selectAllImages() {
        _selectedImages.value = _images.value.map { it.filename }.toSet()
    }

    fun setColor(value: String) {
        if (value != _note.value.color) _note.value = _note.value.copy(color = value)
    }

    fun setTextFieldValue(value: TextFieldValue) {
        if (value != _textFieldValue.value) _textFieldValue.value = value
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
            removeAll(_trashedImages.value.toSet())
        }
        deselectAllImages()
    }

    fun undoTrashBitmapImages() = viewModelScope.launch {
        _images.value = _images.value.toMutableList().apply {
            _trashedImages.value.forEach { add(it.position, it) }
        }
        _deletedImageIds.removeAll(_trashedImages.value.map { it.filename }.toSet())
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
