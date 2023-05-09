package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import javax.inject.Inject

@HiltViewModel
class EditChecklistNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
) : BaseEditNoteViewModel(savedStateHandle, repository) {
    private var _noteSaved = false
    val title = MutableStateFlow("")
    val showChecked = MutableStateFlow(true)
    val checklistItems = repository.loadChecklistItems(noteId)

    private suspend fun save() {
        repository.upsertChecklistNote(noteId, title.value, showChecked.value)
        _noteSaved = true
    }

    fun deleteItem(item: ChecklistItem) = viewModelScope.launch {
        repository.deleteChecklistItem(item)
    }

    fun insertItem(text: String, checked: Boolean, position: Int) = viewModelScope.launch {
        if (!_noteSaved) save()
        repository.insertChecklistItem(noteId, text, checked, position)
    }

    fun updateItem(item: ChecklistItem, text: String, checked: Boolean, position: Int) = viewModelScope.launch {
        repository.updateChecklistItem(item, text, checked, position)
    }

    override fun receiveNote(note: Note) {
        title.value = note.title
        showChecked.value = note.showChecked
    }
}
