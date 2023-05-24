package us.huseli.retain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.R
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.viewmodels.BaseEditNoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun BaseNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: BaseEditNoteViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    reorderableState: ReorderableLazyListState? = null,
    onTitleFieldNext: (() -> Unit)?,
    onBackClick: () -> Unit,
    onBackgroundClick: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    contextMenu: @Composable (() -> Unit)? = null,
    content: LazyListScope.(Color) -> Unit,
) {
    val bitmapImages by viewModel.bitmapImages.collectAsStateWithLifecycle(emptyList())
    val colorIdx by viewModel.colorIdx.collectAsStateWithLifecycle(0)
    val context = LocalContext.current
    val currentCarouselImage by viewModel.currentCarouselImage.collectAsStateWithLifecycle(null)
    val interactionSource = remember { MutableInteractionSource() }
    val title by viewModel.title.collectAsStateWithLifecycle("")
    val trashedBitmapImages by viewModel.trashedBitmapImages.collectAsStateWithLifecycle()
    val noteColor by viewModel.noteColor.collectAsStateWithLifecycle(MaterialTheme.colorScheme.background)
    val appBarColor by viewModel.appBarColor.collectAsStateWithLifecycle(MaterialTheme.colorScheme.surface)

    LaunchedEffect(colorIdx) {
        if (colorIdx > 0) {
            settingsViewModel.setSystemBarColors(
                statusBar = appBarColor,
                navigationBar = noteColor
            )
        }
    }

    LaunchedEffect(trashedBitmapImages) {
        if (trashedBitmapImages.isNotEmpty()) {
            val message = context.resources.getQuantityString(
                R.plurals.x_images_trashed,
                trashedBitmapImages.size,
                trashedBitmapImages.size
            )
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.resources.getString(R.string.undo).uppercase(),
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoTrashBitmapImages()
                SnackbarResult.Dismissed -> viewModel.clearTrashBitmapImages()
            }
        }
    }

    RetainScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            NoteScreenTopAppBar(
                backgroundColor = appBarColor,
                onBackClick = onBackClick,
                onImagePick = { uri -> viewModel.insertImage(uri) },
                onColorSelected = { index -> viewModel.setColorIdx(index) }
            )
        },
    ) { innerPadding ->
        var columnModifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(noteColor)
            .clickable(interactionSource = interactionSource, indication = null) {
                onBackgroundClick?.invoke()
            }
        if (reorderableState != null) columnModifier = columnModifier.reorderable(reorderableState)

        LazyColumn(
            state = reorderableState?.listState ?: rememberLazyListState(),
            modifier = columnModifier,
        ) {
            item {
                NoteImageGrid(
                    bitmapImages = bitmapImages,
                    showDeleteButton = true,
                    onImageClick = { viewModel.setCarouselImage(it.image) },
                    onDeleteButtonClick = { viewModel.deleteImage(it) },
                    secondaryRowHeight = 200.dp,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoteTitleField(
                        modifier = Modifier.weight(1f),
                        value = title,
                        onValueChange = { viewModel.setTitle(it) },
                        onNext = onTitleFieldNext,
                    )
                    contextMenu?.invoke()
                }
                Spacer(Modifier.height(4.dp))
            }
            content(noteColor)
        }

        currentCarouselImage?.let {
            ImageCarousel(
                bitmapImage = it,
                multiple = bitmapImages.size > 1,
                onClose = {
                    viewModel.unsetCarouselImage()
                    onBackClick()
                },
                onSwipeLeft = { viewModel.gotoNextCarouselImage() },
                onSwipeRight = { viewModel.gotoPreviousCarouselImage() },
            )
        }
    }
}


@Composable
fun NoteTitleField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onNext: (() -> Unit)? = null,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { onValueChange(it) },
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        keyboardActions = KeyboardActions(
            onNext = onNext?.let { { onNext() } }
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        },
        singleLine = true,
        colors = outlinedTextFieldColors(),
    )
}
