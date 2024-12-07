package us.huseli.retain.viewmodels

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.dataclasses.uistate.ChecklistItemUiState
import us.huseli.retain.dataclasses.uistate.ImageUiState
import us.huseli.retain.dataclasses.uistate.save
import us.huseli.retain.dataclasses.uistate.toImages
import us.huseli.retain.dataclasses.uistate.toItems
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
) {
    data class UndoState(
        val note: Note?,
        val checklistItems: List<ChecklistItem>,
        val images: List<Image>,
    )

    private val _focusedItemId = MutableStateFlow<UUID?>(null)
    private val _itemUiStates = MutableStateFlow<List<ChecklistItemUiState>>(emptyList())

    val itemUiStates = combine(_itemUiStates, _focusedItemId) { states, focusedId ->
        states.onEach { it.isFocused = it.id == focusedId }
    }.stateWhileSubscribed(emptyList())

    override val shouldSaveUndoState: Boolean
        get() {
            val state = _undoStates.value.getOrNull(_undoStateIdx.value)

            if (state == null) return noteUiState.isChanged ||
                _imageUiStates.value.any { it.isChanged } ||
                _itemUiStates.value.any { it.isChanged }
            return state.note != noteUiState.toNote() ||
                state.checklistItems != _itemUiStates.value ||
                state.images != _imageUiStates.value
        }

    override fun applyUndoState(idx: Int) {
        val undoState = _undoStates.value[idx]
        val deletedItemIds = _itemUiStates.value.map { it.id }.toSet().minus(undoState.checklistItems.map { it.id })
        val deletedImageFilenames =
            _imageUiStates.value.map { it.filename }.toSet().minus(undoState.images.map { it.filename })

        undoState.note?.also { noteUiState.onNoteFetched(it) }
        _itemUiStates.value =
            undoState.checklistItems.map { ChecklistItemUiState(item = it, isFocused = _focusedItemId.value == it.id) }
        _imageUiStates.value = undoState.images.map { ImageUiState(image = it) }
        if (deletedItemIds.isNotEmpty()) launchOnIOThread { repository.deleteChecklistItems(deletedItemIds) }
        if (deletedImageFilenames.isNotEmpty()) launchOnIOThread { repository.deleteImages(deletedImageFilenames) }
        _undoStateIdx.value = idx
    }

    fun deleteChecklistItem(itemId: UUID) {
        _itemUiStates.value.find { it.id == itemId }?.also {
            _itemUiStates.value -= it
            saveUndoState()
            launchOnIOThread { repository.deleteChecklistItem(itemId) }
        }
    }

    fun deleteCheckedItems() {
        val states = _itemUiStates.value.filter { it.isChecked }

        if (states.isNotEmpty()) {
            _itemUiStates.value -= states
            saveUndoState()
            launchOnIOThread { repository.deleteChecklistItems(states.map { it.id }) }
        }
    }

    fun insertChecklistItem(position: Int = 0, isChecked: Boolean = false, text: String = "") {
        val item = ChecklistItem(text = text, noteId = _noteId, isChecked = isChecked, position = position)

        _itemUiStates.value.filter { it.position >= position }.forEach { it.position++ }
        _focusedItemId.value = item.id
        _itemUiStates.value += ChecklistItemUiState(item = item, isFocused = true, isNew = true)
        saveUndoState()
    }

    override suspend fun onInit() {
        val items = repository.listChecklistItemsByNoteId(_noteId)

        withContext(Dispatchers.Main) {
            _itemUiStates.value = items.map { ChecklistItemUiState(item = it, isNew = false) }.sorted()
        }
    }

    override suspend fun onSave() {
        _itemUiStates.value.save(repository::saveChecklistItems)
    }

    fun saveChecklistItem(itemId: UUID) {
        _itemUiStates.value.find { it.id == itemId }?.also { state ->
            launchOnIOThread {
                repository.saveNoteUiState(noteUiState)
                state.save(repository::saveChecklistItem)
                saveUndoState()
            }
        }
    }

    override fun saveUndoState() {
        val undoState = UndoState(
            note = if (!noteUiState.isReadOnly) noteUiState.toNote() else null,
            checklistItems = _itemUiStates.value.toItems(),
            images = _imageUiStates.value.toImages(),
        )

        _undoStates.value = _undoStates.value.take(_undoStateIdx.value + 1)
        if (_undoStates.value.size > 50) {
            _undoStates.value = _undoStates.value.take(50)
            _undoStateIdx.value = 49
        }
        _undoStates.value += undoState
        _undoStateIdx.value += 1
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
