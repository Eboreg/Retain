package us.huseli.retain.dataclasses.google

data class GoogleNoteEntry(
    val attachments: List<GoogleNoteAttachment>? = null,
    val color: String? = null,
    val isTrashed: Boolean = false,
    val isArchived: Boolean = false,
    val listContent: List<GoogleNoteListContent>? = null,
    val title: String? = null,
    val userEditedTimestampUsec: Long? = null,
    val createdTimestampUsec: Long? = null,
    val textContent: String? = null,
)