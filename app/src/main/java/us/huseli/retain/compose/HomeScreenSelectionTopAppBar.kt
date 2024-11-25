package us.huseli.retain.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Archive
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.SelectAll
import androidx.compose.material.icons.sharp.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenSelectionTopAppBar(
    modifier: Modifier = Modifier.Companion,
    selectedCount: Int,
    onCloseClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onArchiveClick: () -> Unit,
    showArchive: Boolean,
) {
    TopAppBar(
        title = { Text(selectedCount.toString()) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Sharp.Close,
                    contentDescription = stringResource(R.string.exit_selection_mode)
                )
            }
        },
        actions = {
            IconButton(onClick = onSelectAllClick) {
                Icon(
                    imageVector = Icons.Sharp.SelectAll,
                    contentDescription = stringResource(R.string.select_all_notes)
                )
            }
            IconButton(onClick = onArchiveClick) {
                Icon(
                    imageVector = if (showArchive) Icons.Sharp.Unarchive else Icons.Sharp.Archive,
                    contentDescription = stringResource(
                        if (showArchive) R.string.unarchive_selected_notes
                        else R.string.archive_selected_notes
                    )
                )
            }
            IconButton(onClick = onTrashClick) {
                Icon(
                    imageVector = Icons.Sharp.Delete,
                    contentDescription = stringResource(R.string.delete_selected_notes)
                )
            }
        }
    )
}
