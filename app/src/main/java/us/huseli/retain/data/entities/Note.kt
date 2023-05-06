package us.huseli.retain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.retain.Enums
import java.util.Date
import java.util.UUID

@Entity(indices = [Index("notePosition")])
class Note(
    @PrimaryKey
    @ColumnInfo(name = "noteId")
    val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "noteTitle") val title: String = "",
    @ColumnInfo(name = "noteText") val text: String = "",
    @ColumnInfo(name = "noteCreated") val created: Date = Date(),
    @ColumnInfo(name = "noteUpdated") val updated: Date = Date(),
    @ColumnInfo(name = "notePosition") val position: Int = 0,
    @ColumnInfo(name = "noteType") val type: Enums.NoteType,
    @ColumnInfo(name = "noteShowChecked") val showChecked: Boolean = true,
) {
    override fun toString(): String {
        return "<Note: id=$id, title=$title]>"
    }
}
