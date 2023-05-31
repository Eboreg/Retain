package us.huseli.retain.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Error
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material.icons.sharp.Visibility
import androidx.compose.material.icons.sharp.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Constants.NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.R
import us.huseli.retain.cleanUri
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import us.huseli.retain.ui.theme.RetainColorDark
import us.huseli.retain.ui.theme.RetainColorLight
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.SettingsViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
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
                    imageVector = Icons.Sharp.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(showIconRotation),
                )
            }
        }
        if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        if (show) content()
    }
}


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


@Composable
fun KeepImportDialog(
    modifier: Modifier = Modifier,
    actionCount: Int,
    currentAction: String,
    currentActionIndex: Int,
    onCancel: () -> Unit
) {
    val progress = if (actionCount > 0) (currentActionIndex.toFloat() / actionCount) else 0f

    AlertDialog(
        modifier = modifier,
        title = { Text(stringResource(R.string.google_keep_import)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.importing_from_google_keep_don_t_go_anywhere),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), progress = progress)
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

@Composable
fun KeepImportSection(modifier: Modifier = Modifier, onZipFilePick: (Uri) -> Unit) {
    val zipFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onZipFilePick(uri)
    }

    SettingsSection(
        modifier = modifier,
        title = stringResource(R.string.google_keep_import)
    ) {
        Text(stringResource(R.string.google_keep_import_about), modifier = Modifier.padding(bottom = 8.dp))
        OutlinedButton(
            onClick = { zipFilePicker.launch(arrayOf("application/zip")) },
            shape = ShapeDefaults.ExtraSmall,
        ) {
            Text(stringResource(R.string.select_zip_file))
        }
    }
}


@Composable
fun NextCloudSection(
    modifier: Modifier = Modifier,
    uri: String,
    username: String,
    password: String,
    baseDir: String,
    isTesting: Boolean,
    isWorking: Boolean?,
    isUrlFail: Boolean,
    isCredentialsFail: Boolean,
    onTestClick: () -> Unit,
    onChange: (field: String, value: Any) -> Unit,
) {
    var uriState by rememberSaveable(uri) { mutableStateOf(uri) }
    var isPasswordFieldFocused by rememberSaveable { mutableStateOf(false) }
    var isPasswordShown by rememberSaveable { mutableStateOf(false) }
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

    SettingsSection(
        modifier = modifier,
        title = stringResource(R.string.nextcloud_sync)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier
                    .onFocusChanged {
                        if (!it.isFocused) {
                            if (uriState.isNotEmpty()) {
                                uriState = cleanUri(uriState)
                                onChange(PREF_NEXTCLOUD_URI, uriState)
                            }
                        }
                    }
                    .fillMaxWidth(),
                label = { Text(stringResource(R.string.nextcloud_uri)) },
                singleLine = true,
                value = uriState,
                onValueChange = {
                    uriState = it
                    onChange(PREF_NEXTCLOUD_URI, it)
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
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.nextcloud_username)) },
                singleLine = true,
                value = username,
                enabled = !isTesting,
                onValueChange = { onChange(PREF_NEXTCLOUD_USERNAME, it) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                trailingIcon = {
                    if (isWorking == true) workingIcon()
                    else if (isCredentialsFail) failIcon()
                }
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isPasswordFieldFocused = it.isFocused },
                label = { Text(stringResource(R.string.nextcloud_password)) },
                singleLine = true,
                value = password,
                onValueChange = { onChange(PREF_NEXTCLOUD_PASSWORD, it) },
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
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = baseDir,
                label = { Text(stringResource(R.string.nextcloud_base_path)) },
                singleLine = true,
                onValueChange = { onChange(PREF_NEXTCLOUD_BASE_DIR, it) },
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
                onClick = onTestClick,
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


