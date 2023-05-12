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
class ChecklistItem(
    @PrimaryKey
    @ColumnInfo(name = "checklistItemId")
    val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "checklistItemText") val text: String = "",
    @ColumnInfo(name = "checklistItemNoteId") val noteId: UUID,
    @ColumnInfo(name = "checklistItemChecked") val checked: Boolean = false,
    @ColumnInfo(name = "checklistItemPosition") val position: Int = 0,
) {
    override fun toString() = "<ChecklistItem: $id / $text / $position>"

    override fun equals(other: Any?) =
        other is ChecklistItem &&
        other.id == id &&
        other.text == text &&
        other.noteId == noteId &&
        other.checked == checked &&
        other.position == position

    override fun hashCode() = id.hashCode()

    fun copy(text: String? = null, checked: Boolean? = null, position: Int? = null): ChecklistItem {
        return ChecklistItem(
            id = id,
            text = text ?: this.text,
            checked = checked ?: this.checked,
            position = position ?: this.position,
            noteId = noteId
        )
    }
}
