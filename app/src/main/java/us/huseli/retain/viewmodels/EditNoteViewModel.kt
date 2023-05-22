package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.data.NoteRepository
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
) : BaseEditNoteViewModel(savedStateHandle, repository, NoteType.TEXT)
