package us.huseli.retain.compose.notescreen.bars

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.viewmodels.AbstractNoteViewModel

@Composable
fun ImageSelectionTopAppBar(
    viewModel: AbstractNoteViewModel<*>,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val imageUiStates by viewModel.imageUiStates.collectAsStateWithLifecycle()

    ImageSelectionTopAppBar(
        modifier = modifier,
        backgroundColor = backgroundColor,
        selectedImageCount = imageUiStates.filter { it.isSelected }.size,
        onCloseClick = { viewModel.deselectAllImages() },
        onSelectAllClick = { viewModel.selectAllImages() },
        onTrashClick = { viewModel.deleteSelectedImages() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectionTopAppBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    selectedImageCount: Int,
    onCloseClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onTrashClick: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(selectedImageCount.toString()) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Sharp.Close,
                    contentDescription = stringResource(R.string.exit_selection_mode),
                )
            }
        },
        actions = {
            IconButton(onClick = onSelectAllClick) {
                Icon(
                    imageVector = Icons.Sharp.SelectAll,
                    contentDescription = stringResource(R.string.select_all_images),
                )
            }
            IconButton(onClick = onTrashClick) {
                Icon(
                    imageVector = Icons.Sharp.Delete,
                    contentDescription = stringResource(R.string.delete_selected_images),
                )
            }
        }
    )
}