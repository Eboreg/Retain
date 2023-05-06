package us.huseli.retain.compose

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retain.AddChecklistNote
import us.huseli.retain.AddTextNote
import us.huseli.retain.EditChecklistNote
import us.huseli.retain.EditTextNote
import us.huseli.retain.Enums
import us.huseli.retain.Home
import us.huseli.retain.ui.theme.RetainTheme
import java.util.UUID

@Composable
fun App() {
    RetainTheme {
        val navController = rememberNavController()
        val onClose: () -> Unit = { navController.popBackStack() }

        NavHost(
            navController = navController,
            startDestination = Home.route,
        ) {
            composable(route = Home.route) {
                HomeScreen(
                    onAddChecklistClick = {
                        navController.navigate(AddChecklistNote.routeForNoteId(UUID.randomUUID()))
                    },
                    onAddTextNoteClick = {
                        navController.navigate(AddTextNote.routeForNoteId(UUID.randomUUID()))
                    },
                    onCardClick = { note ->
                        when (note.type) {
                            Enums.NoteType.CHECKLIST -> navController.navigate(EditChecklistNote.routeForNoteId(note.id))
                            Enums.NoteType.TEXT -> navController.navigate(EditTextNote.routeForNoteId(note.id))
                        }
                    }
                )
            }

            composable(
                route = AddTextNote.routeTemplate,
                arguments = AddTextNote.arguments,
            ) {
                EditTextNoteScreen(onClose = onClose)
            }

            composable(
                route = AddChecklistNote.routeTemplate,
                arguments = AddChecklistNote.arguments,
            ) {
                EditChecklistNoteScreen(onClose = onClose)
            }

            composable(
                route = EditChecklistNote.routeTemplate,
                arguments = EditChecklistNote.arguments,
            ) {
                EditChecklistNoteScreen { onClose() }
            }

            composable(
                route = EditTextNote.routeTemplate,
                arguments = EditTextNote.arguments,
            ) {
                EditTextNoteScreen { onClose() }
            }
        }
    }
}
