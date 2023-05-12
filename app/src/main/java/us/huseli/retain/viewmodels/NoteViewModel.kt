package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import us.huseli.retain.LogMessage
import us.huseli.retain.Logger
import us.huseli.retain.LoggingObject
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.ImageWithBitmap
import us.huseli.retain.data.entities.Note
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    override var logger: Logger?
) : ViewModel(), LoggingObject {
    private val _selectedNoteIds = MutableStateFlow<Set<UUID>>(emptySet())

    val notes: Flow<List<Note>> = repository.notes
    val checklistItems: Flow<List<ChecklistItem>> = repository.checklistItems
    val latestLogMessage: Flow<LogMessage?> = logger?.latestLogMessage ?: emptyFlow()
    val logMessages: SharedFlow<LogMessage?> = logger?.logMessages ?: MutableSharedFlow()
    val imagesWithBitmap: Flow<List<ImageWithBitmap>> = repository.flowImagesWithBitmap()

    val selectedNoteIds: Flow<Set<UUID>> = combine(notes, _selectedNoteIds) { notes, selectedIds ->
        selectedIds.intersect(notes.map { it.id }.toSet()).also { log("selectedNoteIds=$it") }
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

    val saveTextNote: (Boolean, UUID, String, String, Int) -> Unit =
        { shouldSave, id, title, text, colorIdx ->
            if (shouldSave) {
                viewModelScope.launch {
                    repository.upsertNote(id, title, text, true, colorIdx)
                }
            }
        }

    val saveChecklistNote: (Boolean, UUID, String, Boolean, Int, Collection<ChecklistItem>) -> Unit =
        { shouldSave, id, title, showChecked, colorIdx, updatedItems ->
            if (shouldSave) {
                viewModelScope.launch {
                    repository.upsertNote(id, title, "", showChecked, colorIdx)
                    repository.updateChecklistItems(updatedItems)
                }
            }
        }
}
