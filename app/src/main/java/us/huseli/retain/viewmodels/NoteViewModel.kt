package us.huseli.retain.viewmodels

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.Constants.NAV_ARG_NEW_NOTE_TYPE
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.repositories.NoteRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle,
    override val logger: Logger,
) : ViewModel(), LogInterface {
    private val _noteId: UUID =
        savedStateHandle.get<String>(NAV_ARG_NOTE_ID)?.let { UUID.fromString(it) } ?: UUID.randomUUID()
    private val _newNoteType: NoteType? =
        savedStateHandle.get<String>(NAV_ARG_NEW_NOTE_TYPE)?.let { NoteType.valueOf(it) }
    private val _note = MutableStateFlow<Note?>(null)
    private val _images = MutableStateFlow<List<Image>>(emptyList())
    private val _checklistItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
    private val _selectedImages = MutableStateFlow<Set<String>>(emptySet())
    private val _focusedChecklistItemId = MutableStateFlow<UUID?>(null)
    private val _isUnsaved = MutableStateFlow(true)
    private val _checklistItemUndoState = MutableStateFlow<List<ChecklistItem>?>(null)
    private val _imageUndoState = MutableStateFlow<List<Image>?>(null)

    val images = _images.asStateFlow()
    val note = _note.asStateFlow()
    val checkedItems = _checklistItems.map { items -> items.filter { it.checked } }
    val uncheckedItems = _checklistItems.map { items -> items.filter { !it.checked } }
    val selectedImages = _selectedImages.asStateFlow()
    val focusedChecklistItemId = _focusedChecklistItemId.asStateFlow()
    val isUnsaved = _isUnsaved.asStateFlow()

    init {
        viewModelScope.launch {
            repository.flowNotePojo(_noteId).filterNotNull().distinctUntilChanged().collect { pojo ->
                _note.value = pojo.note
                _images.value = pojo.images
                _checklistItems.value = pojo.checklistItems
                _isUnsaved.value = false
            }
        }

        _newNoteType?.also { noteType ->
            _note.value = Note(id = _noteId, type = noteType)
        }
    }

    fun deleteCheckedItems(onFinish: (Int) -> Unit) =
        deleteChecklistItems(_checklistItems.value.filter { it.checked }.map { it.id }, onFinish)

    fun deleteChecklistItem(item: ChecklistItem, onFinish: (Int) -> Unit) =
        deleteChecklistItems(listOf(item.id), onFinish)

    fun deleteSelectedImages(onFinish: (Int) -> Unit) {
        val trashedImageIds = _selectedImages.value

        _imageUndoState.value = _images.value
        _images.value = _images.value.toMutableList().apply {
            removeAll { trashedImageIds.contains(it.filename) }
        }
        deselectAllImages()
        save(NotePojo.Component.IMAGES)
        onFinish(trashedImageIds.size)
    }

    fun deselectAllImages() {
        _selectedImages.value = emptySet()
    }

    fun insertChecklistItem(text: String, checked: Boolean, position: Int): ChecklistItem {
        val item = ChecklistItem(text = text, checked = checked, position = position, noteId = _noteId)

        _checklistItems.value = _checklistItems.value
            .map {
                if (it.position >= position && it.checked == checked) it.copy(position = it.position + 1)
                else it
            }.plus(item).sorted()

        setChecklistItemFocus(item)
        save(NotePojo.Component.CHECKLIST_ITEMS)

        return item
    }

    fun insertImage(uri: Uri) = viewModelScope.launch {
        repository.uriToImage(uri, _noteId)?.let { image ->
            _images.value = _images.value.toMutableList().apply { add(image) }
            updateImagePositions()
            save(NotePojo.Component.IMAGES)
        }
    }

    fun save() = save(listOf(NotePojo.Component.CHECKLIST_ITEMS, NotePojo.Component.NOTE, NotePojo.Component.IMAGES))

    fun selectAllImages() {
        _selectedImages.value = _images.value.map { it.filename }.toSet()
    }

    fun setChecklistItemFocus(item: ChecklistItem?) {
        _focusedChecklistItemId.value = item?.id
    }

    fun setChecklistItemText(item: ChecklistItem, value: String) {
        _checklistItems.value = _checklistItems.value.map {
            if (it.id == item.id) it.copy(text = value) else it
        }
        _isUnsaved.value = true
    }

    fun setColor(value: String) {
        if (value != _note.value?.color) {
            _note.value = _note.value?.copy(color = value)
            save(NotePojo.Component.NOTE)
        }
    }

    fun setText(value: String) {
        if (value != _note.value?.text) _note.value = _note.value?.copy(text = value)
        _isUnsaved.value = true
    }

    fun setTitle(value: String) {
        if (value != _note.value?.title) _note.value = _note.value?.copy(title = value)
        _isUnsaved.value = true
    }

    fun splitChecklistItem(item: ChecklistItem, textFieldValue: TextFieldValue) {
        /**
         * Splits item's text at cursor position, moves the last part to a new
         * item, moves focus to this item.
         */
        val head = textFieldValue.text.substring(0, textFieldValue.selection.start)
        val tail = textFieldValue.text.substring(textFieldValue.selection.start)

        setChecklistItemText(item, head)
        insertChecklistItem(text = tail, checked = item.checked, position = item.position + 1)
    }

    fun switchItemPositions(from: ItemPosition, to: ItemPosition) {
        /**
         * We cannot use ItemPosition.index because the lazy column contains
         * a whole bunch of other junk than checklist items.
         */
        val fromItem = _checklistItems.value.firstOrNull { it.id == from.key }
        val toItem = _checklistItems.value.firstOrNull { it.id == to.key }

        if (fromItem != null && toItem != null) {
            _checklistItems.value = _checklistItems.value.map { item ->
                when (item.id) {
                    fromItem.id -> item.copy(position = toItem.position)
                    toItem.id -> item.copy(position = fromItem.position)
                    else -> item
                }
            }.sorted()
            _isUnsaved.value = true
        }
    }

    fun toggleImageSelected(filename: String) {
        _selectedImages.value = _selectedImages.value.toMutableSet().apply {
            if (contains(filename)) remove(filename)
            else add(filename)
        }
    }

    fun toggleShowCheckedItems() {
        _note.value = _note.value?.let { it.copy(showChecked = !it.showChecked) }
        save(NotePojo.Component.NOTE)
    }

    fun uncheckAllItems() {
        var position = _checklistItems.value.filter { !it.checked }.maxOfOrNull { it.position } ?: -1

        _checklistItems.value = _checklistItems.value.map { item ->
            if (item.checked) item.copy(checked = false, position = ++position)
            else item
        }.sorted()
        save(NotePojo.Component.CHECKLIST_ITEMS)
    }

    fun undeleteChecklistItems() {
        _checklistItemUndoState.value?.also {
            _checklistItems.value = it
            save(NotePojo.Component.CHECKLIST_ITEMS)
        }
        _checklistItemUndoState.value = null
    }

    fun undeleteImages() {
        _imageUndoState.value?.also {
            _images.value = it
            save(NotePojo.Component.IMAGES)
        }
        _imageUndoState.value = null
    }

    fun updateChecklistItemChecked(item: ChecklistItem, checked: Boolean) {
        val position = _checklistItems.value.filter { it.checked == checked }.maxOfOrNull { it.position } ?: -1

        _checklistItems.value = _checklistItems.value.map {
            if (it.id == item.id) it.copy(checked = checked, position = position + 1)
            else it
        }.sorted()
        save(NotePojo.Component.CHECKLIST_ITEMS)
    }

    /** PRIVATE METHODS ******************************************************/

    private fun deleteChecklistItems(itemIds: List<UUID>, onFinish: (Int) -> Unit) {
        _checklistItemUndoState.value = _checklistItems.value
        _checklistItems.value = _checklistItems.value.toMutableList().apply {
            removeAll { itemIds.contains(it.id) }
        }
        save(NotePojo.Component.CHECKLIST_ITEMS)
        onFinish(itemIds.size)
    }

    private fun save(component: NotePojo.Component) = save(listOf(component))

    private fun save(components: List<NotePojo.Component>) = _note.value?.also { note ->
        repository.saveNotePojo(
            NotePojo(note = note, checklistItems = _checklistItems.value, images = _images.value),
            components,
        )
        _isUnsaved.value = false
    }

    private fun updateImagePositions() {
        _images.value = _images.value.mapIndexed { index, image ->
            if (image.position != index) image.copy(position = index) else image
        }
        _isUnsaved.value = true
    }
}
