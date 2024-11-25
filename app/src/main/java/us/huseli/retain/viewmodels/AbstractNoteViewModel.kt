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
import us.huseli.retain.dataclasses.uistate.ImageUiState
import us.huseli.retain.dataclasses.uistate.NoteUiState
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
    protected val _imageUiStates = MutableStateFlow<List<ImageUiState>>(emptyList())
    protected val _noteId: UUID =
        savedStateHandle.get<String>(NAV_ARG_NOTE_ID)?.let { UUID.fromString(it) } ?: UUID.randomUUID()
    protected val _undoStateIdx = MutableStateFlow<Int>(-1)
    protected val _undoStates = MutableStateFlow<List<UndoStateType>>(emptyList())

    val imageUiStates = _imageUiStates.asStateFlow()
    val isRedoPossible = combine(_undoStates, _undoStateIdx) { states, idx -> idx < states.lastIndex }
        .stateWhileSubscribed(false)
    val isUndoPossible = _undoStateIdx.map { it > 0 }.stateWhileSubscribed(false)
    val noteUiState = NoteUiState(
        note = Note(type = noteType, id = _noteId),
        status = if (_create) NoteUiState.Status.NEW else NoteUiState.Status.PLACEHOLDER,
    )

    abstract val shouldSaveUndoState: Boolean

    init {
        launchOnIOThread {
            if (!_create) {
                val images = repository.listImagesByNoteId(_noteId)
                val note = repository.getNote(_noteId)

                withContext(Dispatchers.Main) {
                    noteUiState.onNoteFetched(note)
                    noteUiState.status = NoteUiState.Status.REGULAR
                    for (image in images) _imageUiStates.value += ImageUiState(image = image, isNew = false)
                }
            } else {
                repository.saveNoteUiState(noteUiState)
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
        val states = _imageUiStates.value.filter { it.isSelected }

        if (states.isNotEmpty()) {
            _imageUiStates.value -= states
            saveUndoState()
            launchOnIOThread { repository.deleteImages(states.map { it.filename }) }
        }
    }

    fun deselectAllImages() {
        for (image in _imageUiStates.value.filter { it.isSelected }) {
            image.isSelected = false
        }
    }

    fun insertImage(uri: Uri) = launchOnIOThread {
        repository.uriToImageData(uri)?.let { data ->
            val image =
                data.toImage(noteId = _noteId, position = _imageUiStates.value.maxOfOrNull { it.position + 1 } ?: 0)

            withContext(Dispatchers.Main) {
                _imageUiStates.value += ImageUiState(image = image, isNew = true)
            }
            saveUndoState()
        }
    }

    fun redo() {
        _undoStateIdx.value.also {
            if (it + 1 > -1 && it + 1 <= _undoStates.value.lastIndex) applyUndoState(it + 1)
        }
    }

    fun save() {
        launchOnIOThread {
            repository.saveNoteUiState(noteUiState)
            _imageUiStates.value.save(repository::saveImages)
            onSave()
        }
    }

    fun saveNote() {
        launchOnIOThread {
            repository.saveNoteUiState(noteUiState)
            saveUndoState()
        }
    }

    fun selectAllImages() {
        for (image in _imageUiStates.value.filter { !it.isSelected }) {
            image.isSelected = true
        }
    }

    fun setNoteColor(key: NoteColorKey) {
        noteUiState.colorKey = key
        saveUndoState()
    }

    fun toggleImageSelected(filename: String) {
        _imageUiStates.value.find { it.filename == filename }?.also {
            it.isSelected = !it.isSelected
        }
    }

    fun undo() {
        _undoStateIdx.value.also {
            if (it - 1 > -1 && it - 1 <= _undoStates.value.lastIndex) applyUndoState(it - 1)
        }
    }
}
