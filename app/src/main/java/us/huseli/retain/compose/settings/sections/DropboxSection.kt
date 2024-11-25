package us.huseli.retain.compose.settings.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.compose.SweepLoadingOverlay
import us.huseli.retain.compose.settings.ErrorDialog
import us.huseli.retain.compose.settings.FailIcon
import us.huseli.retain.compose.settings.SuccessIcon
import us.huseli.retain.syncbackend.tasks.result.TestTaskResult
import us.huseli.retain.viewmodels.DropboxViewModel

@Composable
fun DropboxSection(modifier: Modifier = Modifier, viewModel: DropboxViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val accountEmail by viewModel.accountEmail.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle(false)
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    var testResult by remember { mutableStateOf<TestTaskResult?>(null) }

    testResult?.let { result ->
        if (!result.success) {
            ErrorDialog(
                title = stringResource(R.string.dropbox_error),
                text = result.getErrorMessage(context),
                onClose = { testResult = null }
            )
        }
    }

    Box(modifier = modifier) {
        Column {
            if (isAuthenticated) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuccessIcon(modifier = Modifier.padding(start = 8.dp), circled = true)
                    Text(
                        text = stringResource(R.string.connected_to_dropbox_account, accountEmail),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWorking == true) {
                        SuccessIcon(modifier = Modifier.padding(start = 8.dp), circled = true)
                        Text(
                            text = stringResource(R.string.the_dropbox_connection_is_working),
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else if (isWorking == false) {
                        FailIcon(modifier = Modifier.padding(start = 8.dp), circled = true)
                        Text(
                            text = stringResource(R.string.the_dropbox_connection_is_not_working_somehow),
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.revoke() },
                        shape = ShapeDefaults.ExtraSmall,
                    ) {
                        Text(stringResource(R.string.revoke))
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    FailIcon(modifier = Modifier.padding(start = 8.dp), circled = true)
                    Text(
                        text = stringResource(R.string.not_connected_to_dropbox),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.authenticate() },
                        shape = ShapeDefaults.ExtraSmall,
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                }
            }
        }
        if (isTesting) SweepLoadingOverlay(modifier = Modifier.matchParentSize())
    }
    Row {
        OutlinedButton(
            onClick = { viewModel.test { result -> testResult = result } },
            shape = ShapeDefaults.ExtraSmall,
            enabled = !isTesting,
        ) {
            Text(text = if (isTesting) stringResource(R.string.testing) else stringResource(R.string.test_connection))
        }
    }
}
