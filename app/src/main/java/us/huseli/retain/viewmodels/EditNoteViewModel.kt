package us.huseli.retain.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.data.NoteRepository
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
) : BaseEditNoteViewModel(context, savedStateHandle, repository, NoteType.TEXT)
