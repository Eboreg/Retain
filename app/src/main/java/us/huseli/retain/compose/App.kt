package us.huseli.retain.compose

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retaintheme.ui.theme.RetainTheme
import us.huseli.retain.ChecklistNoteDestination
import us.huseli.retain.DebugDestination
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.HomeDestination
import us.huseli.retain.ImageCarouselDestination
import us.huseli.retain.Logger
import us.huseli.retain.SettingsDestination
import us.huseli.retain.TextNoteDestination
import us.huseli.retain.compose.notescreen.ChecklistNoteScreen
import us.huseli.retain.compose.notescreen.TextNoteScreen
import us.huseli.retain.compose.settings.SettingsScreen
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel
import java.util.UUID

@Composable
fun App(
    logger: Logger,
    viewModel: NoteViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    RetainTheme {
        val onClose: () -> Unit = { navController.popBackStack() }

        NavHost(
            navController = navController,
            startDestination = HomeDestination.route,
        ) {
            composable(route = HomeDestination.route) {
                HomeScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    onAddTextNoteClick = {
                        navController.navigate(TextNoteDestination.route(UUID.randomUUID()))
                    },
                    onAddChecklistClick = {
                        navController.navigate(ChecklistNoteDestination.route(UUID.randomUUID()))
                    },
                    onCardClick = { note ->
                        when (note.type) {
                            NoteType.TEXT -> navController.navigate(TextNoteDestination.route(note.id))
                            NoteType.CHECKLIST -> navController.navigate(ChecklistNoteDestination.route(note.id))
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
                    viewModel = settingsViewModel,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onClose,
                )
            }

            composable(
                route = TextNoteDestination.routeTemplate,
                arguments = TextNoteDestination.arguments,
            ) {
                TextNoteScreen(
                    onSave = { dirtyNote, dirtyChecklistItems, dirtyImages, deletedChecklistItemIds, deletedImageIds ->
                        viewModel.save(
                            dirtyNote,
                            dirtyChecklistItems,
                            dirtyImages,
                            deletedChecklistItemIds,
                            deletedImageIds
                        )
                        viewModel.uploadNotes()
                    },
                    onBackClick = onClose,
                    onImageCarouselStart = { noteId, imageId ->
                        navController.navigate(ImageCarouselDestination.route(noteId, imageId))
                    },
                )
            }

            composable(
                route = ChecklistNoteDestination.routeTemplate,
                arguments = ChecklistNoteDestination.arguments,
            ) {
                ChecklistNoteScreen(
                    onSave = { dirtyNote, dirtyChecklistItems, dirtyImages, deletedChecklistItemIds, deletedImageIds ->
                        viewModel.save(
                            dirtyNote,
                            dirtyChecklistItems,
                            dirtyImages,
                            deletedChecklistItemIds,
                            deletedImageIds
                        )
                        viewModel.uploadNotes()
                    },
                    onBackClick = onClose,
                    onImageCarouselStart = { noteId, imageId ->
                        navController.navigate(ImageCarouselDestination.route(noteId, imageId))
                    },
                )
            }

            composable(
                route = ImageCarouselDestination.routeTemplate,
                arguments = ImageCarouselDestination.arguments,
            ) {
                ImageCarouselScreen(
                    onClose = onClose,
                )
            }
        }
    }
}
