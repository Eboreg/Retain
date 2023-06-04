package us.huseli.retain.compose.notescreen

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.R
import us.huseli.retain.compose.NoteImageGrid
import us.huseli.retain.compose.RetainScaffold
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.getAppBarColor
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.BaseEditNoteViewModel
import java.lang.Integer.max
import java.util.UUID

@Composable
fun BaseNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: BaseEditNoteViewModel,
    navController: NavHostController,
    note: Note,
    reorderableState: ReorderableLazyListState? = null,
    lazyListState: LazyListState = reorderableState?.listState ?: rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onTitleFieldNext: (() -> Unit)?,
    onBackClick: () -> Unit,
    onBackgroundClick: (() -> Unit)? = null,
    onSave: (Note?, List<ChecklistItem>, List<Image>, List<UUID>, List<String>) -> Unit,
    onImageCarouselStart: (UUID, String) -> Unit,
    contextMenu: @Composable (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsStateWithLifecycle()
    val trashedImageCount by viewModel.trashedImageCount.collectAsStateWithLifecycle(0)
    val noteColor by remember(note.color) { mutableStateOf(getNoteColor(context, note.color)) }
    val appBarColor by remember(note.color) { mutableStateOf(getAppBarColor(context, note.color)) }
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val imageAdded by viewModel.imageAdded.collectAsStateWithLifecycle(null)
    val isImageSelectEnabled = selectedImages.isNotEmpty()

    BackHandler(isImageSelectEnabled) {
        viewModel.deselectAllImages()
    }

    LaunchedEffect(imageAdded) {
        if (imageAdded != null) lazyListState.animateScrollToItem(max(images.size - 1, 0))
    }

    LaunchedEffect(trashedImageCount) {
        if (trashedImageCount > 0) {
            val message = context.resources.getQuantityString(
                R.plurals.x_images_trashed,
                trashedImageCount,
                trashedImageCount
            )
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.resources.getString(R.string.undo).uppercase(),
                duration = SnackbarDuration.Long,
                withDismissAction = true,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoTrashBitmapImages()
                SnackbarResult.Dismissed -> viewModel.clearTrashedImages()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSave(
                viewModel.dirtyNote,
                viewModel.dirtyChecklistItems,
                viewModel.dirtyImages,
                viewModel.deletedChecklistItemIds,
                viewModel.deletedImageIds
            )
        }
    }

    RetainScaffold(
        snackbarHostState = snackbarHostState,
        statusBarColor = appBarColor,
        navigationBarColor = noteColor,
        topBar = {
            if (isImageSelectEnabled) {
                ImageSelectionTopAppBar(
                    backgroundColor = appBarColor,
                    selectedImageCount = selectedImages.size,
                    onCloseClick = {
                        navController.popBackStack()
                        viewModel.deselectAllImages()
                    },
                    onSelectAllClick = { viewModel.selectAllImages() },
                    onTrashClick = { viewModel.trashSelectedImages() },
                )
            } else {
                NoteScreenTopAppBar(
                    backgroundColor = appBarColor,
                    onBackClick = onBackClick,
                    onImagePick = { uri -> viewModel.insertImage(uri) },
                    onColorSelected = { index -> viewModel.setColor(index) }
                )
            }
        },
    ) { innerPadding ->
        var columnModifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(noteColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onBackgroundClick?.invoke()
            }
        if (reorderableState != null) columnModifier = columnModifier.reorderable(reorderableState)

        LazyColumn(
            state = reorderableState?.listState ?: rememberLazyListState(),
            modifier = columnModifier,
        ) {
            item {
                NoteImageGrid(
                    images = images,
                    secondaryRowHeight = 200.dp,
                    onImageClick = {
                        if (isImageSelectEnabled) viewModel.toggleImageSelected(it)
                        else onImageCarouselStart(note.id, it)
                    },
                    onImageLongClick = { viewModel.toggleImageSelected(it) },
                    selectedImages = selectedImages,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoteTitleField(
                        modifier = Modifier.weight(1f),
                        value = note.title,
                        onValueChange = { viewModel.setTitle(it) },
                        onNext = onTitleFieldNext,
                    )
                    contextMenu?.invoke()
                }
                Spacer(Modifier.height(4.dp))
            }
            content()
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
