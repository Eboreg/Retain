package us.huseli.retain.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.uistate.MutableImageUiState
import us.huseli.retain.dataclasses.uistate.MutableNoteUiState
import us.huseli.retain.dataclasses.uistate.save
import us.huseli.retain.repositories.NoteRepository
import us.huseli.retain.ui.theme.NoteColorKey
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.utils.AbstractBaseViewModel
import java.util.UUID
import kotlin.collections.plus

abstract class AbstractNoteViewModel<UndoStateType>(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle,
    noteType: NoteType,
) : AbstractBaseViewModel() {
    protected val _create = savedStateHandle.get<String>(NAV_ARG_NOTE_ID) == null
    protected val _images = MutableStateFlow<List<MutableImageUiState>>(emptyList())
    protected val _noteId: UUID =
        savedStateHandle.get<String>(NAV_ARG_NOTE_ID)?.let { UUID.fromString(it) } ?: UUID.randomUUID()
    protected val _undoStateIdx = MutableStateFlow<Int?>(null)
    protected val _undoStates = MutableStateFlow<List<UndoStateType>>(emptyList())

    val images = _images.asStateFlow()
    val isRedoPossible = combine(_undoStates, _undoStateIdx) { states, idx -> idx != null && idx < states.lastIndex }
        .stateWhileSubscribed(false)
    val isUndoPossible = _undoStateIdx.map { it != null && it > 0 }.stateWhileSubscribed(false)
    var noteUiState = MutableNoteUiState(
        note = Note(type = noteType, id = _noteId),
        status = if (_create) MutableNoteUiState.Status.NEW else MutableNoteUiState.Status.PLACEHOLDER,
    )

    init {
        launchOnIOThread {
            if (!_create) {
                val images = repository.listImagesByNoteId(_noteId)
                val note = repository.getNote(_noteId)

                withContext(Dispatchers.Main) {
                    noteUiState.refreshFromNote(note)
                    noteUiState.status = MutableNoteUiState.Status.REGULAR
                    for (image in images) _images.value += MutableImageUiState(image = image, isNew = false)
                }
            } else {
                repository.saveMutableNoteUiState(noteUiState)
            }
            onInit()
            saveUndoState()
        }
    }

    open suspend fun onInit() {}
    open suspend fun onSave() {}

    abstract fun applyUndoState(idx: Int)
    abstract fun saveUndoState()

    fun deleteSelectedImages() {
        val states = _images.value.filter { it.isSelected }

        if (states.isNotEmpty()) {
            _images.value -= states
            saveUndoState()
            launchOnIOThread { repository.deleteImages(states.map { it.filename }) }
        }
    }

    fun deselectAllImages() {
        for (image in _images.value.filter { it.isSelected }) {
            image.isSelected = false
        }
    }

    fun insertImage(uri: Uri) = launchOnIOThread {
        repository.uriToImageData(uri)?.let { data ->
            val image = data.toImage(noteId = _noteId, position = _images.value.maxOfOrNull { it.position + 1 } ?: 0)

            withContext(Dispatchers.Main) {
                _images.value += MutableImageUiState(image = image, isNew = true)
            }
            saveUndoState()
        }
    }

    fun redo() {
        _undoStateIdx.value?.also {
            if (it + 1 > -1 && it + 1 <= _undoStates.value.lastIndex) applyUndoState(it + 1)
        }
    }

    fun save() {
        launchOnIOThread {
            repository.saveMutableNoteUiState(noteUiState)
            _images.value.save(repository::saveImages)
            onSave()
        }
    }

    fun saveNote() {
        launchOnIOThread {
            repository.saveMutableNoteUiState(noteUiState)
            saveUndoState()
        }
    }

    fun selectAllImages() {
        for (image in _images.value.filter { !it.isSelected }) {
            image.isSelected = true
        }
    }

    fun setNoteColor(key: NoteColorKey) {
        noteUiState.colorKey = key
        saveUndoState()
    }

    fun toggleImageSelected(filename: String) {
        _images.value.find { it.filename == filename }?.also {
            it.isSelected = !it.isSelected
        }
    }

    fun undo() {
        _undoStateIdx.value?.also {
            if (it - 1 > -1 && it - 1 <= _undoStates.value.lastIndex) applyUndoState(it - 1)
        }
    }
}
