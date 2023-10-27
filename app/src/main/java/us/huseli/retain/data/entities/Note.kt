package us.huseli.retain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retain.Enums.NoteType
import java.time.Instant
import java.util.UUID

@Entity(indices = [Index("notePosition")])
data class Note(
    @ColumnInfo(name = "noteId") @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "noteTitle", defaultValue = "") val title: String = "",
    @ColumnInfo(name = "noteText", defaultValue = "") val text: String = "",
    @ColumnInfo(name = "noteCreated") val created: Instant = Instant.now(),
    @ColumnInfo(name = "noteUpdated") val updated: Instant = Instant.now(),
    @ColumnInfo(name = "notePosition", defaultValue = "0") val position: Int = 0,
    @ColumnInfo(name = "noteType") val type: NoteType,
    @ColumnInfo(name = "noteShowChecked", defaultValue = "1") val showChecked: Boolean = true,
    @ColumnInfo(name = "noteColor", defaultValue = "DEFAULT") val color: String = "DEFAULT",
    @ColumnInfo(name = "noteIsDeleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "noteIsArchived", defaultValue = "0") val isArchived: Boolean = false,
) : Comparable<Note> {
    override fun toString() =
        "<Note: id=$id, title=$title, created=$created, updated=$updated, isDeleted=$isDeleted, isArchived=$isArchived]>"

    override fun compareTo(other: Note) = (updated.epochSecond - other.updated.epochSecond).toInt()

    override fun equals(other: Any?) = other is Note &&
        other.id == id &&
        other.title == title &&
        other.text == text &&
        other.created == created &&
        other.updated == updated &&
        other.position == position &&
        other.type == type &&
        other.showChecked == showChecked &&
        other.color == color &&
        other.isDeleted == isDeleted &&
        other.isArchived == isArchived

    override fun hashCode() = id.hashCode()
}
