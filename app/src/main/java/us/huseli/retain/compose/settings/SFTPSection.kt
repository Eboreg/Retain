package us.huseli.retain.compose.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Visibility
import androidx.compose.material.icons.sharp.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_SFTP_BASE_DIR
import us.huseli.retain.Constants.PREF_SFTP_HOSTNAME
import us.huseli.retain.Constants.PREF_SFTP_PASSWORD
import us.huseli.retain.Constants.PREF_SFTP_PORT
import us.huseli.retain.Constants.PREF_SFTP_USERNAME
import us.huseli.retain.R
import us.huseli.retain.compose.SweepLoadingOverlay
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import us.huseli.retain.ui.theme.RetainColorDark
import us.huseli.retain.ui.theme.RetainColorLight
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun PromptYesNoDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = {
            TextButton(onClick = onNo) {
                Text(stringResource(R.string.no).uppercase())
            }
        },
        confirmButton = {
            TextButton(onClick = onYes) {
                Text(stringResource(R.string.yes).uppercase())
            }
        },
        onDismissRequest = {}
    )
}

@Composable
fun SFTPSection(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val baseDir by viewModel.sftpBaseDir.collectAsStateWithLifecycle()
    val hostname by viewModel.sftpHostname.collectAsStateWithLifecycle()
    val port by viewModel.sftpPort.collectAsStateWithLifecycle()
    val username by viewModel.sftpUsername.collectAsStateWithLifecycle()
    val password by viewModel.sftpPassword.collectAsStateWithLifecycle()
    val promptYesNo by viewModel.sftpPromptYesNo.collectAsStateWithLifecycle()
    val isTesting by viewModel.isSFTPTesting.collectAsStateWithLifecycle()
    val isWorking by viewModel.isSFTPWorking.collectAsStateWithLifecycle()
    var isPasswordShown by rememberSaveable { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestTaskResult?>(null) }
    val colors = if (isSystemInDarkTheme()) RetainColorDark else RetainColorLight
    var isPasswordFieldFocused by rememberSaveable { mutableStateOf(false) }
    val workingIcon = @Composable {
        Icon(
            imageVector = Icons.Sharp.Check,
            contentDescription = null,
            tint = colors.Green,
        )
    }

    promptYesNo?.let {
        PromptYesNoDialog(
            title = "SFTP",
            message = it,
            onYes = { viewModel.approveSFTPKey() },
            onNo = { viewModel.denySFTPKey() },
        )
    }

    BoxWithConstraints(modifier = modifier) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.padding(end = 4.dp),
                    label = { Text(stringResource(R.string.sftp_hostname)) },
                    singleLine = true,
                    value = hostname,
                    onValueChange = { viewModel.updateField(PREF_SFTP_HOSTNAME, it) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Uri,
                    ),
                    enabled = !isTesting,
                    trailingIcon = {
                        if (isWorking == true) workingIcon()
                    },
                )
                OutlinedTextField(
                    label = { Text(stringResource(R.string.sftp_port)) },
                    singleLine = true,
                    value = port.toString(),
                    onValueChange = { viewModel.updateField(PREF_SFTP_PORT, it.toInt()) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number,
                    ),
                    enabled = !isTesting,
                    trailingIcon = {
                        if (isWorking == true) workingIcon()
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sftp_username)) },
                    singleLine = true,
                    value = username,
                    onValueChange = { viewModel.updateField(PREF_SFTP_USERNAME, it) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    enabled = !isTesting,
                    trailingIcon = {
                        if (isWorking == true) workingIcon()
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isPasswordFieldFocused = it.isFocused },
                    label = { Text(stringResource(R.string.sftp_password)) },
                    singleLine = true,
                    value = password,
                    onValueChange = { viewModel.updateField(PREF_SFTP_PASSWORD, it) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    visualTransformation =
                    if (isPasswordShown) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        if (isPasswordFieldFocused)
                            IconButton(onClick = { isPasswordShown = !isPasswordShown }) {
                                Icon(
                                    imageVector = if (isPasswordShown) Icons.Sharp.VisibilityOff else Icons.Sharp.Visibility,
                                    contentDescription =
                                    if (isPasswordShown) stringResource(R.string.hide_password)
                                    else stringResource(R.string.show_password),
                                )
                            }
                        else if (isWorking == true) workingIcon()
                    },
                    enabled = !isTesting,
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sftp_base_directory)) },
                    singleLine = true,
                    value = baseDir,
                    onValueChange = { viewModel.updateField(PREF_SFTP_BASE_DIR, it) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    supportingText = { Text(stringResource(R.string.relative_to_users_home_directory)) },
                    enabled = !isTesting,
                    trailingIcon = {
                        if (isWorking == true) workingIcon()
                    },
                )
            }
        }
        if (isTesting) SweepLoadingOverlay(modifier = Modifier.matchParentSize())
    }
    Row {
        OutlinedButton(
            onClick = { viewModel.testSFTP { result -> testResult = result } },
            shape = ShapeDefaults.ExtraSmall,
            enabled = !isTesting && hostname.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty(),
        ) {
            Text(text = if (isTesting) stringResource(R.string.testing) else stringResource(R.string.test_connection))
        }
    }
}
