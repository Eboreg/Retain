package us.huseli.retain.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retain.Constants
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.Note
import java.util.UUID

abstract class BaseEditNoteViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    type: NoteType
) : ViewModel() {
    private val _bitmapImages = MutableStateFlow<List<BitmapImage>>(emptyList())
    private val _trashedBitmapImages = MutableStateFlow<List<BitmapImage>>(emptyList())
    protected val _note = MutableStateFlow(Note(type = type))
    protected var _isDirty = true

    val noteId: UUID = UUID.fromString(savedStateHandle.get<String>(Constants.NAV_ARG_NOTE_ID)!!)
    val note = _note.asStateFlow()
    val title = _note.map { it.title }
    val text = _note.map { it.text }
    val colorIdx = _note.map { it.colorIdx }
    val showChecked = _note.map { it.showChecked }
    val bitmapImages = _bitmapImages.asStateFlow()
    val trashedBitmapImages = _trashedBitmapImages.asStateFlow()

    val isDirty: Boolean
        get() = _isDirty

    init {
        _bitmapImages.value = repository.bitmapImages.value.filter { it.image.noteId == noteId }

        viewModelScope.launch {
            repository.getNote(noteId)?.let { note ->
                _note.value = note
                _isDirty = false
            }
        }
    }

    fun clearTrashBitmapImages() {
        _trashedBitmapImages.value = emptyList()
    }

    fun deleteImage(bitmapImage: BitmapImage) {
        if (_bitmapImages.value.any { it.image.filename == bitmapImage.image.filename }) {
            _bitmapImages.value = _bitmapImages.value.toMutableList().apply {
                if (removeIf { it.image.filename == bitmapImage.image.filename }) {
                    _isDirty = true
                    _trashedBitmapImages.value = listOf(bitmapImage)
                }
            }
        }
    }

    fun insertImage(uri: Uri) = viewModelScope.launch {
        repository.uriToBitmapImage(uri, noteId)?.let { bitmapImage ->
            _bitmapImages.value = _bitmapImages.value.toMutableList().apply { add(bitmapImage) }
            _isDirty = true
        }
    }

    fun setColorIdx(value: Int) {
        if (value != _note.value.colorIdx) {
            updateNote(colorIdx = value)
            _isDirty = true
        }
    }

    fun setText(value: String) {
        if (value != _note.value.text) {
            updateNote(text = value)
            _isDirty = true
        }
    }

    fun setTitle(value: String) {
        if (value != _note.value.title) {
            updateNote(title = value)
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

    protected fun updateNote(
        title: String? = null,
        text: String? = null,
        colorIdx: Int? = null,
        showChecked: Boolean? = null
    ) {
        _note.value = _note.value.copy(title, text, showChecked, colorIdx)
    }
}
