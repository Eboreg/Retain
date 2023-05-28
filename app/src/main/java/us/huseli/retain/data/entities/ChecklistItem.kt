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
    @PrimaryKey
    @ColumnInfo(name = "checklistItemId")
    val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "checklistItemText", defaultValue = "") val text: String = "",
    @ColumnInfo(name = "checklistItemNoteId") val noteId: UUID,
    @ColumnInfo(name = "checklistItemChecked", defaultValue = "0") val checked: Boolean = false,
    @ColumnInfo(name = "checklistItemPosition", defaultValue = "0") val position: Int = 0,
    @ColumnInfo(name = "checklistItemIsDeleted", defaultValue = "0") val isDeleted: Boolean = false,
) {
    override fun toString() =
        "<ChecklistItem: id=$id, text=$text, position=$position, noteId=$noteId, isDeleted=$isDeleted>"

    override fun equals(other: Any?) =
        other is ChecklistItem &&
        other.id == id &&
        other.text == text &&
        other.noteId == noteId &&
        other.checked == checked &&
        other.position == position &&
        other.isDeleted == isDeleted

    override fun hashCode() = id.hashCode()

    open fun copy(
        text: String = this.text,
        checked: Boolean = this.checked,
        position: Int = this.position,
        isDeleted: Boolean = this.isDeleted,
    ): ChecklistItem {
        return ChecklistItem(
            id = id,
            text = text,
            checked = checked,
            position = position,
            noteId = noteId,
            isDeleted = isDeleted,
        )
    }
}
