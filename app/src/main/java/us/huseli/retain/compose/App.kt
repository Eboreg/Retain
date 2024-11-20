package us.huseli.retain.compose

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retain.ChecklistNoteDestination
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.HomeDestination
import us.huseli.retain.ImageCarouselDestination
import us.huseli.retain.SettingsDestination
import us.huseli.retain.TestDestination
import us.huseli.retain.TextNoteDestination
import us.huseli.retain.compose.notescreen.ChecklistNoteScreen
import us.huseli.retain.compose.notescreen.TextNoteScreen
import us.huseli.retain.compose.settings.SettingsScreen
import us.huseli.retain.viewmodels.NoteListViewModel
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun App(
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val onClose: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
        modifier = modifier,
    ) {
        composable(route = HomeDestination.route) {
            HomeScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onAddTextNoteClick = { navController.navigate(TextNoteDestination.route()) },
                onAddChecklistClick = { navController.navigate(ChecklistNoteDestination.route()) },
                onCardClick = { note ->
                    when (note.type) {
                        NoteType.TEXT -> navController.navigate(TextNoteDestination.route(note.id))
                        NoteType.CHECKLIST -> navController.navigate(ChecklistNoteDestination.route(note.id))
                    }
                },
                onSettingsClick = { navController.navigate(SettingsDestination.route) },
                onTestClick = { navController.navigate(TestDestination.route) },
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
            route = ChecklistNoteDestination.routeTemplate,
            arguments = ChecklistNoteDestination.arguments,
        ) {
            ChecklistNoteScreen(
                onBackClick = onClose,
                onImageCarouselStart = { noteId, imageId ->
                    navController.navigate(ImageCarouselDestination.route(noteId, imageId))
                },
            )
        }

        composable(
            route = TextNoteDestination.routeTemplate,
            arguments = TextNoteDestination.arguments,
        ) {
            TextNoteScreen(
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
            ImageCarouselScreen(onClose = onClose)
        }

        composable(route = TestDestination.route) {
            TestScreen()
        }
    }
}
