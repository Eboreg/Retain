package us.huseli.retain.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.BugReport
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.GridView
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material.icons.sharp.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retain.Enums.HomeScreenViewType
import us.huseli.retain.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    onCloseClick: () -> Unit,
    onTrashClick: () -> Unit,
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
            IconButton(onClick = onTrashClick) {
                Icon(
                    imageVector = Icons.Sharp.Delete,
                    contentDescription = stringResource(R.string.delete_selected_notes)
                )
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopAppBar(
    modifier: Modifier = Modifier,
    viewType: HomeScreenViewType,
    onSettingsClick: () -> Unit,
    onDebugClick: () -> Unit,
    onViewTypeClick: (HomeScreenViewType) -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            Image(
                bitmap = ImageBitmap.imageResource(R.mipmap.ic_launcher_round),
                modifier = Modifier.height(50.dp),
                contentDescription = null,
            )
        },
        actions = {
            when (viewType) {
                HomeScreenViewType.GRID -> {
                    IconButton(onClick = { onViewTypeClick(HomeScreenViewType.LIST) }) {
                        Icon(
                            imageVector = Icons.Sharp.ViewAgenda,
                            contentDescription = stringResource(R.string.list_view),
                        )
                    }
                }

                HomeScreenViewType.LIST -> {
                    IconButton(onClick = { onViewTypeClick(HomeScreenViewType.GRID) }) {
                        Icon(
                            imageVector = Icons.Sharp.GridView,
                            contentDescription = stringResource(R.string.grid_view)
                        )
                    }
                }
            }
            IconButton(onClick = onDebugClick) {
                Icon(
                    imageVector = Icons.Sharp.BugReport,
                    contentDescription = stringResource(R.string.debug),
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Sharp.Settings,
                    contentDescription = stringResource(R.string.app_settings),
                )
            }
        }
    )
}
