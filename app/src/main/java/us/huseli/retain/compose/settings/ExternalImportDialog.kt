package us.huseli.retain.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retain.R

@Composable
fun ExternalImportDialog(
    modifier: Modifier = Modifier,
    actionCount: Int?,
    currentAction: String,
    currentActionIndex: Int,
    serviceName: String,
    onCancel: () -> Unit
) {
    val progressModifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
    AlertDialog(
        modifier = modifier,
        title = { Text(stringResource(R.string.external_import, serviceName)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.importing_dont_go_anywhere, serviceName),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                if (actionCount != null) {
                    LinearProgressIndicator(
                        modifier = progressModifier,
                        progress = { if (actionCount > 0) (currentActionIndex.toFloat() / actionCount) else 0f },
                    )
                } else {
                    LinearProgressIndicator(modifier = progressModifier)
                }
                Text(currentAction, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
        onDismissRequest = {},
        confirmButton = {},
    )
}
