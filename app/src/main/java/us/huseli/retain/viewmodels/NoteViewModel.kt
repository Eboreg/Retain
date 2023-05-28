package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.NextCloudRepository
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min

data class NoteCardChecklistData(
    val noteId: UUID,
    val shownChecklistItems: List<ChecklistItem>,
    val hiddenChecklistItemCount: Int,
    val hiddenChecklistItemAllChecked: Boolean,
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val nextCloudRepository: NextCloudRepository,
    override val logger: Logger
) : ViewModel(), LogInterface {
    private val _selectedNoteIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _trashedNotes = MutableStateFlow<Set<Note>>(emptySet())
    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    private val _isSelectEnabled: Boolean
        get() = _selectedNoteIds.value.isNotEmpty()

    val trashedNoteCount = _trashedNotes.map { it.size }
    val isSelectEnabled = _selectedNoteIds.map { it.isNotEmpty() }
    val selectedNoteIds = _selectedNoteIds.asStateFlow()
    val notes = _notes.asStateFlow()
    val images: Flow<List<Image>> = repository.images
    val checklistData = repository.checklistItems.map { items ->
        items.groupBy { it.noteId }.map { (noteId, noteItems) ->
            val shownItems = noteItems.subList(0, min(noteItems.size, 5))
            val hiddenItems = noteItems.minus(shownItems.toSet())

            NoteCardChecklistData(
                noteId = noteId,
                shownChecklistItems = shownItems,
                hiddenChecklistItemCount = hiddenItems.size,
                hiddenChecklistItemAllChecked = hiddenItems.all { it.checked },
            )
        }
    }

    init {
        viewModelScope.launch {
            repository.notes.collect { notes ->
                _notes.value = notes
            }
        }
    }

    fun reallyTrashNotes() {
        val trashedNotes = _trashedNotes.value

        _trashedNotes.value = emptySet()
        viewModelScope.launch {
            nextCloudRepository.upload(repository.listCombos(trashedNotes.map { it.id }))
        }
    }

    fun deselectAllNotes() {
        _selectedNoteIds.value = emptySet()
    }

    fun deselectNote(noteId: UUID) {
        _selectedNoteIds.value -= noteId
        log("deselectNote: noteId=$noteId, _selectedNoteIds.value=${_selectedNoteIds.value}")
    }

    fun save(note: Note?, checklistItems: List<ChecklistItem>, images: List<Image>) = viewModelScope.launch {
        log("save(): note=$note, checklistItems=$checklistItems, images=$images")
        note?.let { repository.upsertNote(it) }
        repository.upsertChecklistItems(checklistItems)
        repository.upsertImages(images)
    }

    fun saveNotePositions() = viewModelScope.launch {
        repository.updateNotePositions(
            _notes.value.mapIndexedNotNull { index, note ->
                if (note.position != index) note.copy(position = index) else null
            }
        )
    }

    fun selectNote(noteId: UUID) {
        _selectedNoteIds.value += noteId
        log("selectNote: note=$noteId, _selectedNoteIds.value=${_selectedNoteIds.value}")
    }

    fun switchNotePositions(from: ItemPosition, to: ItemPosition) {
        _notes.value = _notes.value.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun toggleNoteSelected(noteId: UUID) {
        if (_isSelectEnabled) {
            if (_selectedNoteIds.value.contains(noteId)) deselectNote(noteId)
            else selectNote(noteId)
        }
    }

    fun trashSelectedNotes() {
        _trashedNotes.value = _notes.value.filter { _selectedNoteIds.value.contains(it.id) }.toSet()
        deselectAllNotes()
        viewModelScope.launch {
            repository.trashNotes(_trashedNotes.value)
        }
    }

    fun undoTrashNotes() = viewModelScope.launch {
        repository.upsertNotes(_trashedNotes.value)
        _trashedNotes.value = emptySet()
    }

    fun uploadCombo(combo: NoteCombo?) {
        combo?.let { nextCloudRepository.upload(it) }
    }
}
