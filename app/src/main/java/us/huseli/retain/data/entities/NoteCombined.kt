package us.huseli.retain.data.entities

class NoteCombined(
    note: Note,
    val checklistItems: Collection<ChecklistItem> = emptyList(),
    val images: Collection<Image> = emptyList(),
    @Suppress("unused") val databaseVersion: Int,
) : Note(
    id = note.id,
    title = note.title,
    text = note.text,
    created = note.created,
    updated = note.updated,
    position = note.position,
    type = note.type,
    showChecked = note.showChecked,
    colorIdx = note.colorIdx,
    isDeleted = note.isDeleted,
) {
    override fun equals(other: Any?) =
        other is NoteCombined &&
        super.equals(other) &&
        other.checklistItems.all { checklistItems.contains(it) } &&
        other.images.all { images.contains(it) }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + checklistItems.sortedBy { it.id }.hashCode()
        result = 31 * result + images.sortedBy { it.filename }.hashCode()
        return result
    }
}
