package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.uistate.ImageUiState
import us.huseli.retain.dataclasses.uistate.clone
import us.huseli.retain.repositories.NoteRepository
import us.huseli.retain.viewmodels.TextNoteViewModel.UndoState
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class TextNoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle,
) : AbstractNoteViewModel<UndoState>(
    repository = repository,
    savedStateHandle = savedStateHandle,
    noteType = NoteType.TEXT,
) {
    data class UndoState(val note: Note?, val imageUiStates: List<ImageUiState>)

    override val shouldSaveUndoState: Boolean
        get() {
            val state = _undoStates.value.getOrNull(_undoStateIdx.value)

            if (state == null) return noteUiState.isChanged || _imageUiStates.value.any { it.isChanged }
            return state.note != noteUiState.toNote() || state.imageUiStates != _imageUiStates.value
        }

    override fun applyUndoState(idx: Int) {
        val images = _undoStates.value[idx].imageUiStates
        val deletedImageFilenames = _imageUiStates.value.map { it.filename }.toSet().minus(images.map { it.filename })

        _undoStates.value[idx].note?.also { noteUiState.onNoteFetched(it) }
        _imageUiStates.value = images
        if (deletedImageFilenames.isNotEmpty()) launchOnIOThread { repository.deleteImages(deletedImageFilenames) }
        _undoStateIdx.value = idx
    }

    override fun saveUndoState() {
        _undoStates.value = _undoStates.value.take(_undoStateIdx.value + 1)
        if (_undoStates.value.size > 50) {
            _undoStates.value = _undoStates.value.take(50)
            _undoStateIdx.value = 49
        }
        _undoStates.value += UndoState(
            note = if (!noteUiState.isReadOnly) noteUiState.toNote() else null,
            imageUiStates = _imageUiStates.value.clone()
        )
        _undoStateIdx.value += 1
    }
}
