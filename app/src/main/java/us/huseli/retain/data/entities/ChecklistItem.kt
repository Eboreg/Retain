package us.huseli.retain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
open class ChecklistItem(
    @ColumnInfo(name = "checklistItemId") @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "checklistItemText", defaultValue = "") val text: String = "",
    @ColumnInfo(name = "checklistItemNoteId") val noteId: UUID,
    @ColumnInfo(name = "checklistItemChecked", defaultValue = "0") val checked: Boolean = false,
    @ColumnInfo(name = "checklistItemPosition", defaultValue = "0") val position: Int = 0,
) {
    override fun toString() =
        "<ChecklistItem: id=$id, text=$text, position=$position, noteId=$noteId>"

    override fun equals(other: Any?) =
        other is ChecklistItem &&
        other.id == id &&
        other.text == text &&
        other.noteId == noteId &&
        other.checked == checked &&
        other.position == position

    override fun hashCode() = id.hashCode()

    open fun copy(
        text: String = this.text,
        checked: Boolean = this.checked,
        position: Int = this.position,
    ): ChecklistItem {
        return ChecklistItem(
            id = id,
            text = text,
            checked = checked,
            position = position,
            noteId = noteId,
        )
    }
}
