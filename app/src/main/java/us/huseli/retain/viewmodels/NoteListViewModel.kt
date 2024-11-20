package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.retain.ILogger
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.repositories.NoteRepository
import us.huseli.retain.repositories.SyncBackendRepository
import us.huseli.retain.syncbackend.tasks.result.OperationTaskResult
import us.huseli.retaintheme.extensions.launchOnIOThread
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val syncBackendRepository: SyncBackendRepository,
) : ViewModel(), ILogger {
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
        launchOnIOThread {
            repository.deleteTrashedNotes()
        }

        launchOnIOThread {
            combine(repository.pojos, _showArchive) { pojos, showArchive ->
                pojos.filter { it.note.isArchived == showArchive }
            }.distinctUntilChanged().collect { _pojos.value = it }
        }
    }

    fun archiveSelectedNotes() = launchOnIOThread {
        val selected = _pojos.value.filter { _selectedNoteIds.value.contains(it.note.id) }.map { it.note }

        deselectAllNotes()
        if (selected.isNotEmpty()) {
            repository.archiveNotes(selected)
            log("Archived ${selected.size} notes.", showSnackbar = true)
        }
    }

    fun deselectAllNotes() {
        _selectedNoteIds.value = emptySet()
    }

    fun reallyTrashNotes() = launchOnIOThread {
        _trashedPojos.value = emptySet()
        repository.deleteTrashedNotes()
        syncBackendRepository.uploadNotes()
    }

    fun saveNotePositions() = launchOnIOThread {
        repository.updateNotePositions(
            _pojos.value.mapIndexedNotNull { index, pojo ->
                if (pojo.note.position != index) pojo.note.copy(position = index) else null
            }
        )
    }

    fun selectAllNotes() {
        _selectedNoteIds.value = _pojos.value.map { it.note.id }.toSet()
    }

    fun switchNotePositions(from: Int, to: Int) {
        _pojos.value = _pojos.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun syncBackend() = launchOnIOThread { syncBackendRepository.sync() }

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
        launchOnIOThread {
            repository.trashNotes(_trashedPojos.value.map { it.note })
        }
    }

    fun unarchiveSelectedNotes() {
        val selected = _pojos.value.filter { _selectedNoteIds.value.contains(it.note.id) }.map { it.note }

        deselectAllNotes()
        if (selected.isNotEmpty()) {
            launchOnIOThread {
                repository.unarchiveNotes(selected)
                log("Unarchived ${selected.size} notes.", showSnackbar = true)
            }
        }
    }

    fun undoTrashNotes() = launchOnIOThread {
        repository.updateNotes(_trashedPojos.value.map { it.note })
        _trashedPojos.value = emptySet()
    }

    suspend fun uploadNotes(onResult: ((OperationTaskResult) -> Unit)? = null) {
        syncBackendRepository.uploadNotes(onResult)
    }
}
