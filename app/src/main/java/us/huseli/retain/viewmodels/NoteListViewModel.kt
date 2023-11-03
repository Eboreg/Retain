package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.repositories.NoteRepository
import us.huseli.retain.repositories.SyncBackendRepository
import us.huseli.retain.syncbackend.tasks.OperationTaskResult
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val syncBackendRepository: SyncBackendRepository,
    override val logger: Logger
) : ViewModel(), LogInterface {
    private val _selectedNoteIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val _trashedPojos = MutableStateFlow<Set<NotePojo>>(emptySet())
    private val _pojos = MutableStateFlow<List<NotePojo>>(emptyList())
    private val _showArchive = MutableStateFlow(false)

    val syncBackend = syncBackendRepository.syncBackend
    val isSyncBackendSyncing = syncBackendRepository.isSyncing
    val showArchive = _showArchive.asStateFlow()
    val trashedPojos = _trashedPojos.asStateFlow()
    val isSelectEnabled = _selectedNoteIds.map { it.isNotEmpty() }
    val selectedNoteIds = _selectedNoteIds.asStateFlow()
    val pojos = _pojos.asStateFlow()

    init {
        viewModelScope.launch {
            repository.deleteTrashedNotes()
        }

        viewModelScope.launch {
            combine(repository.pojos, _showArchive) { pojos, showArchive ->
                pojos.filter { it.note.isArchived == showArchive }
            }.distinctUntilChanged().collect { _pojos.value = it }
        }
    }

    fun archiveSelectedNotes() = viewModelScope.launch {
        val selected = _pojos.value.filter { _selectedNoteIds.value.contains(it.note.id) }.map { it.note }

        deselectAllNotes()
        if (selected.isNotEmpty()) {
            repository.archiveNotes(selected)
            log("Archived ${selected.size} notes.", showInSnackbar = true)
        }
    }

    fun deselectAllNotes() {
        _selectedNoteIds.value = emptySet()
    }

    fun reallyTrashNotes() = viewModelScope.launch {
        _trashedPojos.value = emptySet()
        repository.deleteTrashedNotes()
        syncBackendRepository.uploadNotes()
    }

    fun saveNotePositions() = viewModelScope.launch {
        repository.updateNotePositions(
            _pojos.value.mapIndexedNotNull { index, pojo ->
                if (pojo.note.position != index) pojo.note.copy(position = index) else null
            }
        )
    }

    fun selectAllNotes() {
        _selectedNoteIds.value = _pojos.value.map { it.note.id }.toSet()
    }

    fun switchNotePositions(from: ItemPosition, to: ItemPosition) {
        _pojos.value = _pojos.value.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun syncBackend() = viewModelScope.launch { syncBackendRepository.sync() }

    fun toggleNoteSelected(noteId: UUID) {
        if (_selectedNoteIds.value.contains(noteId)) _selectedNoteIds.value -= noteId
        else _selectedNoteIds.value += noteId
    }

    fun toggleShowArchive() {
        _showArchive.value = !_showArchive.value
    }

    fun trashSelectedNotes() {
        _trashedPojos.value = _pojos.value.filter { _selectedNoteIds.value.contains(it.note.id) }.toSet()
        deselectAllNotes()
        viewModelScope.launch {
            repository.trashNotes(_trashedPojos.value.map { it.note })
        }
    }

    fun unarchiveSelectedNotes() {
        val selected = _pojos.value.filter { _selectedNoteIds.value.contains(it.note.id) }.map { it.note }

        deselectAllNotes()
        if (selected.isNotEmpty()) {
            viewModelScope.launch {
                repository.unarchiveNotes(selected)
                log("Unarchived ${selected.size} notes.", showInSnackbar = true)
            }
        }
    }

    fun undoTrashNotes() = viewModelScope.launch {
        repository.updateNotes(_trashedPojos.value.map { it.note })
        _trashedPojos.value = emptySet()
    }

    suspend fun uploadNotes(onResult: ((OperationTaskResult) -> Unit)? = null) {
        syncBackendRepository.uploadNotes(onResult)
    }
}
