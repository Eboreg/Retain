package us.huseli.retain.compose

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import us.huseli.retain.AddChecklistNote
import us.huseli.retain.AddTextNote
import us.huseli.retain.Debug
import us.huseli.retain.EditChecklistNote
import us.huseli.retain.EditTextNote
import us.huseli.retain.Enums
import us.huseli.retain.Home
import us.huseli.retain.Logger
import us.huseli.retain.Settings
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel
import java.util.UUID

@Composable
fun App(
    logger: Logger,
    viewModel: NoteViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    RetainTheme {
        val navController = rememberNavController()
        val onClose: () -> Unit = { navController.popBackStack() }
        val snackbarMessage by logger.snackbarMessage.collectAsStateWithLifecycle(null)
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val nextCloudNeedsTesting by settingsViewModel.nextCloudNeedsTesting.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        snackbarMessage?.let {
            LaunchedEffect(it) {
                snackbarHostState.showSnackbar(it.message)
            }
        }

        if (nextCloudNeedsTesting) {
            settingsViewModel.testNextCloud { result ->
                if (!result.success) {
                    scope.launch { snackbarHostState.showSnackbar(result.getErrorMessage(context)) }
                }
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
                    logger = logger,
                    onClose = onClose,
                    snackbarHostState = snackbarHostState,
                )
            }

            composable(route = Settings.route) {
                SettingsScreen(
                    snackbarHostState = snackbarHostState,
                    onClose = onClose,
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
