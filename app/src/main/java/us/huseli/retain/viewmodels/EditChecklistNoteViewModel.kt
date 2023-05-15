package us.huseli.retain.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditChecklistNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: NoteRepository
) : EditNoteViewModel(savedStateHandle, repository) {
    private var _checklistItems = mutableMapOf<UUID, ChecklistItem>()
    private var _isDirtyMap = mutableMapOf<UUID, Boolean>()
    private val _checklistItemsFlow = MutableStateFlow<List<ChecklistItem>>(emptyList())

    val checklistItems = _checklistItemsFlow.asStateFlow()
    val updatedItems: List<ChecklistItem>
        get() = _isDirtyMap.filterValues { it }.keys.mapNotNull { _checklistItems[it] }

    init {
        loadItems()
    }

    fun deleteItem(item: ChecklistItem) = viewModelScope.launch {
        repository.deleteChecklistItem(item)
        loadItems()
        _isDirty = true
    }

    fun insertItem(text: String, checked: Boolean, position: Int) = viewModelScope.launch {
        if (!_isStored) saveNote()
        repository.insertChecklistItem(noteId, text, checked, position)
        loadItems()
        _isDirty = true
    }

    private fun loadItems() = viewModelScope.launch {
        repository.listChecklistItems(noteId).also { items ->
            items.forEach { item ->
                if (_checklistItems[item.id] == null) {
                    _checklistItems[item.id] = item
                    _isDirtyMap[item.id] = false
                } else {
                    @Suppress("ReplaceNotNullAssertionWithElvisReturn")
                    _checklistItems[item.id] = _checklistItems[item.id]!!.copy(position = item.position)
                }
            }
            _checklistItems.keys.toList().forEach { id ->
                if (!items.map { it.id }.contains(id)) _checklistItems.remove(id)
            }
            _checklistItemsFlow.value = _checklistItems.values.sortedBy { it.position }
        }
    }

    fun toggleShowChecked() {
        _showChecked.value = !_showChecked.value
        _isDirty = true
    }

    fun updateItem(id: UUID, text: String? = null, checked: Boolean? = null, position: Int? = null) {
        _checklistItems[id]?.let { item ->
            if (
                (checked != null && item.checked != checked) ||
                (text != null && item.text != text) ||
                (position != null && item.position != position)
            ) {
                _checklistItems[id] = item.copy(checked = checked, text = text, position = position)
                _isDirtyMap[id] = true
                _checklistItemsFlow.value = _checklistItems.values.sortedBy { it.position }
                _isDirty = true
            }
        }
    }

    fun updateItemChecked(id: UUID, checked: Boolean) = updateItem(id, checked = checked)

    fun updateItemText(id: UUID, text: String) = updateItem(id, text = text)

    fun switchItemPositions(fromId: UUID, toId: UUID) {
        val from = _checklistItems[fromId]
        val to = _checklistItems[toId]

        if (from != null && to != null) {
            _checklistItems[fromId] = from.copy(position = to.position)
            _checklistItems[toId] = to.copy(position = from.position)
            _isDirtyMap[fromId] = true
            _isDirtyMap[toId] = true
            _checklistItemsFlow.value = _checklistItems.values.sortedBy { it.position }
            _isDirty = true
        }
    }
}
