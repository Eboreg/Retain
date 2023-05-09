package us.huseli.retain.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R
import java.util.UUID

@Composable
fun DeleteNotesDialog(
    modifier: Modifier = Modifier,
    selectedNoteIds: Collection<UUID>,
    onDelete: (Collection<UUID>) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(stringResource(R.string.delete_notes)) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.delete_x_notes,
                    selectedNoteIds.size,
                    selectedNoteIds.size
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(selectedNoteIds)
                    onClose()
                }
            ) {
                Text(stringResource(R.string.delete).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
        onDismissRequest = onClose,
    )
}
