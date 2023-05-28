package us.huseli.retain.viewmodels

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

fun adjustSelection(range: TextRange): TextRange = if (range.start < 1) TextRange(1, max(range.end, 1)) else range
fun stripNullChar(str: String): String = str.filter { it != Char.MIN_VALUE }
fun addNullChar(str: String): String = Char.MIN_VALUE + stripNullChar(str)

@HiltViewModel
class EditChecklistNoteViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(context, savedStateHandle, repository, NoteType.CHECKLIST) {
    private val _focusedItemId = MutableStateFlow<UUID?>(null)
    private val _trashedItems = MutableStateFlow<List<ChecklistItem>>(emptyList())

    val checkedItems = _checklistItems.map { items -> items.filter { item -> item.checked } }
    val focusedItemId = _focusedItemId.asStateFlow()
    val trashedItems = _trashedItems.asStateFlow()
    val uncheckedItems = _checklistItems.map { items -> items.filter { item -> !item.checked } }

    private fun addDirtyItem(item: ChecklistItem) {
        _dirtyChecklistItems.removeIf { it.id == item.id }
        if (_originalChecklistItems.none { it == item }) _dirtyChecklistItems.add(item)
    }

    fun clearTrashedItems() {
        _trashedItems.value = emptyList()
    }

    fun deleteCheckedItems() {
        clearTrashedItems()
        _checklistItems.value = _checklistItems.value.mapNotNull { item ->
            if (item.checked) {
                _trashedItems.value = _trashedItems.value.toMutableList().apply { add(item) }
                addDirtyItem(item.copy(isDeleted = true))
                null
            } else item
        }
    }

    fun deleteItem(item: ChecklistItem, permanent: Boolean = false) {
        val index = _checklistItems.value.indexOfFirst { it.id == item.id }
        addDirtyItem(item.copy(isDeleted = true))

        if (index > -1) {
            _checklistItems.value = _checklistItems.value.toMutableList().apply {
                removeAt(index)
                if (!permanent) {
                    _trashedItems.value = listOf(item)
                }
            }
        }
    }

    private fun insertItem(item: ChecklistItem) {
        val itemExtended = ChecklistItemExtended(item)

        log("insertItem: inserting $itemExtended with textFieldValue=${itemExtended.textFieldValue.value}")
        _checklistItems.value = _checklistItems.value.toMutableList().apply { add(item.position, itemExtended) }
        _focusedItemId.value = item.id
        addDirtyItem(item)
        updatePositions()
    }

    fun insertItem(text: String, checked: Boolean, index: Int) =
        insertItem(ChecklistItem(text = text, checked = checked, noteId = _noteId, position = index))

    private fun mergeItemWithPrevious(item: ChecklistItemExtended) {
        val items = _checklistItems.value.filter { it.checked == item.checked }.toMutableList()
        val index = items.indexOf(item)
        val previousItem = items[index - 1]

        log("mergeItemWithPrevious: item=$item, item.textFieldValue=${item.textFieldValue.value} previousItem=$previousItem, previousItem.textFieldValue=${previousItem.textFieldValue.value}")

        if (item.text.isNotEmpty()) {
            updateItemText(previousItem, previousItem.text + stripNullChar(item.text), previousItem.text.length + 1)
        }
        _focusedItemId.value = previousItem.id
        deleteItem(item, true)
    }

    fun onItemFocus(item: ChecklistItemExtended) {
        _focusedItemId.value = item.id
    }

    fun onNextItem(item: ChecklistItemExtended) {
        val index = _checklistItems.value.indexOf(item)
        val textFieldValue = item.textFieldValue.value
        val head = textFieldValue.text.substring(0, textFieldValue.selection.start)
        val tail = textFieldValue.text.substring(textFieldValue.selection.start)

        log("onNextItem: item=$item, head=$head, tail=$tail")
        insertItem(tail, item.checked, index + 1)
        if (tail.isNotEmpty()) updateItemText(item, head)
    }

    fun onTextFieldValueChange(item: ChecklistItemExtended, textFieldValue: TextFieldValue) {
        /**
         * If the new TextFieldValue does not start with the null character,
         * that must mean the user has just erased it by inputting a backspace
         * at the beginning of the field. In that case, join this row with the
         * one above. If this is the first row: just re-insert the null
         * character move the selection start to after it.
         */
        val index = _checklistItems.value.indexOfFirst { it.id == item.id }

        if (
            stripNullChar(textFieldValue.text) != item.textFieldValue.value.text &&
            (textFieldValue.text.isEmpty() || textFieldValue.text[0] != Char.MIN_VALUE)
        ) {
            if (index > 0) mergeItemWithPrevious(item)
            else if (textFieldValue.text.isEmpty()) deleteItem(item, true)
        } else {
            updateItemText(item, stripNullChar(textFieldValue.text))
            item.textFieldValue.value = textFieldValue.copy(
                text = addNullChar(textFieldValue.text),
                selection = adjustSelection(textFieldValue.selection)
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
        if (_checklistItems.value.any { it.checked }) {
            _checklistItems.value = _checklistItems.value.map { item ->
                if (item.checked) {
                    addDirtyItem(item.copy(checked = false))
                    item.copy(checked = false)
                } else item
            }
            updatePositions()
        }
    }

    fun undoTrashItems() = viewModelScope.launch {
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            _trashedItems.value.forEach { insertItem(it) }
        }
        clearTrashedItems()
    }

    private fun updateItem(old: ChecklistItemExtended, new: ChecklistItemExtended) {
        val index = _checklistItems.value.indexOfFirst { it.id == old.id }

        if (index > -1) {
            log("updateItem: old=$old, new=$new")
            addDirtyItem(new)
            _checklistItems.value = _checklistItems.value.toMutableList().apply { set(index, new) }
        }
    }

    fun updateItemChecked(item: ChecklistItemExtended, checked: Boolean) {
        updateItem(item, item.copy(checked = checked))
        updatePositions()
    }

    private fun updateItemText(item: ChecklistItemExtended, text: String, selectionStart: Int? = null) {
        updateItem(
            old = item,
            new = item.copy(text = stripNullChar(text)).also {
                if (selectionStart != null) it.textFieldValue.value =
                    it.textFieldValue.value.copy(selection = TextRange(selectionStart))
            }
        )
    }

    private fun updatePositions() {
        _checklistItems.value = _checklistItems.value.mapIndexed { index, item ->
            if (item.position != index) {
                item.copy(position = index).also { addDirtyItem(it) }
            } else item
        }
    }
}
