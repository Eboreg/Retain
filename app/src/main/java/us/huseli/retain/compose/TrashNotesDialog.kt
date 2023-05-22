package us.huseli.retain.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R
import us.huseli.retain.data.entities.Note

@Composable
fun TrashNotesDialog(
    modifier: Modifier = Modifier,
    selectedNotes: Collection<Note>,
    onTrash: (Collection<Note>) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(pluralStringResource(R.plurals.send_notes_to_trash, selectedNotes.size)) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.send_x_notes_to_trash,
                    selectedNotes.size,
                    selectedNotes.size
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTrash(selectedNotes)
                    onClose()
                }
            ) {
                Text(stringResource(R.string.send_to_trash).uppercase())
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
