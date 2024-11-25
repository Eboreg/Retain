package us.huseli.retain.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retain.annotation.RetainAnnotatedString
import us.huseli.retain.interfaces.IChecklistItem
import java.util.UUID

@Entity(
    foreignKeys = [ForeignKey(
        entity = Note::class,
        parentColumns = ["noteId"],
        childColumns = ["checklistItemNoteId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
    )],
    indices = [Index("checklistItemNoteId"), Index("checklistItemPosition")],
)
data class ChecklistItem(
    @ColumnInfo(name = "checklistItemId") @PrimaryKey override val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "checklistItemText", defaultValue = "") val text: String = "",
    @ColumnInfo(name = "checklistItemNoteId") val noteId: UUID,
    @ColumnInfo(name = "checklistItemChecked", defaultValue = "0") override val isChecked: Boolean = false,
    @ColumnInfo(name = "checklistItemPosition", defaultValue = "0") override val position: Int = 0,
) : IChecklistItem {
    @Ignore override val isFocused: Boolean = false
    @Ignore override val isDragging: Boolean = false
    @Ignore override val annotatedText: RetainAnnotatedString = RetainAnnotatedString.deserialize(text)

    override fun equals(other: Any?) = other is ChecklistItem &&
        other.id == id &&
        other.text == text &&
        other.noteId == noteId &&
        other.isChecked == isChecked &&
        other.position == position

    override fun hashCode() = id.hashCode()
}
