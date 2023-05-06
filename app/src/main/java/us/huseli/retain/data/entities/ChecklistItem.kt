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
    @ColumnInfo(name = "checklistItemText") var text: String = "",
    @ColumnInfo(name = "checklistItemNoteId") val noteId: UUID,
    @ColumnInfo(name = "checklistItemChecked") var checked: Boolean = false,
    @ColumnInfo(name = "checklistItemPosition") val position: Int = 0,
) {
    override fun toString(): String {
        return "<ChecklistItem: $id / $text>"
    }
}
