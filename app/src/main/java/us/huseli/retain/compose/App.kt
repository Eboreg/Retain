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
import us.huseli.retain.ChecklistNoteDestination
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.Constants.NAV_ARG_NOTE_ID
import us.huseli.retain.DebugDestination
import us.huseli.retain.Enums
import us.huseli.retain.HomeDestination
import us.huseli.retain.Logger
import us.huseli.retain.SettingsDestination
import us.huseli.retain.TextNoteDestination
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
            startDestination = HomeDestination.route,
        ) {
            composable(route = HomeDestination.route) {
                HomeScreen(
                    snackbarHostState = snackbarHostState,
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
                    snackbarHostState = snackbarHostState,
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
                val noteId: UUID = UUID.fromString(it.arguments?.getString(NAV_ARG_NOTE_ID))
                val imageCarouselCurrentId = it.arguments?.getString(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID)

                @Suppress("Destructure")
                TextNoteScreen(
                    snackbarHostState = snackbarHostState,
                    imageCarouselCurrentId = imageCarouselCurrentId,
                    onSave = viewModel.saveTextNote,
                    onBackClick = onClose,
                    onImageClick = { image ->
                        navController.navigate(TextNoteDestination.routeForNoteId(noteId, image.filename))
                    },
                )
            }

            composable(
                route = ChecklistNoteDestination.routeTemplate,
                arguments = ChecklistNoteDestination.arguments,
            ) {
                val noteId: UUID = UUID.fromString(it.arguments?.getString(NAV_ARG_NOTE_ID))
                val imageCarouselCurrentId = it.arguments?.getString(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID)

                @Suppress("Destructure")
                ChecklistNoteScreen(
                    snackbarHostState = snackbarHostState,
                    imageCarouselCurrentId = imageCarouselCurrentId,
                    onSave = viewModel.saveChecklistNote,
                    onBackClick = onClose,
                    onImageClick = { image ->
                        navController.navigate(ChecklistNoteDestination.routeForNoteId(noteId, image.filename))
                    }
                )
            }
        }
    }
}
