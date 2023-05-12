package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retain.Constants.NOTE_ID_SAVED_STATE_KEY
import java.util.UUID

interface RetainDestination {
    val route: String
}

abstract class NoteDestination : RetainDestination {
    private val idArg = NOTE_ID_SAVED_STATE_KEY
    val arguments = listOf(navArgument(idArg) { type = NavType.StringType })
    val routeTemplate: String
        get() = "$route/{$idArg}"

    fun routeForNoteId(id: UUID) = "$route/$id"
}

object Home : RetainDestination {
    override val route = "home"
}

object Settings : RetainDestination {
    override val route = "settings"
}

object Debug : RetainDestination {
    override val route = "debug"
}

object EditTextNote : NoteDestination() {
    override val route = "editTextNote"
}

object EditChecklistNote : NoteDestination() {
    override val route = "editChecklistNote"
}

object AddTextNote : NoteDestination() {
    override val route = "addTextNote"
}

object AddChecklistNote : NoteDestination() {
    override val route = "addChecklistNote"
}
