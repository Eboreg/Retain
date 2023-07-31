package us.huseli.retain.data.entities

import androidx.room.Embedded

data class ChecklistItemWithNote(
    @Embedded val checklistItem: ChecklistItem,
    @Embedded val note: Note,
)
