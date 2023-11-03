package us.huseli.retain.dataclasses

data class QuickNoteTodoList(
    val todo: Collection<String>? = null,
    val done: Collection<String>? = null,
)

@Suppress("PropertyName")
data class QuickNoteEntry(
    val creation_date: Long? = null,
    val title: String? = null,
    val last_modification_date: Long? = null,
    val color: String? = null,
    val todolists: Collection<QuickNoteTodoList>? = null,
)
