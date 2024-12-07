package us.huseli.retain.dataclasses.uistate

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import us.huseli.retain.Enums
import us.huseli.retain.annotation.RetainAnnotatedString
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.ui.theme.NoteColorKey
import java.time.Instant
import java.util.UUID

@Stable
class NoteUiState(note: Note, status: Status = Status.REGULAR) {
    // PLACEHOLDER: Is a temp placeholder that will very soon be overwritten from DB, so render it read only
    // NEW: Is not pre-existing and has not been saved to DB yet
    enum class Status { NEW, PLACEHOLDER, REGULAR }

    private var note: Note by mutableStateOf(note)

    val id: UUID = note.id
    val type: Enums.NoteType = note.type
    var showChecked: Boolean by mutableStateOf(note.showChecked)
    var title: String by mutableStateOf(note.title)
    var colorKey: NoteColorKey by mutableStateOf(note.colorKey)
    var status: Status by mutableStateOf(status)
    var annotatedText: RetainAnnotatedString by mutableStateOf(note.annotatedText)

    var position: Int by mutableIntStateOf(note.position)
        private set

    val updated: Instant by derivedStateOf { this.note.updated }

    val serializedText by derivedStateOf { this.note.text }

    val isReadOnly: Boolean
        get() = status == Status.PLACEHOLDER

    val isChanged: Boolean
        get() = when (status) {
            Status.PLACEHOLDER -> false
            else -> note.title != title
                || note.annotatedText != annotatedText
                || note.position != position
                || note.showChecked != showChecked
                || note.colorKey != colorKey
        }

    val shouldSave: Boolean
        get() = when (status) {
            Status.NEW -> true
            else -> isChanged
        }

    val isTextChanged: Boolean
        get() = note.annotatedText != annotatedText

    val isTitleChanged: Boolean
        get() = note.title != title

    fun onNoteFetched(note: Note) {
        this.note = note
        position = note.position
        showChecked = note.showChecked
        annotatedText = note.annotatedText
        title = note.title
        colorKey = note.colorKey
    }

    fun onNoteUpdated(note: Note) {
        this.note = note
    }

    fun toNote() = note.copy(
        position = position,
        showChecked = showChecked,
        title = title,
        text = annotatedText.serialize(),
        color = colorKey.name,
    )
}
