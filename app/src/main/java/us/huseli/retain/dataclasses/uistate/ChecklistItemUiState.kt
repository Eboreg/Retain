package us.huseli.retain.dataclasses.uistate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retain.annotation.RetainAnnotatedString
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.interfaces.IChecklistItem

@Stable
class ChecklistItemUiState(
    private var item: ChecklistItem,
    isFocused: Boolean = false,
    isNew: Boolean = false,
) : IChecklistItem {
    override val id = item.id
    override var isChecked by mutableStateOf(item.isChecked)
    override var isFocused by mutableStateOf(isFocused)
    override var isDragging by mutableStateOf(false)
    override var position by mutableIntStateOf(item.position)
    override var annotatedText: RetainAnnotatedString by mutableStateOf(item.annotatedText)

    var isNew by mutableStateOf(isNew)

    val serializedText: String
        get() = annotatedText.serialize()

    val isChanged: Boolean
        get() = item.annotatedText != annotatedText || item.isChecked != isChecked || item.position != position

    val isTextChanged: Boolean
        get() = item.annotatedText != annotatedText

    fun clone() = ChecklistItemUiState(item = toItem())

    override fun equals(other: Any?): Boolean = other is IChecklistItem &&
        other.id == id &&
        other.isChecked == isChecked &&
        other.position == position &&
        other.annotatedText == annotatedText

    override fun hashCode(): Int {
        var result = id.hashCode()

        result = 31 * result + isChecked.hashCode()
        result = 31 * result + position
        result = 31 * result + annotatedText.hashCode()
        return result
    }

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

    fun toItem() = item.copy(text = annotatedText.serialize(), isChecked = isChecked, position = position)
}

suspend fun Collection<ChecklistItemUiState>.save(dbSaveFunc: suspend (Collection<ChecklistItem>) -> Unit) {
    val states = filter { it.isNew || it.isChanged }

    if (states.isNotEmpty()) {
        dbSaveFunc(states.map { it.toItem() })
        withContext(Dispatchers.Main) { states.forEach { it.onSaved() } }
    }
}

fun Collection<ChecklistItemUiState>.clone() = map { it.clone() }
