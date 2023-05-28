package us.huseli.retain.data.entities

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation

data class NoteCombo(
    @Embedded
    val note: Note,
    @Relation(parentColumn = "noteId", entityColumn = "checklistItemNoteId")
    val checklistItems: List<ChecklistItem>,
    @Relation(parentColumn = "noteId", entityColumn = "imageNoteId")
    val images: List<Image>,
    @Ignore val databaseVersion: Int? = null,
) {
    constructor(note: Note, checklistItems: List<ChecklistItem>, images: List<Image>) :
        this(note, checklistItems, images, null)
}
