package us.huseli.retain.viewmodels

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.ILogger
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.uistate.MutableChecklistItemUiState
import us.huseli.retain.dataclasses.uistate.MutableImageUiState
import us.huseli.retain.dataclasses.uistate.clone
import us.huseli.retain.dataclasses.uistate.save
import us.huseli.retain.repositories.NoteRepository
import us.huseli.retaintheme.extensions.launchOnIOThread
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.map
import kotlin.collections.plus

@HiltViewModel
class ChecklistNoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle,
) : AbstractNoteViewModel<ChecklistNoteViewModel.UndoState>(
    repository = repository,
    savedStateHandle = savedStateHandle,
    noteType = NoteType.CHECKLIST,
), ILogger {
    data class UndoState(
        val note: Note?,
        val checklistItems: List<MutableChecklistItemUiState>,
        val images: List<MutableImageUiState>,
    )

    private val _focusedItemId = MutableStateFlow<UUID?>(null)
    private val _itemUiStates = MutableStateFlow<List<MutableChecklistItemUiState>>(emptyList())

    val itemUiStates = combine(_itemUiStates, _focusedItemId) { states, focusedId ->
        states.onEach { it.isFocused = it.id == focusedId }
    }.stateWhileSubscribed(emptyList())

    override fun applyUndoState(idx: Int) {
        val items = _undoStates.value[idx].checklistItems
        val images = _undoStates.value[idx].images
        val deletedItemIds = _itemUiStates.value.map { it.id }.toSet().minus(items.map { it.id })
        val deletedImageFilenames = _images.value.map { it.filename }.toSet().minus(images.map { it.filename })

        _undoStates.value[idx].note?.also { noteUiState.refreshFromNote(it) }
        _itemUiStates.value = items
        _images.value = images
        if (deletedItemIds.isNotEmpty()) launchOnIOThread { repository.deleteChecklistItems(deletedItemIds) }
        if (deletedImageFilenames.isNotEmpty()) launchOnIOThread { repository.deleteImages(deletedImageFilenames) }
        _undoStateIdx.value = idx
    }

    fun deleteCheckedItems() {
        val states = _itemUiStates.value.filter { it.isChecked }

        if (states.isNotEmpty()) {
            _itemUiStates.value -= states
            saveUndoState()
            launchOnIOThread { repository.deleteChecklistItems(states.map { it.id }) }
        }
    }

    fun deleteChecklistItem(itemId: UUID) {
        _itemUiStates.value.find { it.id == itemId }?.also {
            _itemUiStates.value -= it
            saveUndoState()
            launchOnIOThread { repository.deleteChecklistItem(itemId) }
        }
    }

    fun insertChecklistItem(position: Int = 0, isChecked: Boolean = false, text: String = "") {
        val item = ChecklistItem(text = text, noteId = _noteId, checked = isChecked, position = position)

        _itemUiStates.value.filter { it.position >= position }.forEach { it.position++ }
        _focusedItemId.value = item.id
        _itemUiStates.value += MutableChecklistItemUiState(item = item, isFocused = true, isNew = true)
        saveUndoState()
    }

    override suspend fun onInit() {
        val items = repository.listChecklistItemsByNoteId(_noteId)

        withContext(Dispatchers.Main) {
            _itemUiStates.value = items.map { MutableChecklistItemUiState(item = it, isNew = false) }.sorted()
            // for (item in items) _itemUiStates.value += MutableChecklistItemUiState(item = item, isNew = false)
        }
    }

    override suspend fun onSave() {
        _itemUiStates.value.save(repository::saveChecklistItems)
    }

    fun saveChecklistItem(itemId: UUID) {
        _itemUiStates.value.find { it.id == itemId }?.also { state ->
            launchOnIOThread {
                repository.saveMutableNoteUiState(noteUiState)
                state.save(repository::saveChecklistItem)
                saveUndoState()
            }
        }
    }

    override fun saveUndoState() {
        _undoStateIdx.value?.also { _undoStates.value = _undoStates.value.take(it + 1) }
        _undoStates.value += UndoState(
            note = if (!noteUiState.isReadOnly) noteUiState.toNote() else null,
            checklistItems = _itemUiStates.value.clone(),
            images = _images.value.clone(),
        )
        _undoStateIdx.value = (_undoStateIdx.value ?: -1) + 1
    }

    fun setChecklistItemIsChecked(itemId: UUID, value: Boolean) {
        _itemUiStates.value.find { it.id == itemId && it.isChecked != value }?.also {
            it.isChecked = value
            sortItems()
            saveUndoState()
        }
    }

    fun setFocusedChecklistItemId(itemId: UUID?) {
        _focusedItemId.value = itemId
    }

    private fun sortItems() {
        _itemUiStates.value = _itemUiStates.value.sorted()
    }

    fun switchChecklistItemPositions(fromPos: LazyListItemInfo, toPos: LazyListItemInfo) {
        /**
         * We cannot use ItemPosition.index because the lazy column contains
         * a whole bunch of other junk than checklist items.
         */
        val from = _itemUiStates.value.find { it.id == fromPos.key }
        val to = _itemUiStates.value.find { it.id == toPos.key }

        log(
            message = "switchChecklistItemPositions: from=${from?.text}, from.id = ${from?.id}, to=${to?.text}, to.id=${to?.id}",
            tag = "onMove"
        )

        if (from != null && to != null) {
            val fromPosition = from.position

            from.position = to.position
            to.position = fromPosition
            sortItems()
            saveUndoState()
        }
    }

    fun toggleShowCheckedItems() {
        noteUiState.showChecked = !noteUiState.showChecked
    }

    fun uncheckAllItems() {
        for (item in _itemUiStates.value.filter { it.isChecked }) {
            item.isChecked = false
        }
        sortItems()
        saveUndoState()
    }
}
