package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import java.util.UUID

abstract class NoteDestination {
    abstract val baseRoute: String
    val arguments = listOf(
        navArgument(NAV_ARG_NOTE_ID) { type = NavType.StringType },
    )
    val routeTemplate: String
        get() = "$baseRoute/{$NAV_ARG_NOTE_ID}"

    fun route(noteId: UUID) = "$baseRoute/$noteId"
}

object HomeDestination {
    const val route = "home"
}

object SettingsDestination {
    const val route = "settings"
}

object DebugDestination {
    const val route = "debug"
}

object TextNoteDestination : NoteDestination() {
    override val baseRoute = "textNote"
}

object ChecklistNoteDestination : NoteDestination() {
    override val baseRoute = "checklistNote"
}

object ImageCarouselDestination {
    private const val baseRoute = "imageCarousel"
    const val routeTemplate = "$baseRoute/{$NAV_ARG_NOTE_ID}/{$NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID}"
    val arguments = listOf(
        navArgument(NAV_ARG_NOTE_ID) { type = NavType.StringType },
        navArgument(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID) {
            type = NavType.StringType
            nullable = true
        },
    )

    fun route(noteId: UUID, imageId: String) = "$baseRoute/$noteId/$imageId"
}
