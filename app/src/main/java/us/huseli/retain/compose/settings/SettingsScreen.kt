package us.huseli.retain.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.compose.RetainScaffold
import us.huseli.retain.viewmodels.SettingsViewModel


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
) {
    val keepImportIsActive by viewModel.keepImportIsActive.collectAsStateWithLifecycle()
    val quickNoteImportIsActive by viewModel.quickNoteImportIsActive.collectAsStateWithLifecycle()
    val importActionCount by viewModel.importActionCount.collectAsStateWithLifecycle()
    val importCurrentAction by viewModel.importCurrentAction.collectAsStateWithLifecycle()
    val importCurrentActionIndex by viewModel.importCurrentActionIndex.collectAsStateWithLifecycle()

    if (keepImportIsActive || quickNoteImportIsActive) {
        ExternalImportDialog(
            actionCount = importActionCount,
            currentAction = importCurrentAction,
            currentActionIndex = importCurrentActionIndex,
            onCancel = { viewModel.cancelImport() },
            serviceName = if (keepImportIsActive) "Google Keep" else "Quicknote",
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.save()
        }
    }

    RetainScaffold(
        topBar = { SettingsTopAppBar(onBackClick = onBackClick) },
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            columns = StaggeredGridCells.Adaptive(minSize = 400.dp),
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                GeneralSection(modifier = Modifier.fillMaxWidth(), viewModel = viewModel)
            }
            item {
                SyncBackendSection(
                    modifier = Modifier.fillMaxWidth(),
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                )
            }
            item {
                QuickNoteImportSection(modifier = Modifier.fillMaxWidth(), viewModel = viewModel)
            }
            item {
                KeepImportSection(modifier = Modifier.fillMaxWidth(), viewModel = viewModel)
            }
        }
    }
}
