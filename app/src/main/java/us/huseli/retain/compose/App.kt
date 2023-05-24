package us.huseli.retain.compose

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retain.ChecklistNoteDestination
import us.huseli.retain.DebugDestination
import us.huseli.retain.Enums
import us.huseli.retain.HomeDestination
import us.huseli.retain.Logger
import us.huseli.retain.SettingsDestination
import us.huseli.retain.TextNoteDestination
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.NoteViewModel
import java.util.UUID

@Composable
fun App(logger: Logger, viewModel: NoteViewModel = hiltViewModel()) {
    RetainTheme {
        val navController = rememberNavController()
        val onClose: () -> Unit = { navController.popBackStack() }
        val snackbarHostState = remember { SnackbarHostState() }

        NavHost(
            navController = navController,
            startDestination = HomeDestination.route,
        ) {
            composable(route = HomeDestination.route) {
                HomeScreen(
                    onAddChecklistClick = {
                        navController.navigate(ChecklistNoteDestination.routeForNoteId(UUID.randomUUID()))
                    },
                    onAddTextNoteClick = {
                        navController.navigate(TextNoteDestination.routeForNoteId(UUID.randomUUID()))
                    },
                    onCardClick = { note ->
                        when (note.type) {
                            Enums.NoteType.TEXT -> navController.navigate(TextNoteDestination.routeForNoteId(note.id))
                            Enums.NoteType.CHECKLIST ->
                                navController.navigate(ChecklistNoteDestination.routeForNoteId(note.id))
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(SettingsDestination.route)
                    },
                    onDebugClick = {
                        navController.navigate(DebugDestination.route)
                    },
                )
            }

            composable(route = DebugDestination.route) {
                DebugScreen(
                    logger = logger,
                    onClose = onClose,
                )
            }

            composable(route = SettingsDestination.route) {
                SettingsScreen(
                    snackbarHostState = snackbarHostState,
                    onClose = onClose,
                )
            }

            composable(
                route = TextNoteDestination.routeTemplate,
                arguments = TextNoteDestination.arguments,
            ) {
                TextNoteScreen(
                    onSave = { shouldSave, combo -> if (shouldSave) viewModel.saveCombo(combo) },
                    onBackClick = onClose,
                )
            }

            composable(
                route = ChecklistNoteDestination.routeTemplate,
                arguments = ChecklistNoteDestination.arguments,
            ) {
                ChecklistNoteScreen(
                    onSave = { shouldSave, combo -> if (shouldSave) viewModel.saveCombo(combo) },
                    onBackClick = onClose,
                )
            }
        }
    }
}
