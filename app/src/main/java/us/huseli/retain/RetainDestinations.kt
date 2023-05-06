package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.util.UUID

interface RetainDestination {
    val route: String
}

abstract class AbstractNoteDestination : RetainDestination {
    private val idArg = "noteId"
    val arguments = listOf(navArgument(idArg) { type = NavType.StringType })
    val routeTemplate: String
        get() = "$route/{$idArg}"

    fun routeForNoteId(id: UUID) = "$route/$id"
}

object Home : RetainDestination {
    override val route = "home"
}

object EditTextNote : AbstractNoteDestination() {
    override val route = "editTextNote"
}

object EditChecklistNote : AbstractNoteDestination() {
    override val route = "editChecklistNote"
}

object AddTextNote : AbstractNoteDestination() {
    override val route = "addTextNote"
}

object AddChecklistNote : AbstractNoteDestination() {
    override val route = "addChecklistNote"
}
