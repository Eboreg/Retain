package us.huseli.retain.compose.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.retain.R

@Composable
fun ErrorDialog(
    modifier: Modifier = Modifier,
    title: String,
    text: String,
    onClose: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(title) },
        text = { Text(text) },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close).uppercase())
            }
        },
        onDismissRequest = onClose,
        confirmButton = {},
    )
}