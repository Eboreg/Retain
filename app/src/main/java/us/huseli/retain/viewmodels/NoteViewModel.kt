package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(private val repository: NoteRepository) : ViewModel(), LoggingObject {
    private val _selectedNotes = MutableStateFlow<Set<Note>>(emptySet())

    val notes: Flow<List<Note>> = repository.notes

    val checklistItems: Flow<List<ChecklistItem>> = repository.checklistItems

    val selectedNotes: Flow<Set<Note>> = combine(notes, _selectedNotes) { notes, selected ->
        selected.intersect(notes.toSet())
    }

    fun deleteNotes(notes: Collection<Note>) = viewModelScope.launch {
        log("deleteNotes: $notes")
        repository.deleteNotes(notes)
    }

    val selectNote = { note: Note -> _selectedNotes.value += note }

    val deselectNote = { note: Note -> _selectedNotes.value -= note }

    val deselectAllNotes = { _selectedNotes.value = emptySet() }
}
