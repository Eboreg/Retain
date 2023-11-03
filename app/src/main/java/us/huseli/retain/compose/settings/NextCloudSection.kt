package us.huseli.retain.compose.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Visibility
import androidx.compose.material.icons.sharp.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.R
import us.huseli.retain.cleanUri
import us.huseli.retain.compose.SweepLoadingOverlay
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import us.huseli.retain.viewmodels.NextCloudViewModel

@Composable
fun NextCloudSection(
    modifier: Modifier = Modifier,
    viewModel: NextCloudViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val uri by viewModel.uri.collectAsStateWithLifecycle("")
    val username by viewModel.username.collectAsStateWithLifecycle("")
    val password by viewModel.password.collectAsStateWithLifecycle("")
    val baseDir by viewModel.baseDir.collectAsStateWithLifecycle("")
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
    val isUrlError by viewModel.isUrlError.collectAsStateWithLifecycle()
    val isAuthError by viewModel.isAuthError.collectAsStateWithLifecycle()
    val successMessage = stringResource(R.string.successfully_connected_to_nextcloud)

    var testResult by remember { mutableStateOf<TestTaskResult?>(null) }
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

    BoxWithConstraints(modifier = modifier) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier
                        .onFocusChanged {
                            if (!it.isFocused) {
                                if (uriState.isNotEmpty()) {
                                    uriState = uriState.cleanUri()
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
                        if (isWorking == true) SuccessIcon()
                        else if (isUrlError) FailIcon()
                    },
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
                        if (isWorking == true) SuccessIcon()
                        else if (isAuthError) FailIcon()
                    },
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
                        else if (isAuthError) FailIcon()
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
                    trailingIcon = { if (isWorking == true) SuccessIcon() },
                )
            }
        }
        if (isTesting) SweepLoadingOverlay(modifier = Modifier.matchParentSize())
    }
    Row {
        OutlinedButton(
            onClick = { viewModel.test { result -> testResult = result } },
            shape = ShapeDefaults.ExtraSmall,
            enabled = !isTesting && uriState.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty(),
        ) {
            Text(text = if (isTesting) stringResource(R.string.testing) else stringResource(R.string.test_connection))
        }
    }
}
