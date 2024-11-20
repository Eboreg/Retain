package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.retain.R
import us.huseli.retain.dataclasses.uistate.MutableNoteUiState
import us.huseli.retain.outlinedTextFieldColors
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
    val note: MutableNoteUiState = viewModel.noteUiState

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
                note.selection = TextRange(note.text.length)
            },
        ),
    ) {
        item {
            OutlinedTextField(
                value = note.textFieldValue,
                onValueChange = {
                    note.text = it.text
                    note.selection = it.selection
                },
                readOnly = note.isReadOnly,
                placeholder = { Text(text = stringResource(R.string.note)) },
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (!it.isFocused && note.isTextChanged) viewModel.saveNote()
                    },
            )
        }
    }
}
