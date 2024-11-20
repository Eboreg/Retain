package us.huseli.retain.dataclasses

data class QuickNoteTodoList(
    val todo: Collection<String>? = null,
    val done: Collection<String>? = null,
)