package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.retain.Logger
import us.huseli.retain.R
import us.huseli.retain.annotation.OutlinedAnnotatedTextField
import us.huseli.retain.annotation.rememberRetainAnnotatedStringState
import us.huseli.retain.dataclasses.uistate.NoteUiState
import us.huseli.retain.viewmodels.TextNoteViewModel
import java.util.UUID

@Composable
fun TextNoteScreen(
    onBackClick: () -> Unit,
    onImageCarouselStart: (UUID, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TextNoteViewModel = hiltViewModel(),
) {
    val focusRequester = remember { FocusRequester() }
    val note: NoteUiState = viewModel.noteUiState
    val textState = rememberRetainAnnotatedStringState(note.serializedText)
    var isFormattingEnabled by remember { mutableStateOf(false) }

    NoteScreenScaffold(
        listState = rememberLazyListState(),
        onImageCarouselStart = onImageCarouselStart,
        onBackClick = onBackClick,
        viewModel = viewModel,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                focusRequester.requestFocus()
                textState.jumpToLast()
            },
        ),
        currentAnnotatedStringState = if (isFormattingEnabled) textState else null,
    ) {
        item {
            OutlinedAnnotatedTextField(
                state = textState,
                /*
                onValueChange = {
                    Logger.log("TextNoteScreen", "onValueChange: note.annotatedText=${note.annotatedText}, it=$it")
                    note.annotatedText = it
                },
                 */
                readOnly = note.isReadOnly,
                placeholder = { Text(text = stringResource(R.string.note)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                onChange = { change ->
                    Logger.log("TextNoteScreen", "onChange: note.annotatedText=${note.annotatedText}, change=$change")
                    note.annotatedText = textState.getAnnotatedString()
                    if (change.style || change.wholeWords) viewModel.saveUndoState()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFormattingEnabled = it.isFocused
                        if (!it.isFocused && note.isTextChanged) viewModel.saveNote()
                    },
            )
        }
    }
}
