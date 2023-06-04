package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.NextCloudRepository
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
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
    private val _showArchive = MutableStateFlow(false)

    val showArchive = _showArchive.asStateFlow()
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
        // Just make sure note positions are set:
        viewModelScope.launch {
            var fetchCount = 0

            repository.notes.takeWhile { fetchCount++ == 0 }.collect { notes ->
                notes.toMutableList().mapIndexedNotNull { index, note ->
                    if (note.position != index) note.copy(position = index) else null
                }.also { repository.updateNotes(it) }
            }
        }

        viewModelScope.launch {
            repository.deleteTrashedNotes()
        }

        viewModelScope.launch {
            combine(repository.notes, _showArchive) { notes, showArchive ->
                notes.filter { it.isArchived == showArchive }
            }.collect { notes ->
                _notes.value = notes
            }
        }
    }

    fun archiveSelectedNotes() {
        val selectedNotes = _notes.value.filter { _selectedNoteIds.value.contains(it.id) }

        deselectAllNotes()
        if (selectedNotes.isNotEmpty()) {
            viewModelScope.launch {
                repository.archiveNotes(selectedNotes)
                log("Archived ${selectedNotes.size} notes.", showInSnackbar = true)
            }
        }
    }

    fun deselectAllNotes() {
        _selectedNoteIds.value = emptySet()
    }

    fun reallyTrashNotes() {
        _trashedNotes.value = emptySet()
        viewModelScope.launch {
            repository.deleteTrashedNotes()
            nextCloudRepository.uploadNotes()
        }
    }

    fun save(
        note: Note?,
        checklistItems: List<ChecklistItem>,
        images: List<Image>,
        deletedChecklistItemIds: List<UUID>,
        deletedImageIds: List<String>
    ) = viewModelScope.launch {
        log("save(): note=$note, checklistItems=$checklistItems, images=$images")
        note?.let { repository.upsertNote(it) }
        if (checklistItems.isNotEmpty()) repository.upsertChecklistItems(checklistItems)
        if (images.isNotEmpty()) repository.upsertImages(images)
        if (deletedChecklistItemIds.isNotEmpty()) repository.deleteChecklistItems(deletedChecklistItemIds)
        if (deletedImageIds.isNotEmpty()) {
            val deletedImages = repository.listImages(deletedImageIds)
            nextCloudRepository.removeImages(deletedImages)
            repository.deleteImages(deletedImages)
        }
    }

    fun saveNotePositions() = viewModelScope.launch {
        repository.updateNotePositions(
            _notes.value.mapIndexedNotNull { index, note ->
                if (note.position != index) note.copy(position = index) else null
            }
        )
    }

    fun selectAllNotes() {
        _selectedNoteIds.value = _notes.value.map { it.id }.toSet()
    }

    fun switchNotePositions(from: ItemPosition, to: ItemPosition) {
        _notes.value = _notes.value.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun toggleNoteSelected(noteId: UUID) {
        if (_selectedNoteIds.value.contains(noteId)) _selectedNoteIds.value -= noteId
        else _selectedNoteIds.value += noteId
    }

    fun toggleShowArchive() {
        _showArchive.value = !_showArchive.value
    }

    fun trashSelectedNotes() {
        _trashedNotes.value = _notes.value.filter { _selectedNoteIds.value.contains(it.id) }.toSet()
        deselectAllNotes()
        viewModelScope.launch {
            repository.trashNotes(_trashedNotes.value)
        }
    }

    fun unarchiveSelectedNotes() {
        val selectedNotes = _notes.value.filter { _selectedNoteIds.value.contains(it.id) }

        deselectAllNotes()
        if (selectedNotes.isNotEmpty()) {
            viewModelScope.launch {
                repository.unarchiveNotes(selectedNotes)
                log("Unarchived ${selectedNotes.size} notes.", showInSnackbar = true)
            }
        }
    }

    fun undoTrashNotes() = viewModelScope.launch {
        repository.updateNotes(_trashedNotes.value)
        _trashedNotes.value = emptySet()
    }

    fun uploadNotes() = viewModelScope.launch {
        nextCloudRepository.uploadNotes()
    }
}
