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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    override val logger: Logger
) : ViewModel(), LogInterface {
    private val _selectedNoteIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    val notes = _notes.asStateFlow()
    val checklistItems: Flow<List<ChecklistItem>> = repository.checklistItems
    val bitmapImages: Flow<List<BitmapImage>> = repository.bitmapImages

    val selectedNoteIds: Flow<Set<UUID>> = combine(notes, _selectedNoteIds) { notes, selectedIds ->
        selectedIds.intersect(notes.map { it.id }.toSet()).also { log("selectedNoteIds=$it") }
    }

    init {
        viewModelScope.launch {
            repository.notes.collect { notes ->
                _notes.value = notes
            }
        }
    }

    val deleteNotes = { ids: Collection<UUID> ->
        viewModelScope.launch {
            log("deleteNotes: $ids")
            repository.deleteNotes(ids)
        }
    }

    val selectNote = { note: Note ->
        _selectedNoteIds.value += note.id
        log("selectNote: note=$note, _selectedNoteIds.value=${_selectedNoteIds.value}")
    }

    val deselectNote = { note: Note ->
        _selectedNoteIds.value -= note.id
        log("deselectNote: note=$note, _selectedNoteIds.value=${_selectedNoteIds.value}")
    }

    val deselectAllNotes = {
        _selectedNoteIds.value = emptySet()
    }

    val saveTextNote: (Boolean, Note, Collection<Image>) -> Unit =
        { shouldSave, note, images ->
            if (shouldSave) {
                viewModelScope.launch {
                    repository.upsertNote(note, images)
                }
            }
        }

    val saveChecklistNote: (Boolean, Note, Collection<Image>, Collection<ChecklistItem>) -> Unit =
        { shouldSave, note, images, checklistItems ->
            if (shouldSave) {
                viewModelScope.launch {
                    repository.upsertNote(note, checklistItems, images)
                }
            }
        }

    fun switchNotePositions(from: ItemPosition, to: ItemPosition) {
        _notes.value = _notes.value.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun saveNotePositions() = viewModelScope.launch {
        repository.updateNotePositions(
            _notes.value.mapIndexedNotNull { index, note ->
                if (note.position != index) note.copy(position = index) else null
            }
        )
    }
}
