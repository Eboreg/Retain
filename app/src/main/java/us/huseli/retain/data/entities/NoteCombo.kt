package us.huseli.retain.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class NoteCombo(
    @Embedded
    val note: Note,
    @Relation(parentColumn = "noteId", entityColumn = "checklistItemNoteId")
    val checklistItems: List<ChecklistItem>,
    @Relation(parentColumn = "noteId", entityColumn = "imageNoteId")
    val images: List<Image>,
    val databaseVersion: Int? = null,
)
