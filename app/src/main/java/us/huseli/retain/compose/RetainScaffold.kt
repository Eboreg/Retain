package us.huseli.retain.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import us.huseli.retain.R
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun RetainScaffold(
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    statusBarColor: Color = MaterialTheme.colorScheme.surface,
    navigationBarColor: Color = MaterialTheme.colorScheme.background,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val nextCloudNeedsTesting by settingsViewModel.nextCloudNeedsTesting.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.logger.snackbarMessage.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    val trashedNoteCount by viewModel.trashedNoteCount.collectAsStateWithLifecycle(0)
    val systemUiController = rememberSystemUiController()

    LaunchedEffect(statusBarColor) {
        systemUiController.setStatusBarColor(statusBarColor)
    }

    LaunchedEffect(navigationBarColor) {
        systemUiController.setNavigationBarColor(navigationBarColor)
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != viewModel.logger.lastSnackbarMessage) {
            snackbarMessage?.let {
                snackbarHostState.showSnackbar(it.message)
                viewModel.logger.setLastSnackbarMessage(it)
            }
        }
    }

    LaunchedEffect(trashedNoteCount) {
        if (trashedNoteCount > 0) {
            val message = context.resources.getQuantityString(
                R.plurals.x_notes_trashed,
                trashedNoteCount,
                trashedNoteCount
            )
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.resources.getString(R.string.undo).uppercase(),
                duration = SnackbarDuration.Long,
                withDismissAction = true,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoTrashNotes()
                SnackbarResult.Dismissed -> viewModel.reallyTrashNotes()
            }
        }
    }

    LaunchedEffect(nextCloudNeedsTesting) {
        if (nextCloudNeedsTesting) settingsViewModel.testNextCloud { result ->
            scope.launch {
                if (!result.success) snackbarHostState.showSnackbar(result.getErrorMessage(context))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.zIndex(2f),
            )
        },
        content = content,
    )
}
