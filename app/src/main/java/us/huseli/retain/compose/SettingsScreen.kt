package us.huseli.retain.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.R
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.SettingsViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    var show by rememberSaveable { mutableStateOf(true) }
    val showIconRotation by animateFloatAsState(if (show) 0f else 180f)

    Column(modifier = modifier.padding(bottom = 16.dp).padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            IconButton(onClick = { show = !show }) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(showIconRotation),
                )
            }
        }
        if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        if (show) content()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextCloudSection(
    modifier: Modifier = Modifier,
    uri: String,
    username: String,
    password: String,
    onStringChange: (String, String) -> Unit,
    onFocusChange: (String, Boolean) -> Unit,
) {
    SettingsSection(
        modifier = modifier,
        title = stringResource(R.string.nextcloud)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .onFocusChanged { onFocusChange(PREF_NEXTCLOUD_URI, it.isFocused) }
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.nextcloud_uri)) },
            singleLine = true,
            value = uri,
            onValueChange = { onStringChange(PREF_NEXTCLOUD_URI, it) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            modifier = Modifier
                .onFocusChanged { onFocusChange(PREF_NEXTCLOUD_USERNAME, it.isFocused) }
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.nextcloud_username)) },
            singleLine = true,
            value = username,
            onValueChange = { onStringChange(PREF_NEXTCLOUD_USERNAME, it) }
        )
        OutlinedTextField(
            modifier = Modifier
                .onFocusChanged { onFocusChange(PREF_NEXTCLOUD_PASSWORD, it.isFocused) }
                .fillMaxWidth(),
            label = { Text(stringResource(R.string.nextcloud_password)) },
            singleLine = true,
            value = password,
            onValueChange = { onStringChange(PREF_NEXTCLOUD_PASSWORD, it) },
            visualTransformation = PasswordVisualTransformation(),
        )
    }
}


@Composable
fun GeneralSection(
    modifier: Modifier = Modifier,
    minColumnWidth: Int,
    onIntChange: (String, Int) -> Unit,
) {
    var minColumnWidthSliderPos by remember { mutableStateOf(minColumnWidth) }
    val maxScreenWidth = max(LocalConfiguration.current.screenHeightDp, LocalConfiguration.current.screenWidthDp)
    val maxColumnWidth = (maxScreenWidth - 24) / 10 * 10
    val columnWidthSteps = (maxColumnWidth - 51) / 10

    SettingsSection(
        modifier = modifier,
        title = stringResource(R.string.general)
    ) {
        Text(
            text = stringResource(R.string.minimum_column_width_on_home_screen),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                value = minColumnWidthSliderPos.toFloat(),
                steps = columnWidthSteps,
                onValueChange = { minColumnWidthSliderPos = it.roundToInt() },
                onValueChangeFinished = { onIntChange(PREF_MIN_COLUMN_WIDTH, minColumnWidthSliderPos) },
                valueRange = 50f..maxColumnWidth.toFloat(),
            )
            Text(
                text = minColumnWidthSliderPos.toString(),
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopAppBar(
    modifier: Modifier = Modifier,
    onSave: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(R.string.settings))
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.save_settings),
                )
            }
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenImpl(
    modifier: Modifier = Modifier,
    nextCloudUri: String,
    nextCloudUsername: String,
    nextCloudPassword: String,
    minColumnWidth: Int,
    snackbarHostState: SnackbarHostState,
    onStringChange: (String, String) -> Unit = { _, _ -> },
    onIntChange: (String, Int) -> Unit = { _, _ -> },
    onFocusChange: (String, Boolean) -> Unit = { _, _ -> },
    onSave: () -> Unit = {},
) {
    Scaffold(
        topBar = { SettingsTopAppBar(onSave = onSave) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(modifier = modifier.padding(innerPadding)) {
            if (maxWidth > 600.dp) {
                val columnWidth = maxWidth / 2

                Row {
                    Column(modifier = Modifier.width(columnWidth)) {
                        GeneralSection(
                            minColumnWidth = minColumnWidth,
                            onIntChange = onIntChange,
                        )
                    }
                    Column {
                        NextCloudSection(
                            uri = nextCloudUri,
                            username = nextCloudUsername,
                            password = nextCloudPassword,
                            onStringChange = onStringChange,
                            onFocusChange = onFocusChange,
                        )
                    }
                }
            } else {
                Column {
                    GeneralSection(
                        minColumnWidth = minColumnWidth,
                        onIntChange = onIntChange,
                    )
                    NextCloudSection(
                        uri = nextCloudUri,
                        username = nextCloudUsername,
                        password = nextCloudPassword,
                        onStringChange = onStringChange,
                        onFocusChange = onFocusChange,
                    )
                }
            }
        }
    }
}


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
) {
    val nextCloudUri by viewModel.nextCloudUri.collectAsStateWithLifecycle("")
    val nextCloudUsername by viewModel.nextCloudUsername.collectAsStateWithLifecycle("")
    val nextCloudPassword by viewModel.nextCloudPassword.collectAsStateWithLifecycle("")
    val minColumnWidth by viewModel.minColumnWidth.collectAsStateWithLifecycle()

    SettingsScreenImpl(
        modifier = modifier,
        nextCloudUri = nextCloudUri,
        nextCloudUsername = nextCloudUsername,
        nextCloudPassword = nextCloudPassword,
        minColumnWidth = minColumnWidth,
        snackbarHostState = snackbarHostState,
        onStringChange = viewModel.setString,
        onIntChange = viewModel.setInt,
        onFocusChange = { field, value -> if (!value) viewModel.save(field) },
        onSave = {
            viewModel.saveAll()
            onClose()
        },
    )
}


@Composable
@Preview(showBackground = true, showSystemUi = true)
fun SettingsScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    RetainTheme {
        SettingsScreenImpl(
            nextCloudUri = "https://apan.ap",
            nextCloudUsername = "apan",
            nextCloudPassword = "apeliapanap",
            minColumnWidth = 180,
            snackbarHostState = snackbarHostState,
        )
    }
}