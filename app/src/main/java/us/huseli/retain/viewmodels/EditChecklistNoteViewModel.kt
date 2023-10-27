package us.huseli.retain.viewmodels

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditChecklistNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(savedStateHandle, repository, NoteType.CHECKLIST) {
    private val _focusedItemId = MutableStateFlow<UUID?>(null)
    private val _trashedItems = MutableStateFlow<List<ChecklistItemFlow>>(emptyList())
    private val _checkedItems = MutableStateFlow<List<ChecklistItemFlow>>(emptyList())
    private val _uncheckedItems = MutableStateFlow<List<ChecklistItemFlow>>(emptyList())

    val checkedItems = _checkedItems.asStateFlow()
    val focusedItemId = _focusedItemId.asStateFlow()
    val trashedItems = _trashedItems.asStateFlow()
    val uncheckedItems = _uncheckedItems.asStateFlow()

    init {
        viewModelScope.launch {
            _checklistItems.collect { items ->
                _checkedItems.value = items.filter { it.checked.value }
                _uncheckedItems.value = items.filter { !it.checked.value }
            }
        }
    }

    private fun addDirtyItem(item: ChecklistItemFlow) {
        _dirtyChecklistItems.removeIf { it.id == item.id }
        if (_originalChecklistItems.none { item.equals(it) }) _dirtyChecklistItems.add(item)
    }

    fun clearTrashedItems() {
        _trashedItems.value = emptyList()
    }

    fun deleteCheckedItems() {
        clearTrashedItems()
        _trashedItems.value = _checklistItems.value.filter { it.checked.value }
        val trashedItemIds = _trashedItems.value.map { it.id }
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            removeAll { trashedItemIds.contains(it.id) }
        }
        _deletedChecklistItemIds.addAll(trashedItemIds)
        _dirtyChecklistItems.removeAll { trashedItemIds.contains(it.id) }
    }

    fun deleteItem(item: ChecklistItemFlow, permanent: Boolean = false) {
        if (!permanent) _trashedItems.value = listOf(item)
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            removeIf { it.id == item.id }
        }
        _deletedChecklistItemIds.add(item.id)
        _dirtyChecklistItems.removeIf { it.id == item.id }
    }

    private fun insertItem(item: ChecklistItemFlow) {
        log("insertItem: inserting $item with textFieldValue=${item.textFieldValue.value}")
        _checklistItems.value = _checklistItems.value.toMutableList().apply { add(item.position.value, item) }
        _focusedItemId.value = item.id
        addDirtyItem(item)
        updatePositions()
    }

    fun insertItem(text: String, checked: Boolean, index: Int) =
        insertItem(ChecklistItemFlow(ChecklistItem(text = text, checked = checked, noteId = _noteId, position = index)))

    fun onItemFocus(item: ChecklistItemFlow) {
        _focusedItemId.value = item.id
    }

    fun onNextItem(item: ChecklistItemFlow) {
        val index = _checklistItems.value.indexOf(item)  // todo: go after id
        val textFieldValue = item.textFieldValue.value
        val head = textFieldValue.text.substring(0, textFieldValue.selection.start)
        val tail = textFieldValue.text.substring(textFieldValue.selection.start)

        log("onNextItem: item=$item, head=$head, tail=$tail, index=$index")
        if (tail.isNotEmpty()) updateItemTextFieldValue(
            item = item,
            text = head,
            selection = item.textFieldValue.value.selection,
            composition = null,
        )
        insertItem(tail, item.checked.value, index + 1)
    }

    fun onTextFieldValueChange(item: ChecklistItemFlow, textFieldValue: TextFieldValue) {
        if (item.id == _focusedItemId.value && textFieldValue != item.textFieldValue.value) {
            updateItemTextFieldValue(
                item = item,
                text = textFieldValue.text,
                selection = textFieldValue.selection,
                composition = textFieldValue.composition,
            )
        }
    }

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
        }
    }

    fun toggleShowChecked() {
        _note.value = _note.value.copy(showChecked = !_note.value.showChecked)
    }

    fun uncheckAllItems() {
        _checklistItems.value.filter { it.checked.value }.forEach {
            it.checked.value = false
            addDirtyItem(it)
        }
        updatePositions()
        updateItemListFlows()
    }

    fun undoDeleteItems() {
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            _trashedItems.value.forEach {
                add(it.position.value, it)
                addDirtyItem(it)
            }
        }
        _deletedChecklistItemIds.removeAll(_trashedItems.value.map { it.id }.toSet())
        clearTrashedItems()
    }

    fun updateItemChecked(item: ChecklistItemFlow, checked: Boolean) {
        val index = _checklistItems.value.indexOfFirst { it.id == item.id }

        if (index > -1) {
            item.checked.value = checked
            addDirtyItem(item)
            updatePositions()
            updateItemListFlows()
        }
    }

    private fun updateItemTextFieldValue(
        item: ChecklistItemFlow,
        text: String,
        selection: TextRange,
        composition: TextRange?,
    ) {
        item.textFieldValue.value = item.textFieldValue.value.copy(
            text = text,
            selection = selection,
            composition = composition,
        )
        _dirtyChecklistItems.removeIf { it.id == item.id }
        if (_originalChecklistItems.none { it.id == item.id && it.text == item.textFieldValue.value.text }) {
            _dirtyChecklistItems.add(item)
        }
    }

    private fun updatePositions() {
        _checklistItems.value.forEachIndexed { index, item ->
            if (item.position.value != index) {
                item.position.value = index
                addDirtyItem(item)
            }
        }
    }

    private fun updateItemListFlows() {
        _checkedItems.value = _checklistItems.value.filter { it.checked.value }
        _uncheckedItems.value = _checklistItems.value.filter { !it.checked.value }
    }
}
