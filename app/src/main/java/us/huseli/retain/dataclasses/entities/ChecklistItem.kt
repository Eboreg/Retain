package us.huseli.retain.dataclasses.entities

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
data class ChecklistItem(
    @ColumnInfo(name = "checklistItemId") @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "checklistItemText", defaultValue = "") val text: String = "",
    @ColumnInfo(name = "checklistItemNoteId") val noteId: UUID,
    @ColumnInfo(name = "checklistItemChecked", defaultValue = "0") val checked: Boolean = false,
    @ColumnInfo(name = "checklistItemPosition", defaultValue = "0") val position: Int = 0,
) : Comparable<ChecklistItem> {
    val compareValue: Int
        get() = position + if (checked) 100_000 else 0

    override fun compareTo(other: ChecklistItem): Int = compareValue - other.compareValue

    override fun equals(other: Any?) = other is ChecklistItem &&
        other.id == id &&
        other.text == text &&
        other.noteId == noteId &&
        other.checked == checked &&
        other.position == position

    override fun hashCode() = id.hashCode()
}
