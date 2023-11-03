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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.Enums.NoteType
import us.huseli.retain.R
import us.huseli.retain.compose.NoteImageGrid
import us.huseli.retain.compose.RetainScaffold
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.getAppBarColor
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retaintheme.snackbar.SnackbarEngine
import java.util.UUID

@Composable
fun NoteScreen(
    onBackClick: () -> Unit,
    onImageCarouselStart: (UUID, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = hiltViewModel(),
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val note by viewModel.note.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val checkedItems by viewModel.checkedItems.collectAsStateWithLifecycle(emptyList())
    val uncheckedItems by viewModel.uncheckedItems.collectAsStateWithLifecycle(emptyList())
    val focusedChecklistItemId by viewModel.focusedChecklistItemId.collectAsStateWithLifecycle()
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val isUnsaved by viewModel.isUnsaved.collectAsStateWithLifecycle()

    val appBarColor by remember(note?.color) {
        mutableStateOf(note?.color?.let { getAppBarColor(context, it, surface) } ?: surface)
    }
    val noteColor by remember(note?.color) {
        mutableStateOf(note?.color?.let { getNoteColor(context, it, background) } ?: background)
    }
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.switchItemPositions(from, to) },
    )

    note?.text?.also { textFieldValue = textFieldValue.copy(text = it) }

    BackHandler(selectedImages.isNotEmpty()) {
        viewModel.deselectAllImages()
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (isUnsaved) viewModel.save()
            delay(10_000)
        }
    }

    val onImagesDeleted = { count: Int ->
        if (count > 0) {
            SnackbarEngine.addInfo(
                message = context.resources.getQuantityString(
                    R.plurals.x_images_trashed,
                    count,
                    count,
                ),
                actionLabel = context.getString(R.string.undo).uppercase(),
                onActionPerformed = { viewModel.undeleteImages() },
            )
        }
    }

    val onChecklistItemsDeleted = { count: Int ->
        if (count > 0) {
            SnackbarEngine.addInfo(
                message = context.resources.getQuantityString(
                    R.plurals.x_checklistitems_trashed,
                    count,
                    count,
                ),
                actionLabel = context.getString(R.string.undo).uppercase(),
                onActionPerformed = { viewModel.undeleteChecklistItems() },
            )
        }
    }

    RetainScaffold(
        navigationBarColor = noteColor,
        statusBarColor = appBarColor,
        topBar = {
            if (selectedImages.isNotEmpty()) {
                ImageSelectionTopAppBar(
                    backgroundColor = appBarColor,
                    selectedImageCount = selectedImages.size,
                    onCloseClick = { viewModel.deselectAllImages() },
                    onSelectAllClick = { viewModel.selectAllImages() },
                    onTrashClick = { viewModel.deleteSelectedImages(onImagesDeleted) },
                )
            } else {
                NoteScreenTopAppBar(
                    backgroundColor = appBarColor,
                    onBackClick = {
                        if (isUnsaved) viewModel.save()
                        onBackClick()
                    },
                    onImagePick = { uri -> viewModel.insertImage(uri) },
                    onColorSelected = { index -> viewModel.setColor(index) }
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = reorderableState.listState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(noteColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (note?.type == NoteType.TEXT) {
                            focusRequester.requestFocus()
                            textFieldValue = textFieldValue.copy(selection = TextRange(note?.text?.length ?: 0))
                        }
                    },
                )
                .reorderable(reorderableState)
        ) {
            note?.also { note ->
                item {
                    NoteImageGrid(
                        images = images,
                        secondaryRowHeight = 200.dp,
                        onImageClick = {
                            if (selectedImages.isNotEmpty()) viewModel.toggleImageSelected(it)
                            else onImageCarouselStart(note.id, it)
                        },
                        onImageLongClick = { viewModel.toggleImageSelected(it) },
                        selectedImages = selectedImages,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) viewModel.setChecklistItemFocus(null) },
                        value = note?.title ?: "",
                        onValueChange = { viewModel.setTitle(it) },
                        textStyle = MaterialTheme.typography.headlineSmall,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (note?.type == NoteType.CHECKLIST && checkedItems.isEmpty() && uncheckedItems.isEmpty()) {
                                    viewModel.insertChecklistItem(text = "", checked = false, position = 0)
                                }
                            }
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

                    if (note?.type == NoteType.CHECKLIST) {
                        ChecklistNoteContextMenu(
                            onDeleteCheckedClick = { viewModel.deleteCheckedItems(onChecklistItemsDeleted) },
                            onUncheckAllClick = { viewModel.uncheckAllItems() },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            note?.also { note ->
                when (note.type) {
                    NoteType.TEXT -> {
                        item {
                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = {
                                    textFieldValue = it
                                    viewModel.setText(it.text)
                                },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                placeholder = { Text(text = stringResource(R.string.note)) },
                                colors = outlinedTextFieldColors(),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    capitalization = KeyboardCapitalization.Sentences,
                                ),
                            )
                        }
                    }
                    NoteType.CHECKLIST -> {
                        ChecklistNoteChecklist(
                            state = reorderableState,
                            showChecked = note.showChecked,
                            uncheckedItems = uncheckedItems,
                            checkedItems = checkedItems,
                            onItemDeleteClick = { viewModel.deleteChecklistItem(it, onChecklistItemsDeleted) },
                            onItemCheckedChange = { item, value -> viewModel.updateChecklistItemChecked(item, value) },
                            onItemTextChange = { item, text -> viewModel.setChecklistItemText(item, text) },
                            onNextItem = { item, textFieldValue -> viewModel.splitChecklistItem(item, textFieldValue) },
                            onShowCheckedClick = { viewModel.toggleShowCheckedItems() },
                            backgroundColor = noteColor,
                            focusedItemId = focusedChecklistItemId,
                            onItemFocus = { viewModel.setChecklistItemFocus(it) },
                            onAddItemClick = {
                                viewModel.insertChecklistItem(
                                    text = "",
                                    checked = false,
                                    position = uncheckedItems.size,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
