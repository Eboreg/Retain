package us.huseli.retain.compose.notescreen.bars

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.AddAPhoto
import androidx.compose.material.icons.sharp.AddPhotoAlternate
import androidx.compose.material.icons.sharp.Palette
import androidx.compose.material.icons.sharp.TextFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import us.huseli.retain.R
import us.huseli.retain.compose.ColorDropdownMenu
import us.huseli.retain.ui.theme.NoteColorKey
import us.huseli.retain.ui.theme.getNoteColors
import us.huseli.retain.viewmodels.AbstractNoteViewModel
import java.io.File
import java.util.UUID

@Composable
fun NoteScreenTopAppBar(
    viewModel: AbstractNoteViewModel<*>,
    backgroundColor: Color,
    isTextFormatEnabled: Boolean,
    onBackClick: () -> Unit,
    onTextFormatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoteScreenTopAppBar(
        modifier = modifier,
        backgroundColor = backgroundColor,
        onBackClick = {
            viewModel.save()
            onBackClick()
        },
        onImagePick = { uri -> viewModel.insertImage(uri) },
        onColorSelected = { key -> viewModel.setNoteColor(key) },
        onTextFormatClick = onTextFormatClick,
        isTextFormatEnabled = isTextFormatEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreenTopAppBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    isTextFormatEnabled: Boolean,
    onBackClick: () -> Unit,
    onImagePick: (Uri) -> Unit,
    onColorSelected: (NoteColorKey) -> Unit,
    onTextFormatClick: () -> Unit,
) {
    val context = LocalContext.current
    var isColorDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    val photoTempDir = File(context.cacheDir, "photos").apply { mkdirs() }
    val photoTempFile = File(photoTempDir, "${UUID.randomUUID()}.png")
    val photoUri by remember {
        mutableStateOf(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoTempFile))
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePick(uri)
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { result ->
        if (result) onImagePick(photoUri)
    }

    TopAppBar(
        title = {},
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Sharp.ArrowBack,
                    contentDescription = stringResource(R.string.go_back),
                )
            }
        },
        actions = {
            IconButton(onClick = { isColorDropdownExpanded = true }) {
                Icon(
                    imageVector = Icons.Sharp.Palette,
                    contentDescription = stringResource(R.string.set_colour),
                )
            }
            ColorDropdownMenu(
                colors = getNoteColors(),
                isExpanded = isColorDropdownExpanded,
                onDismiss = { isColorDropdownExpanded = false },
                onColorClick = {
                    isColorDropdownExpanded = false
                    onColorSelected(it)
                },
            )
            IconButton(onClick = { photoLauncher.launch(photoUri) }) {
                Icon(
                    imageVector = Icons.Sharp.AddAPhoto,
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                    contentDescription = stringResource(R.string.take_picture)
                )
            }
            IconButton(
                onClick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Icon(
                    imageVector = Icons.Sharp.AddPhotoAlternate,
                    modifier = Modifier.scale(1.1f),
                    contentDescription = stringResource(R.string.add_image)
                )
            }
            IconButton(onClick = onTextFormatClick, enabled = isTextFormatEnabled) {
                Icon(
                    imageVector = Icons.Sharp.TextFormat,
                    contentDescription = stringResource(R.string.text_format),
                    modifier = Modifier.scale(1.2f),
                )
            }
        }
    )
}
