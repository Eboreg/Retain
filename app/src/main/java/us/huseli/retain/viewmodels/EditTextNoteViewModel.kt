package us.huseli.retain.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import us.huseli.retain.Logger
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.Note
import javax.inject.Inject

@HiltViewModel
class EditTextNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override var logger: Logger?
) : BaseEditNoteViewModel(savedStateHandle, repository), LoggingObject {
    val title = MutableStateFlow("")
    val text = MutableStateFlow("")

    override fun receiveNote(note: Note) {
        try {
            title.value = note.title
            text.value = note.text
        } catch (e: Exception) {
            log(e.toString(), Log.ERROR)
        }
    }
}
