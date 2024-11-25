package us.huseli.retain.compose.settings.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_SFTP_BASE_DIR
import us.huseli.retain.Constants.PREF_SFTP_HOSTNAME
import us.huseli.retain.Constants.PREF_SFTP_PASSWORD
import us.huseli.retain.Constants.PREF_SFTP_PORT
import us.huseli.retain.Constants.PREF_SFTP_USERNAME
import us.huseli.retain.R
import us.huseli.retain.compose.SweepLoadingOverlay
import us.huseli.retain.compose.settings.SuccessIcon
import us.huseli.retain.syncbackend.tasks.result.TestTaskResult
import us.huseli.retain.viewmodels.SFTPViewModel

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
fun SFTPSection(modifier: Modifier = Modifier, viewModel: SFTPViewModel = hiltViewModel()) {
    val baseDir by viewModel.baseDir.collectAsStateWithLifecycle()
    val hostname by viewModel.hostname.collectAsStateWithLifecycle()
    val port by viewModel.port.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val promptYesNo by viewModel.promptYesNo.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    var isPasswordShown by rememberSaveable { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestTaskResult?>(null) }
    var isPasswordFieldFocused by rememberSaveable { mutableStateOf(false) }

    promptYesNo?.let {
        PromptYesNoDialog(
            title = "SFTP",
            message = it,
            onYes = { viewModel.approveKey() },
            onNo = { viewModel.denyKey() },
        )
    }

    Box(modifier = modifier) {
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
                        if (isWorking == true) SuccessIcon()
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
                        if (isWorking == true) SuccessIcon()
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
                        if (isWorking == true) SuccessIcon()
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
                        else if (isWorking == true) SuccessIcon()
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
                        if (isWorking == true) SuccessIcon()
                    },
                )
            }
        }
        if (isTesting) SweepLoadingOverlay(modifier = Modifier.matchParentSize())
    }
    Row {
        OutlinedButton(
            onClick = { viewModel.test { result -> testResult = result } },
            shape = ShapeDefaults.ExtraSmall,
            enabled = !isTesting && hostname.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty(),
        ) {
            Text(text = if (isTesting) stringResource(R.string.testing) else stringResource(R.string.test_connection))
        }
    }
}
