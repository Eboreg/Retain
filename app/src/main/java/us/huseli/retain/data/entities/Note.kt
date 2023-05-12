package us.huseli.retain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retain.Enums
import java.time.Instant
import java.util.UUID

@Entity(indices = [Index("notePosition")])
open class Note(
    @PrimaryKey
    @ColumnInfo(name = "noteId")
    val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "noteTitle") val title: String = "",
    @ColumnInfo(name = "noteText") val text: String = "",
    @ColumnInfo(name = "noteCreated") val created: Instant = Instant.now(),
    @ColumnInfo(name = "noteUpdated") val updated: Instant = Instant.now(),
    @ColumnInfo(name = "notePosition") val position: Int = 0,
    @ColumnInfo(name = "noteType") val type: Enums.NoteType,
    @ColumnInfo(name = "noteShowChecked") val showChecked: Boolean = true,
    @ColumnInfo(name = "noteColorIdx") val colorIdx: Int = 0,
) : Comparable<Note> {
    override fun toString() = "<Note: id=$id, title=$title]>"

    override fun compareTo(other: Note) = (updated.epochSecond - other.updated.epochSecond).toInt()

    override fun equals(other: Any?) =
        other is Note &&
        other.id == id &&
        other.title == title &&
        other.text == text &&
        other.created == created &&
        other.updated == updated &&
        other.position == position &&
        other.type == type &&
        other.showChecked == showChecked &&
        other.colorIdx == colorIdx

    override fun hashCode() = id.hashCode()
}
