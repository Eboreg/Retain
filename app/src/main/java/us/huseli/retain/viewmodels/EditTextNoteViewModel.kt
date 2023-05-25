package us.huseli.retain.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import javax.inject.Inject

@HiltViewModel
class EditTextNoteViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(context, savedStateHandle, repository, NoteType.TEXT) {
    init {
        _bitmapImages.value = repository.bitmapImages.value.filter { it.image.noteId == _noteId }

        viewModelScope.launch {
            @Suppress("Destructure")
            repository.getCombo(_noteId)?.let { combo ->
                _note.value = combo.note
                _isNew = false
                _isDirty = false
            }
        }
    }
}
