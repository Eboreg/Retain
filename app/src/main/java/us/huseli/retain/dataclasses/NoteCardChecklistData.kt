package us.huseli.retain.dataclasses

import us.huseli.retain.dataclasses.entities.ChecklistItem
import java.util.UUID

data class NoteCardChecklistData(
    val noteId: UUID,
    val shownChecklistItems: List<ChecklistItem>,
    val hiddenChecklistItemCount: Int,
    val hiddenChecklistItemAllChecked: Boolean,
)
