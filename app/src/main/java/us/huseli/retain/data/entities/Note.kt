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
    @ColumnInfo(name = "noteTitle", defaultValue = "") val title: String = "",
    @ColumnInfo(name = "noteText", defaultValue = "") val text: String = "",
    @ColumnInfo(name = "noteCreated") val created: Instant = Instant.now(),
    @ColumnInfo(name = "noteUpdated") val updated: Instant = Instant.now(),
    @ColumnInfo(name = "notePosition", defaultValue = "0") val position: Int = 0,
    @ColumnInfo(name = "noteType") val type: Enums.NoteType,
    @ColumnInfo(name = "noteShowChecked", defaultValue = "1") val showChecked: Boolean = true,
    @ColumnInfo(name = "noteColorIdx", defaultValue = "0") val colorIdx: Int = 0,
    @ColumnInfo(name = "noteIsDeleted", defaultValue = "0") val isDeleted: Boolean = false,
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
        other.colorIdx == colorIdx &&
        other.isDeleted == isDeleted

    override fun hashCode() = id.hashCode()

    fun copy(
        title: String = this.title,
        text: String = this.text,
        showChecked: Boolean = this.showChecked,
        colorIdx: Int = this.colorIdx,
        position: Int = this.position,
        isDeleted: Boolean = this.isDeleted,
    ): Note {
        return Note(
            id = id,
            title = title,
            text = text,
            created = created,
            updated = Instant.now(),
            position = position,
            type = type,
            showChecked = showChecked,
            colorIdx = colorIdx,
            isDeleted = isDeleted,
        )
    }
}
