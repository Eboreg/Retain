package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditChecklistNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(savedStateHandle, repository, NoteType.CHECKLIST), LogInterface {
    private val _items = MutableStateFlow<List<ChecklistItem>>(emptyList())
    private val _trashedItems = MutableStateFlow<List<Pair<Int, ChecklistItem>>>(emptyList())

    val items = _items.asStateFlow()
    val trashedItems = _trashedItems.asStateFlow()

    init {
        viewModelScope.launch {
            _items.value = repository.listChecklistItems(noteId)
        }
    }

    fun clearTrashItems() {
        _trashedItems.value = emptyList()
    }

    fun deleteCheckedItems() {
        _trashedItems.value = _items.value.mapIndexedNotNull { index, item ->
            if (item.checked) Pair(index, item) else null
        }
        if (_trashedItems.value.isNotEmpty()) {
            _items.value = _items.value.filter { !it.checked }
            _isDirty = true
        }
    }

    fun deleteItem(item: ChecklistItem) {
        _items.value = _items.value.toMutableList().apply {
            val index = indexOf(item)

            if (index > -1) {
                removeAt(index)
                _trashedItems.value = listOf(Pair(index, item))
                _isDirty = true
            }
        }
    }

    fun insertItem(text: String, checked: Boolean, index: Int) {
        val item = ChecklistItem(text = text, checked = checked, noteId = noteId, position = index)

        log("insertItem($text, $checked, $index): inserting $item")
        _items.value = _items.value.toMutableList().apply { add(index, item) }
        updatePositions()
        _isDirty = true
    }

    override fun isEmpty() = super.isEmpty() && _items.value.isEmpty()

    fun switchItemPositions(from: ItemPosition, to: ItemPosition) {
        /**
         * We cannot use ItemPosition.index because the lazy column contains
         * a whole bunch of other junk than checklist items.
         */
        val fromIdx = _items.value.indexOfFirst { it.id == from.key }
        val toIdx = _items.value.indexOfFirst { it.id == to.key }

        if (fromIdx > -1 && toIdx > -1) {
            log("switchItemPositions($from, $to) before: ${_items.value}")
            _items.value = _items.value.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
            log("switchItemPositions($from, $to) after: ${_items.value}")
            updatePositions()
            _isDirty = true
        }
    }

    fun toggleShowChecked() {
        _note.value = _note.value.copy(showChecked = !_note.value.showChecked)
        _isDirty = true
    }

    fun uncheckAllItems() {
        if (_items.value.any { it.checked }) {
            _items.value = _items.value.map { if (it.checked) it.copy(checked = false) else it }
            _isDirty = true
        }
    }

    fun undoTrashItems() = viewModelScope.launch {
        _items.value = _items.value.toMutableList().apply {
            _trashedItems.value.forEach { (index, item) -> add(index, item) }
        }
        clearTrashItems()
    }

    private fun updateItem(id: UUID, text: String? = null, checked: Boolean? = null) {
        _items.value.find { it.id == id }?.let { item ->
            val index = _items.value.indexOf(item)

            log("updateItem($id, $text, $checked): updating item $item")
            _items.value = _items.value.toMutableList().apply {
                add(index, removeAt(index).copy(text = text ?: item.text, checked = checked ?: item.checked))
            }
            _isDirty = true
        }
    }

    fun updateItemChecked(id: UUID, checked: Boolean) = updateItem(id, checked = checked)

    fun updateItemText(id: UUID, text: String) = updateItem(id, text = text)

    private fun updatePositions() {
        log("updatePositions() before: ${_items.value}")
        _items.value = _items.value.mapIndexed { index, item -> item.copy(position = index) }
        log("updatePositions() after: ${_items.value}")
    }
}
