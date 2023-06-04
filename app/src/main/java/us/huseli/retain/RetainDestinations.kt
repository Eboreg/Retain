package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Constants.NAV_ARG_SELECTED_IMAGE
import us.huseli.retain.Constants.NAV_ARG_SELECTED_NOTE
import java.util.UUID

abstract class NoteDestination {
    abstract val baseRoute: String
    val arguments = listOf(
        navArgument(NAV_ARG_NOTE_ID) { type = NavType.StringType },
        navArgument(NAV_ARG_SELECTED_IMAGE) {
            type = NavType.StringType
            nullable = true
        }
    )
    val routeTemplate: String
        get() = "$baseRoute/{$NAV_ARG_NOTE_ID}?$NAV_ARG_SELECTED_IMAGE={$NAV_ARG_SELECTED_IMAGE}"

    fun route(noteId: UUID, selectedImageId: String? = null) =
        if (selectedImageId == null) "$baseRoute/$noteId"
        else "$baseRoute/$noteId?$NAV_ARG_SELECTED_IMAGE=$selectedImageId"
}

object HomeDestination {
    private const val baseRoute = "home"
    const val routeTemplate = "$baseRoute?$NAV_ARG_SELECTED_NOTE={$NAV_ARG_SELECTED_NOTE}"
    val arguments = listOf(
        navArgument(NAV_ARG_SELECTED_NOTE) {
            type = NavType.StringType
            nullable = true
        }
    )

    fun route(selectedNoteId: UUID? = null): String =
        if (selectedNoteId == null) baseRoute else "$baseRoute?$NAV_ARG_SELECTED_NOTE=$selectedNoteId"
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
