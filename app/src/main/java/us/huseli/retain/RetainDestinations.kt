package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import java.util.UUID

interface RetainDestination {
    val route: String
}

abstract class NoteDestination : RetainDestination {
    val arguments = listOf(
        navArgument(NAV_ARG_NOTE_ID) { type = NavType.StringType },
        navArgument(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID) {
            type = NavType.StringType
            nullable = true
        },
    )
    val routeTemplate: String
        get() = "$route/{$NAV_ARG_NOTE_ID}?$NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID={$NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID}"

    fun routeForNoteId(id: UUID) = "$route/$id"

    fun routeForNoteId(id: UUID, imageCarouselCurrentId: String) =
        routeForNoteId(id) + "?$NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID=$imageCarouselCurrentId"
}

object HomeDestination : RetainDestination {
    override val route = "home"
}

object SettingsDestination : RetainDestination {
    override val route = "settings"
}

object DebugDestination : RetainDestination {
    override val route = "debug"
}

object TextNoteDestination : NoteDestination() {
    override val route = "textNote"
}

object ChecklistNoteDestination : NoteDestination() {
    override val route = "checklistNote"
}

