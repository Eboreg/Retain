package us.huseli.retain.compose.notescreen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R

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