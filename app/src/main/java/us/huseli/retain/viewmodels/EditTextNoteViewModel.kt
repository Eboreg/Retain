package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import javax.inject.Inject

@HiltViewModel
class EditTextNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(savedStateHandle, repository, NoteType.TEXT)
