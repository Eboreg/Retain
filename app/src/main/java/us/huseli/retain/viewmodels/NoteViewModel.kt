package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    override val logger: Logger
) : ViewModel(), LogInterface {
    private val _selectedNotes = MutableStateFlow<Set<Note>>(emptySet())
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    private val _trashedNotes = MutableStateFlow<List<Note>>(emptyList())

    val notes = _notes.asStateFlow()
    val checklistItems: Flow<List<ChecklistItem>> = repository.checklistItems
    val bitmapImages: Flow<List<BitmapImage>> = repository.bitmapImages
    val trashedNotes = _trashedNotes.asStateFlow()

    val selectedNotes: Flow<Set<Note>> = combine(notes, _selectedNotes) { notes, selectedNotes ->
        selectedNotes.intersect(notes.toSet())
    }

    init {
        viewModelScope.launch {
            repository.notes.collect { notes ->
                _notes.value = notes
            }
        }
    }

    fun clearTrashNotes() {
        _trashedNotes.value = emptyList()
    }

    fun deselectAllNotes() {
        _selectedNotes.value = emptySet()
    }

    fun deselectNote(note: Note) {
        _selectedNotes.value -= note
        log("deselectNote: note=$note, _selectedNotes.value=${_selectedNotes.value}")
    }

    fun saveNote(note: Note, images: Collection<Image>) = saveNote(note, images, emptyList())

    fun saveNote(note: Note, images: Collection<Image>, checklistItems: Collection<ChecklistItem>) =
        viewModelScope.launch {
            repository.upsertNote(note, checklistItems, images)
        }

    fun saveNotePositions() = viewModelScope.launch {
        repository.updateNotePositions(
            _notes.value.mapIndexedNotNull { index, note ->
                if (note.position != index) note.copy(position = index) else null
            }
        )
    }

    fun selectNote(note: Note) {
        _selectedNotes.value += note
        log("selectNote: note=$note, _selectedNotes.value=${_selectedNotes.value}")
    }

    fun switchNotePositions(from: ItemPosition, to: ItemPosition) {
        _notes.value = _notes.value.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun trashNotes(notes: Collection<Note>) {
        log("trashNotes: $notes")
        _trashedNotes.value = notes.toList()
        viewModelScope.launch {
            repository.trashNotes(notes)
        }
    }

    fun undoTrashNotes() = viewModelScope.launch {
        repository.upsertNotes(_trashedNotes.value)
        clearTrashNotes()
    }
}
