package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.uistate.MutableImageUiState
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
    data class UndoState(val note: Note?, val images: List<MutableImageUiState>)

    override fun applyUndoState(idx: Int) {
        val images = _undoStates.value[idx].images
        val deletedImageFilenames = _images.value.map { it.filename }.toSet().minus(images.map { it.filename })

        _undoStates.value[idx].note?.also { noteUiState.refreshFromNote(it) }
        _images.value = images
        if (deletedImageFilenames.isNotEmpty()) launchOnIOThread { repository.deleteImages(deletedImageFilenames) }
        _undoStateIdx.value = idx
    }

    override fun saveUndoState() {
        _undoStateIdx.value?.also { _undoStates.value = _undoStates.value.take(it + 1) }
        _undoStates.value += UndoState(
            note = if (!noteUiState.isReadOnly) noteUiState.toNote() else null,
            images = _images.value.clone()
        )
        _undoStateIdx.value = (_undoStateIdx.value ?: -1) + 1
    }
}
