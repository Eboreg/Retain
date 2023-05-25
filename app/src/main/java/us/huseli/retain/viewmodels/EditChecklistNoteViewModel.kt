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
import us.huseli.retain.data.entities.NoteCombo
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

fun adjustSelection(range: TextRange): TextRange = if (range.start < 1) TextRange(1, max(range.end, 1)) else range
fun stripNullChar(str: String): String = str.filter { it != Char.MIN_VALUE }
fun addNullChar(str: String): String = Char.MIN_VALUE + stripNullChar(str)

class ChecklistItemExtended(
    item: ChecklistItem,
    val textFieldValue: MutableStateFlow<TextFieldValue> = MutableStateFlow(
        TextFieldValue(
            addNullChar(item.text),
            TextRange(1)
        )
    )
) : ChecklistItem(
    id = item.id,
    text = item.text,
    noteId = item.noteId,
    checked = item.checked,
    position = item.position,
    isDeleted = item.isDeleted
) {
    override fun copy(text: String, checked: Boolean, position: Int, isDeleted: Boolean): ChecklistItemExtended {
        return ChecklistItemExtended(super.copy(text, checked, position, isDeleted), textFieldValue).also {
            it.textFieldValue.value = it.textFieldValue.value.copy(text = addNullChar(text))
        }
    }
}

@HiltViewModel
class EditChecklistNoteViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(context, savedStateHandle, repository, NoteType.CHECKLIST) {
    private val _checklistItems = MutableStateFlow<List<ChecklistItemExtended>>(emptyList())
    private val _trashedItems = MutableStateFlow<Map<Int, ChecklistItem>>(emptyMap())
    private val _focusedItemId = MutableStateFlow<UUID?>(null)

    val checkedItems = _checklistItems.map { items -> items.filter { item -> item.checked } }
    val uncheckedItems = _checklistItems.map { items -> items.filter { item -> !item.checked } }
    val trashedItems = _trashedItems.asStateFlow()
    val focusedItemId = _focusedItemId.asStateFlow()

    override val combo: NoteCombo
        get() = NoteCombo(_note.value, _checklistItems.value, _bitmapImages.value.map { it.image })

    init {
        _bitmapImages.value = repository.bitmapImages.value.filter { it.image.noteId == _noteId }

        viewModelScope.launch {
            @Suppress("Destructure")
            repository.getCombo(_noteId)?.let { combo ->
                _note.value = combo.note
                _checklistItems.value = combo.checklistItems.map { ChecklistItemExtended(it) }
                _isNew = false
                _isDirty = false
            }
        }
    }

    fun clearTrashItems() {
        _trashedItems.value = emptyMap()
    }

    fun deleteCheckedItems() {
        _trashedItems.value = _checklistItems.value.mapIndexedNotNull { index, item ->
            if (item.checked) index to item else null
        }.associate { it }
        if (_trashedItems.value.isNotEmpty()) {
            _checklistItems.value = _checklistItems.value.filter { !it.checked }
            // _textFieldValues.minusAssign(_trashedItems.value.values.map { it.id }.toSet())
            _isDirty = true
        }
    }

    fun deleteItem(item: ChecklistItem, permanent: Boolean = false) {
        val index = _checklistItems.value.indexOfFirst { it.id == item.id }

        if (index > -1) {
            _checklistItems.value = _checklistItems.value.toMutableList().apply {
                removeAt(index)
                if (!permanent) {
                    _trashedItems.value = mapOf(index to item)
                    // _textFieldValues.minusAssign(item.id)
                }
                _isDirty = true
            }
        }
    }

    fun insertItem(text: String, checked: Boolean, index: Int) {
        val item = ChecklistItemExtended(
            ChecklistItem(text = text, checked = checked, noteId = _noteId, position = index)
        )

        log("insertItem($text, $checked, $index): inserting $item with textFieldValue=${item.textFieldValue.value}")
        _focusedItemId.value = item.id
        _checklistItems.value = _checklistItems.value.toMutableList().apply { add(index, item) }
        updatePositions()
        _isDirty = true
    }

    override fun isEmpty() = super.isEmpty() && _checklistItems.value.isEmpty()

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

        log("onTextFieldValueChange before: item=$item, textFieldValue=$textFieldValue, item.textFieldValue=${item.textFieldValue.value}")

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

        log("onTextFieldValueChange after: item=$item, item.textFieldValue=${item.textFieldValue.value}")
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
            _trashedItems.value.forEach { (index, item) -> add(index, ChecklistItemExtended(item)) }
        }
        clearTrashItems()
    }

    private fun updateItem(old: ChecklistItemExtended, new: ChecklistItemExtended) {
        val index = _checklistItems.value.indexOfFirst { it.id == old.id }

        if (index > -1) {
            log("updateItem: old=$old, new=$new")
            _checklistItems.value = _checklistItems.value.toMutableList().apply { set(index, new) }
            _isDirty = true
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
        _checklistItems.value = _checklistItems.value.mapIndexed { index, item -> item.copy(position = index) }
    }
}
