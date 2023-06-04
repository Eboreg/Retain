package us.huseli.retain.compose.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Error
import androidx.compose.material.icons.sharp.Visibility
import androidx.compose.material.icons.sharp.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_ENABLED
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.R
import us.huseli.retain.cleanUri
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.ui.theme.RetainColorDark
import us.huseli.retain.ui.theme.RetainColorLight
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun NextCloudSection(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val uri by viewModel.nextCloudUri.collectAsStateWithLifecycle("")
    val username by viewModel.nextCloudUsername.collectAsStateWithLifecycle("")
    val password by viewModel.nextCloudPassword.collectAsStateWithLifecycle("")
    val baseDir by viewModel.nextCloudBaseDir.collectAsStateWithLifecycle("")
    val isTesting by viewModel.isNextCloudTesting.collectAsStateWithLifecycle()
    val isWorking by viewModel.isNextCloudWorking.collectAsStateWithLifecycle()
    val isUrlFail by viewModel.isNextCloudUrlFail.collectAsStateWithLifecycle()
    val isCredentialsFail by viewModel.isNextCloudCredentialsFail.collectAsStateWithLifecycle()
    val isEnabled by viewModel.isNextCloudEnabled.collectAsStateWithLifecycle()
    val successMessage = stringResource(R.string.successfully_connected_to_nextcloud)

    var testResult by remember { mutableStateOf<TestNextCloudTaskResult?>(null) }
    var uriState by rememberSaveable(uri) { mutableStateOf(uri) }
    var isPasswordFieldFocused by rememberSaveable { mutableStateOf(false) }
    var isPasswordShown by rememberSaveable { mutableStateOf(false) }

    testResult?.let { result ->
        if (result.success) {
            LaunchedEffect(result) {
                snackbarHostState.showSnackbar(successMessage)
            }
        } else {
            ErrorDialog(
                title = stringResource(R.string.nextcloud_error),
                text = result.getErrorMessage(context),
                onClose = { testResult = null }
            )
        }
    }

    val colors = if (isSystemInDarkTheme()) RetainColorDark else RetainColorLight
    val workingIcon = @Composable {
        Icon(
            imageVector = Icons.Sharp.Check,
            contentDescription = null,
            tint = colors.Green,
        )
    }
    val failIcon = @Composable {
        Icon(
            imageVector = Icons.Sharp.Error,
            contentDescription = null,
            tint = colors.Red,
        )
    }

    BaseSettingsSection(
        modifier = modifier,
        title = stringResource(R.string.nextcloud_sync),
        key = "nextcloud",
        viewModel = viewModel,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { viewModel.updateField(PREF_NEXTCLOUD_ENABLED, it) }
            )
            Text(
                text = stringResource(R.string.enable_nextcloud_sync),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isEnabled) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged {
                            if (!it.isFocused) {
                                if (uriState.isNotEmpty()) {
                                    uriState = cleanUri(uriState)
                                    viewModel.updateField(PREF_NEXTCLOUD_URI, uriState)
                                }
                            }
                        }
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.nextcloud_uri)) },
                    singleLine = true,
                    value = uriState,
                    onValueChange = {
                        uriState = it
                        viewModel.updateField(PREF_NEXTCLOUD_URI, it)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Uri
                    ),
                    enabled = !isTesting,
                    trailingIcon = {
                        if (isWorking == true) workingIcon()
                        else if (isUrlFail) failIcon()
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.nextcloud_username)) },
                    singleLine = true,
                    value = username,
                    enabled = !isTesting,
                    onValueChange = { viewModel.updateField(PREF_NEXTCLOUD_USERNAME, it) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (isWorking == true) workingIcon()
                        else if (isCredentialsFail) failIcon()
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isPasswordFieldFocused = it.isFocused },
                    label = { Text(stringResource(R.string.nextcloud_password)) },
                    singleLine = true,
                    value = password,
                    onValueChange = { viewModel.updateField(PREF_NEXTCLOUD_PASSWORD, it) },
                    visualTransformation = if (isPasswordShown) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isTesting,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (isPasswordFieldFocused && isPasswordShown)
                            IconButton(onClick = { isPasswordShown = false }) {
                                Icon(
                                    imageVector = Icons.Sharp.VisibilityOff,
                                    contentDescription = stringResource(R.string.hide_password),
                                )
                            }
                        else if (isPasswordFieldFocused)
                            IconButton(onClick = { isPasswordShown = true }) {
                                Icon(
                                    imageVector = Icons.Sharp.Visibility,
                                    contentDescription = stringResource(R.string.show_password),
                                )
                            }
                        else if (isWorking == true) workingIcon()
                        else if (isCredentialsFail) failIcon()
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = baseDir,
                    label = { Text(stringResource(R.string.nextcloud_base_path)) },
                    singleLine = true,
                    onValueChange = { viewModel.updateField(PREF_NEXTCLOUD_BASE_DIR, it) },
                    enabled = !isTesting,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    trailingIcon = { if (isWorking == true) workingIcon() },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.testNextCloud { result -> testResult = result }
                    },
                    shape = ShapeDefaults.ExtraSmall,
                    enabled = !isTesting && uriState.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty(),
                ) {
                    Text(
                        text = if (isTesting) stringResource(R.string.testing) else stringResource(R.string.test_connection),
                    )
                }
                if (isTesting) {
                    LinearProgressIndicator()
                }
            }
        }
    }
}