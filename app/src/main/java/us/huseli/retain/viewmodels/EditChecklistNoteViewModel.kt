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
    repository: NoteRepository,
    override val logger: Logger,
) : BaseEditNoteViewModel(savedStateHandle, repository, NoteType.CHECKLIST), LogInterface {
    private val _checklistItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val checklistItems = _checklistItems.asStateFlow()

    init {
        viewModelScope.launch {
            _checklistItems.value = repository.listChecklistItems(noteId)
        }
    }

    fun deleteItem(item: ChecklistItem) = viewModelScope.launch {
        _checklistItems.value = _checklistItems.value.toMutableList().apply { remove(item) }
        _isDirty = true
    }

    private fun updatePositions() {
        log("updatePositions() before: ${_checklistItems.value}")
        _checklistItems.value = _checklistItems.value.mapIndexed { index, item -> item.copy(position = index) }
        log("updatePositions() after: ${_checklistItems.value}")
    }

    fun insertItem(text: String, checked: Boolean, index: Int) = viewModelScope.launch {
        val item = ChecklistItem(text = text, checked = checked, noteId = noteId, position = index)

        log("insertItem($text, $checked, $index): inserting $item")
        _checklistItems.value = _checklistItems.value.toMutableList().apply { add(index, item) }
        updatePositions()
        _isDirty = true
    }

    fun toggleShowChecked() {
        updateNote(showChecked = !_note.value.showChecked)
        _isDirty = true
    }

    private fun updateItem(id: UUID, text: String? = null, checked: Boolean? = null) {
        _checklistItems.value.find { it.id == id }?.let { item ->
            val index = _checklistItems.value.indexOf(item)

            log("updateItem($id, $text, $checked): updating item $item")
            _checklistItems.value = _checklistItems.value.toMutableList().apply {
                add(index, removeAt(index).copy(text = text, checked = checked))
            }
            _isDirty = true
        }
    }

    fun updateItemChecked(id: UUID, checked: Boolean) = updateItem(id, checked = checked)

    fun updateItemText(id: UUID, text: String) = updateItem(id, text = text)

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
}
