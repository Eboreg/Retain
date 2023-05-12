package us.huseli.retain.compose

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retain.AddChecklistNote
import us.huseli.retain.AddTextNote
import us.huseli.retain.Debug
import us.huseli.retain.EditChecklistNote
import us.huseli.retain.EditTextNote
import us.huseli.retain.Enums
import us.huseli.retain.Home
import us.huseli.retain.Settings
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.NoteViewModel
import java.util.UUID

@Composable
fun App(viewModel: NoteViewModel = hiltViewModel()) {
    RetainTheme {
        val navController = rememberNavController()
        val onClose: () -> Unit = { navController.popBackStack() }
        val logMessage by viewModel.latestLogMessage.collectAsStateWithLifecycle(null)
        val snackbarHostState = remember { SnackbarHostState() }

        logMessage?.let {
            LaunchedEffect(it) {
                snackbarHostState.showSnackbar(it.message)
            }
        }

        NavHost(
            navController = navController,
            startDestination = Home.route,
        ) {
            composable(route = Home.route) {
                HomeScreen(
                    snackbarHostState = snackbarHostState,
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
                    },
                    onSettingsClick = {
                        navController.navigate(Settings.route)
                    },
                    onDebugClick = {
                        navController.navigate(Debug.route)
                    },
                )
            }

            composable(route = Debug.route) {
                DebugScreen(
                    onClose = onClose,
                    snackbarHostState = snackbarHostState,
                )
            }

            composable(route = Settings.route) {
                SettingsScreen(
                    onClose = onClose,
                    snackbarHostState = snackbarHostState,
                )
            }

            composable(
                route = AddTextNote.routeTemplate,
                arguments = AddTextNote.arguments,
            ) {
                TextNoteScreen(
                    snackbarHostState = snackbarHostState,
                    onSave = viewModel.saveTextNote,
                    onClose = onClose,
                )
            }

            composable(
                route = AddChecklistNote.routeTemplate,
                arguments = AddChecklistNote.arguments,
            ) {
                ChecklistNoteScreen(
                    snackbarHostState = snackbarHostState,
                    onSave = viewModel.saveChecklistNote,
                    onClose = onClose,
                )
            }

            composable(
                route = EditChecklistNote.routeTemplate,
                arguments = EditChecklistNote.arguments,
            ) {
                ChecklistNoteScreen(
                    snackbarHostState = snackbarHostState,
                    onSave = viewModel.saveChecklistNote,
                    onClose = onClose,
                )
            }

            composable(
                route = EditTextNote.routeTemplate,
                arguments = EditTextNote.arguments,
            ) {
                TextNoteScreen(
                    snackbarHostState = snackbarHostState,
                    onSave = viewModel.saveTextNote,
                    onClose = onClose,
                )
            }
        }
    }
}