@Composable
fun GeneralSection(
    modifier: Modifier = Modifier,
    minColumnWidth: Int,
    onChange: (field: String, value: Any) -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
                value = minColumnWidthSliderPos.toFloat(),
                steps = columnWidthSteps,
                onValueChange = { minColumnWidthSliderPos = it.roundToInt() },
                onValueChangeFinished = { onChange(PREF_MIN_COLUMN_WIDTH, minColumnWidthSliderPos) },
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
    onBackClick: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(R.string.settings))
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Sharp.ArrowBack,
                    contentDescription = stringResource(R.string.close)
                )
            }
        },
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreenImpl(
    modifier: Modifier = Modifier,
    nextCloudUri: String,
    nextCloudUsername: String,
    nextCloudPassword: String,
    nextCloudBaseDir: String,
    minColumnWidth: Int,
    isNextCloudTesting: Boolean = false,
    isNextCloudWorking: Boolean? = null,
    isNextCloudUrlFail: Boolean = false,
    isNextCloudCredentialsFail: Boolean = false,
    onSave: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNextCloudTestClick: () -> Unit = {},
    onChange: (field: String, value: Any) -> Unit = { _, _ -> },
    onKeepZipFilePick: (Uri) -> Unit = {},
) {
    DisposableEffect(Unit) {
        onDispose {
            onSave()
        }
    }

    RetainScaffold(
        topBar = { SettingsTopAppBar(onBackClick = onBackClick) },
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            columns = StaggeredGridCells.Adaptive(minSize = 400.dp),
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                GeneralSection(
                    modifier = Modifier.fillMaxWidth(),
                    minColumnWidth = minColumnWidth,
                    onChange = onChange,
                )
            }
            item {
                NextCloudSection(
                    modifier = Modifier.fillMaxWidth(),
                    uri = nextCloudUri,
                    username = nextCloudUsername,
                    password = nextCloudPassword,
                    baseDir = nextCloudBaseDir,
                    isTesting = isNextCloudTesting,
                    onTestClick = onNextCloudTestClick,
                    onChange = onChange,
                    isCredentialsFail = isNextCloudCredentialsFail,
                    isUrlFail = isNextCloudUrlFail,
                    isWorking = isNextCloudWorking,
                )
            }
            item {
                KeepImportSection(
                    modifier = Modifier.fillMaxWidth(),
                    onZipFilePick = onKeepZipFilePick,
                )
            }
        }
    }
}


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
) {
    val nextCloudUri by viewModel.nextCloudUri.collectAsStateWithLifecycle("")
    val nextCloudUsername by viewModel.nextCloudUsername.collectAsStateWithLifecycle("")
    val nextCloudPassword by viewModel.nextCloudPassword.collectAsStateWithLifecycle("")
    val nextCloudBaseDir by viewModel.nextCloudBaseDir.collectAsStateWithLifecycle("")
    val minColumnWidth by viewModel.minColumnWidth.collectAsStateWithLifecycle()
    val isNextCloudTesting by viewModel.isNextCloudTesting.collectAsStateWithLifecycle()
    val isNextCloudWorking by viewModel.isNextCloudWorking.collectAsStateWithLifecycle()
    val isNextCloudUrlFail by viewModel.isNextCloudUrlFail.collectAsStateWithLifecycle()
    val isNextCloudCredentialsFail by viewModel.isNextCloudCredentialsFail.collectAsStateWithLifecycle()
    val keepImportIsActive by viewModel.keepImportIsActive.collectAsStateWithLifecycle()
    val keepImportActionCount by viewModel.keepImportActionCount.collectAsStateWithLifecycle()
    val keepImportCurrentAction by viewModel.keepImportCurrentAction.collectAsStateWithLifecycle()
    val keepImportCurrentActionIndex by viewModel.keepImportCurrentActionIndex.collectAsStateWithLifecycle()
    val nextCloudSuccessMessage = stringResource(R.string.successfully_connected_to_nextcloud)
    val context = LocalContext.current

    var nextCloudTestResult by remember { mutableStateOf<TestNextCloudTaskResult?>(null) }

    nextCloudTestResult?.let { result ->
        if (result.success) {
            LaunchedEffect(result) {
                snackbarHostState.showSnackbar(nextCloudSuccessMessage)
            }
        } else {
            ErrorDialog(
                title = stringResource(R.string.nextcloud_error),
                text = result.getErrorMessage(context),
                onClose = { nextCloudTestResult = null }
            )
        }
    }

    if (keepImportIsActive) {
        KeepImportDialog(
            actionCount = keepImportActionCount,
            currentAction = keepImportCurrentAction,
            currentActionIndex = keepImportCurrentActionIndex,
            onCancel = { viewModel.cancelKeepImport() }
        )
    }

    SettingsScreenImpl(
        modifier = modifier,
        nextCloudUri = nextCloudUri,
        nextCloudUsername = nextCloudUsername,
        nextCloudPassword = nextCloudPassword,
        nextCloudBaseDir = nextCloudBaseDir,
        minColumnWidth = minColumnWidth,
        isNextCloudTesting = isNextCloudTesting,
        isNextCloudWorking = isNextCloudWorking,
        isNextCloudUrlFail = isNextCloudUrlFail,
        isNextCloudCredentialsFail = isNextCloudCredentialsFail,
        onSave = { viewModel.save() },
        onBackClick = onBackClick,
        onNextCloudTestClick = {
            viewModel.testNextCloud { result -> nextCloudTestResult = result }
        },
        onChange = { field, value -> viewModel.updateField(field, value) },
        onKeepZipFilePick = { uri -> viewModel.keepImport(uri, context) },
    )
}


@Composable
@Preview(showBackground = true, showSystemUi = true)
fun SettingsScreenPreview() {
    RetainTheme {
        SettingsScreenImpl(
            nextCloudUri = "https://apan.ap",
            nextCloudUsername = "apan",
            nextCloudPassword = "apeliapanap",
            nextCloudBaseDir = NEXTCLOUD_BASE_DIR,
            minColumnWidth = 180,
        )
    }
}
