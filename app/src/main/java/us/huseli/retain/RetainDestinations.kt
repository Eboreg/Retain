package us.huseli.retain

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NEW_NOTE_TYPE
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.Enums.NoteType
import java.util.UUID

object HomeDestination {
    const val route = "home"
}

object SettingsDestination {
    const val route = "settings"
}

object DebugDestination {
    const val route = "debug"
}

object NoteDestination {
    const val routeTemplate = "note?id={$NAV_ARG_NOTE_ID}&type={$NAV_ARG_NEW_NOTE_TYPE}"
    val arguments = listOf(
        navArgument(NAV_ARG_NOTE_ID) {
            type = NavType.StringType
            nullable = true
        },
        navArgument(NAV_ARG_NEW_NOTE_TYPE) {
            type = NavType.StringType
            nullable = true
        },
    )

    fun route(noteId: UUID) = "note?id=$noteId"

    fun route(newNoteType: NoteType) = "note?type=$newNoteType"
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
