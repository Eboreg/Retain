package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.Note
import java.util.UUID

abstract class BaseEditNoteViewModel(
    savedStateHandle: SavedStateHandle,
    internal val repository: NoteRepository,
) : ViewModel() {
    val noteId: UUID = UUID.fromString(savedStateHandle.get<String>(NOTE_ID_SAVED_STATE_KEY)!!)
    var note: Note? = null

    abstract fun receiveNote(note: Note)

    init {
        viewModelScope.launch {
            repository.getNote(noteId).transformWhile {
                if (it != null) emit(it)
                it == null
            }.collect {
                note = it
                receiveNote(it)
            }
        }
    }

    companion object {
        private const val NOTE_ID_SAVED_STATE_KEY = "noteId"
    }
}