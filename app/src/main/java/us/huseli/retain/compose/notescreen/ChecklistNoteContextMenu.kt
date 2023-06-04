package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R

@Composable
fun ChecklistNoteContextMenu(
    modifier: Modifier = Modifier,
    onUncheckAllClick: () -> Unit,
    onDeleteCheckedClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Sharp.MoreVert,
                contentDescription = null,
            )
        }
        DropdownMenu(
            modifier = modifier,
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuItem(
                onClick = {
                    onUncheckAllClick()
                    isExpanded = false
                },
                text = { Text(stringResource(R.string.uncheck_all)) },
            )
            DropdownMenuItem(
                onClick = {
                    onDeleteCheckedClick()
                    isExpanded = false
                },
                text = { Text(stringResource(R.string.delete_checked)) },
            )
        }
    }
}
