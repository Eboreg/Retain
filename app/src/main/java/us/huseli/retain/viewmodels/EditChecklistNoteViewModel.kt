package us.huseli.retain.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(context, savedStateHandle, repository, NoteType.CHECKLIST), LogInterface {
    private val _trashedItems = MutableStateFlow<List<Pair<Int, ChecklistItem>>>(emptyList())

    val items = _checklistItems.asStateFlow()
    val trashedItems = _trashedItems.asStateFlow()

    fun clearTrashItems() {
        _trashedItems.value = emptyList()
    }

    fun deleteCheckedItems() {
        _trashedItems.value = _checklistItems.value.mapIndexedNotNull { index, item ->
            if (item.checked) Pair(index, item) else null
        }
        if (_trashedItems.value.isNotEmpty()) {
            _checklistItems.value = _checklistItems.value.filter { !it.checked }
            _isDirty = true
        }
    }

    fun deleteItem(item: ChecklistItem) {
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            val index = indexOf(item)

            if (index > -1) {
                removeAt(index)
                _trashedItems.value = listOf(Pair(index, item))
                _isDirty = true
            }
        }
    }

    fun insertItem(text: String, checked: Boolean, index: Int) {
        val item = ChecklistItem(text = text, checked = checked, noteId = _noteId, position = index)

        log("insertItem($text, $checked, $index): inserting $item")
        _checklistItems.value = _checklistItems.value.toMutableList().apply { add(index, item) }
        updatePositions()
        _isDirty = true
    }

    override fun isEmpty() = super.isEmpty() && _checklistItems.value.isEmpty()

    fun switchItemPositions(from: ItemPosition, to: ItemPosition) {
        /**
         * We cannot use ItemPosition.index because the lazy column contains
         * a whole bunch of other junk than checklist items.
         */
        val fromIdx = _checklistItems.value.indexOfFirst { it.id == from.key }
        val toIdx = _checklistItems.value.indexOfFirst { it.id == to.key }

        if (fromIdx > -1 && toIdx > -1) {
            log("switchItemPositions($from, $to) before: ${_checklistItems.value}")
            _checklistItems.value = _checklistItems.value.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
            log("switchItemPositions($from, $to) after: ${_checklistItems.value}")
            updatePositions()
            _isDirty = true
        }
    }

    fun toggleShowChecked() {
        _note.value = _note.value.copy(showChecked = !_note.value.showChecked)
        _isDirty = true
    }

    fun uncheckAllItems() {
        if (_checklistItems.value.any { it.checked }) {
            _checklistItems.value = _checklistItems.value.map { if (it.checked) it.copy(checked = false) else it }
            updatePositions()
            _isDirty = true
        }
    }

    fun undoTrashItems() = viewModelScope.launch {
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            _trashedItems.value.forEach { (index, item) -> add(index, item) }
        }
        clearTrashItems()
    }

    private fun updateItem(id: UUID, text: String? = null, checked: Boolean? = null) {
        _checklistItems.value.find { it.id == id }?.let { item ->
            val index = _checklistItems.value.indexOf(item)

            log("updateItem($id, $text, $checked): updating item $item")
            _checklistItems.value = _checklistItems.value.toMutableList().apply {
                add(index, removeAt(index).copy(text = text ?: item.text, checked = checked ?: item.checked))
            }
            _isDirty = true
        }
    }

    fun updateItemChecked(id: UUID, checked: Boolean) {
        updateItem(id, checked = checked)
        updatePositions()
    }

    fun updateItemText(id: UUID, text: String) = updateItem(id, text = text)

    private fun updatePositions() {
        _checklistItems.value = _checklistItems.value.mapIndexed { index, item -> item.copy(position = index) }
    }
}
