@file:Suppress("ConstPropertyName")

package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import java.util.UUID

object HomeDestination {
    const val route = "home"
}

object SettingsDestination {
    const val route = "settings"
}

open class BaseNoteDestination(noteType: NoteType) {
    private val baseRoute = "note/${noteType.name}"

    val routeTemplate = "${baseRoute}?id={$NAV_ARG_NOTE_ID}"
    val arguments = listOf(
        navArgument(NAV_ARG_NOTE_ID) {
            type = NavType.StringType
            nullable = true
        },
    )

    fun route(noteId: UUID) = "${baseRoute}?id=$noteId"

    fun route() = baseRoute
}

object ChecklistNoteDestination : BaseNoteDestination(NoteType.CHECKLIST)

object TextNoteDestination : BaseNoteDestination(NoteType.TEXT)

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

object TestDestination {
    const val route = "test"
}
