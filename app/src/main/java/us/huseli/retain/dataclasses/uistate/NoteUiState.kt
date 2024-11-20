package us.huseli.retain.dataclasses.uistate

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.ui.theme.NoteColorKey
import us.huseli.retain.ui.theme.darken
import us.huseli.retain.ui.theme.getNoteColor
import java.time.Instant
import java.util.UUID

@Stable
interface INoteUiState {
    val id: UUID
    val title: String
    val text: String
    val position: Int
    val type: NoteType
    val showChecked: Boolean
    val colorKey: NoteColorKey
    val selection: TextRange
    val textFieldValue: TextFieldValue
    val updated: Instant

    @Composable
    fun getSystemBarColor(): Color? {
        val dark = isSystemInDarkTheme()
        return remember(colorKey, dark) { getNoteColor(colorKey, dark)?.darken() }
    }
}

@Stable
class MutableNoteUiState(private var note: Note, status: Status = Status.REGULAR) : INoteUiState {
    // PLACEHOLDER: Is a temp placeholder that will very soon be overwritten from DB, so render it read only
    // NEW: Is not pre-existing and has not been saved to DB yet
    enum class Status { NEW, PLACEHOLDER, REGULAR }

    override val id = note.id
    override val type = note.type
    override var position by mutableIntStateOf(note.position)
    override var showChecked by mutableStateOf(note.showChecked)
    override var text by mutableStateOf(note.text)
    override var title by mutableStateOf(note.title)
    override var colorKey by mutableStateOf(note.colorKey)
    override var selection by mutableStateOf(TextRange(note.text.length))
    override var updated by mutableStateOf(note.updated)
        private set
    var status by mutableStateOf(status)

    val isReadOnly: Boolean
        get() = status == Status.PLACEHOLDER

    override val textFieldValue: TextFieldValue
        get() = TextFieldValue(text = text, selection = selection)

    val shouldSave: Boolean
        get() {
            return when (status) {
                Status.NEW -> true
                Status.PLACEHOLDER -> false
                Status.REGULAR -> note.title != title
                    || note.text != text
                    || note.position != position
                    || note.showChecked != showChecked
                    || note.colorKey != colorKey
            }
        }

    val isTextChanged: Boolean
        get() = note.text != text

    val isTitleChanged: Boolean
        get() = note.title != title

    fun refreshFromNote(note: Note) {
        this.note = note
        position = note.position
        showChecked = note.showChecked
        text = note.text
        title = note.title
        colorKey = note.colorKey
        updated = note.updated
    }

    fun toNote() =
        note.copy(position = position, showChecked = showChecked, title = title, text = text, color = colorKey.name)
}
