package us.huseli.retain.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_SYNC_BACKEND
import us.huseli.retain.Enums.SyncBackend
import us.huseli.retain.R
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun SyncBackendRadioButton(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable { onClick() }
        )
    }
}

@Composable
fun SyncBackendSection(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val syncBackend by viewModel.syncBackend.collectAsStateWithLifecycle()

    BaseSettingsSection(
        modifier = modifier,
        title = stringResource(R.string.backend_sync),
        key = "syncBackend",
        viewModel = viewModel,
    ) {
        SyncBackendRadioButton(
            text = stringResource(R.string.do_not_sync),
            selected = syncBackend == SyncBackend.NONE,
            onClick = { viewModel.updateField(PREF_SYNC_BACKEND, SyncBackend.NONE) },
        )
        SyncBackendRadioButton(
            text = stringResource(R.string.sync_with_nextcloud),
            selected = syncBackend == SyncBackend.NEXTCLOUD,
            onClick = { viewModel.updateField(PREF_SYNC_BACKEND, SyncBackend.NEXTCLOUD) },
        )
        SyncBackendRadioButton(
            text = stringResource(R.string.sync_with_sftp),
            selected = syncBackend == SyncBackend.SFTP,
            onClick = { viewModel.updateField(PREF_SYNC_BACKEND, SyncBackend.SFTP) },
        )

        if (syncBackend == SyncBackend.NEXTCLOUD) {
            NextCloudSection(viewModel = viewModel, snackbarHostState = snackbarHostState)
        }
        if (syncBackend == SyncBackend.SFTP) {
            SFTPSection(viewModel = viewModel)
        }
    }
}
