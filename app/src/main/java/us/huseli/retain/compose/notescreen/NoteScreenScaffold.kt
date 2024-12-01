package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import us.huseli.retain.R
import us.huseli.retain.annotation.RetainAnnotatedStringState
import us.huseli.retain.compose.NoteImageGrid
import us.huseli.retain.compose.RetainScaffold
import us.huseli.retain.compose.notescreen.bars.ImageSelectionTopAppBar
import us.huseli.retain.compose.notescreen.bars.NoteScreenBottomBar
import us.huseli.retain.compose.notescreen.bars.NoteScreenTopAppBar
import us.huseli.retain.compose.notescreen.bars.TextFormatTopAppBar
import us.huseli.retain.dataclasses.uistate.NoteUiState
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.getNoteColorVariant
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.AbstractNoteViewModel
import java.util.UUID

@Composable
fun NoteScreenScaffold(
    listState: LazyListState,
    onImageCarouselStart: (UUID, String) -> Unit,
    onBackClick: () -> Unit,
    onTitleNext: () -> Unit = {},
    onTitleFocusChanged: (FocusState) -> Unit = {},
    viewModel: AbstractNoteViewModel<*>,
    currentAnnotatedStringState: RetainAnnotatedStringState?,
    modifier: Modifier = Modifier,
    noteContextMenu: @Composable () -> Unit = {},
    extraContent: LazyListScope.() -> Unit = {},
) {
    val imageUiStates by viewModel.imageUiStates.collectAsStateWithLifecycle()
    val hasSelectedImages = imageUiStates.filter { it.isSelected }.isNotEmpty()
    val note: NoteUiState = viewModel.noteUiState
    var isFormatButtonsActive by remember(currentAnnotatedStringState) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.save() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (viewModel.shouldSaveUndoState) viewModel.saveUndoState()
            viewModel.save()
            delay(5_000)
        }
    }

    RetainScaffold(
        systemBarColorKey = note.colorKey,
        topBar = {
            val backgroundColor = getNoteColorVariant(
                viewModel.noteUiState.colorKey,
                MaterialTheme.colorScheme.surfaceContainer,
            )

            if (hasSelectedImages) ImageSelectionTopAppBar(viewModel = viewModel, backgroundColor = backgroundColor)
            else if (isFormatButtonsActive && currentAnnotatedStringState != null) TextFormatTopAppBar(
                state = currentAnnotatedStringState,
                onDismissRequest = { isFormatButtonsActive = false },
                backgroundColor = backgroundColor,
            )
            else NoteScreenTopAppBar(
                backgroundColor = backgroundColor,
                viewModel = viewModel,
                onBackClick = onBackClick,
                isTextFormatEnabled = currentAnnotatedStringState != null,
                onTextFormatClick = { isFormatButtonsActive = true },
            )
        },
        bottomBar = { NoteScreenBottomBar(viewModel = viewModel) },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(getNoteColor(note.colorKey, MaterialTheme.colorScheme.background))
        ) {
            if (imageUiStates.isNotEmpty()) {
                item {
                    NoteImageGrid(
                        images = imageUiStates,
                        secondaryRowHeight = 200.dp,
                        onImageClick = {
                            if (hasSelectedImages) viewModel.toggleImageSelected(it)
                            else onImageCarouselStart(note.id, it)
                        },
                        onImageLongClick = { viewModel.toggleImageSelected(it) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = note.title,
                        onValueChange = { note.title = it },
                        readOnly = note.isReadOnly,
                        textStyle = MaterialTheme.typography.headlineSmall,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        keyboardActions = KeyboardActions(onNext = { onTitleNext() }),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged {
                                if (!it.isFocused && note.isTitleChanged) viewModel.saveNote()
                                onTitleFocusChanged(it)
                            },
                    )

                    noteContextMenu()
                }
            }

            extraContent()
        }
    }
}
