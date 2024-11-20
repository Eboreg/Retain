package us.huseli.retain.dataclasses.uistate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retain.dataclasses.entities.ChecklistItem
import java.util.UUID

@Stable
interface IChecklistItemUiState : Comparable<IChecklistItemUiState> {
    val text: String
    val selection: TextRange
    val isChecked: Boolean
    val isFocused: Boolean
    val isDragging: Boolean
    val showAutocomplete: Boolean
    val alpha: Float
    val id: UUID
    val position: Int

    val compareValue: Int
        get() = position + if (isChecked) 100_000 else 0

    override fun compareTo(other: IChecklistItemUiState): Int = compareValue - other.compareValue
}

@Stable
class MutableChecklistItemUiState(
    private var item: ChecklistItem,
    isFocused: Boolean = false,
    isNew: Boolean = false,
) : IChecklistItemUiState {
    override val id = item.id
    override var text by mutableStateOf(item.text)
    override var selection by mutableStateOf(TextRange(item.text.length))
    override var isChecked by mutableStateOf(item.checked)
    override var isFocused by mutableStateOf(isFocused)
    override var isDragging by mutableStateOf(false)
    override var position by mutableIntStateOf(item.position)
    var isNew by mutableStateOf(isNew)

    override val showAutocomplete: Boolean
        get() = isFocused && !isDragging && !isChecked

    override val alpha: Float
        get() = if (isChecked) 0.5f else 1f

    val isChanged: Boolean
        get() = item.text != text || item.checked != isChecked || item.position != position

    val isTextChanged: Boolean
        get() = item.text != text

    fun clone() = MutableChecklistItemUiState(item = toItem())

    fun onSaved() {
        item = toItem()
        isNew = false
    }

    suspend fun save(dbSaveFunc: suspend (ChecklistItem) -> Unit) {
        if (isNew || isChanged) {
            withContext(Dispatchers.Main) { onSaved() }
            dbSaveFunc(item)
        }
    }

    fun toItem() = item.copy(text = text, checked = isChecked, position = position)
}

suspend fun Collection<MutableChecklistItemUiState>.save(dbSaveFunc: suspend (Collection<ChecklistItem>) -> Unit) {
    val states = filter { it.isNew || it.isChanged }

    if (states.isNotEmpty()) {
        dbSaveFunc(states.map { it.toItem() })
        withContext(Dispatchers.Main) { states.forEach { it.onSaved() } }
    }
}

fun Collection<MutableChecklistItemUiState>.clone() = map { it.clone() }
