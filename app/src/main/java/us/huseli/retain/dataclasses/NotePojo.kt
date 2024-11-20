package us.huseli.retain.dataclasses

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import us.huseli.retain.dataclasses.entities.ChecklistItem
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.dataclasses.entities.Note

data class NotePojo(
    @Embedded val note: Note,
    @Relation(parentColumn = "noteId", entityColumn = "checklistItemNoteId") val checklistItems: List<ChecklistItem>,
    @Relation(parentColumn = "noteId", entityColumn = "imageNoteId") val images: List<Image>,
    @Ignore val databaseVersion: Int? = null,
) {
    constructor(note: Note, checklistItems: List<ChecklistItem>, images: List<Image>) :
        this(note, checklistItems, images, null)

    fun getCardChecklist(): NoteCardChecklistData {
        val items = checklistItems.sorted()
        val filteredItems = if (note.showChecked) items else items.filter { !it.checked }
        val shownItems = filteredItems.take(5)
        val hiddenItems = items.minus(shownItems.toSet())

        return NoteCardChecklistData(
            noteId = note.id,
            shownChecklistItems = shownItems,
            hiddenChecklistItemCount = hiddenItems.size,
            hiddenChecklistItemAllChecked = hiddenItems.all { it.checked },
        )
    }
}
