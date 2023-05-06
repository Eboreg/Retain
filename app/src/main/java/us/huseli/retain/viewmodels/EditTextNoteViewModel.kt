package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.Note
import javax.inject.Inject

@HiltViewModel
class EditTextNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
) : BaseEditNoteViewModel(savedStateHandle, repository) {
    val title = MutableStateFlow("")
    val text = MutableStateFlow("")

    fun save() = viewModelScope.launch {
        repository.upsertTextNote(noteId, title.value, text.value)
    }

    override fun receiveNote(note: Note) {
        title.value = note.title
        text.value = note.text
    }
}
