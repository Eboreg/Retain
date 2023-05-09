package us.huseli.retain.data.entities

class NoteCombined(
    note: Note,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val databaseVersion: Int,
) : Note(
    id = note.id,
    title = note.title,
    text = note.text,
    created = note.created,
    updated = note.updated,
    position = note.position,
    type = note.type,
    showChecked = note.showChecked
) {
    override fun equals(other: Any?) =
        other is NoteCombined &&
        other.id == id &&
        other.title == title &&
        other.text == text &&
        other.created == created &&
        other.updated == updated &&
        other.position == position &&
        other.type == type &&
        other.showChecked == showChecked &&
        other.checklistItems.all { checklistItems.contains(it) }

    override fun hashCode() = 31 * id.hashCode() + checklistItems.sortedBy { it.id }.hashCode()
}
