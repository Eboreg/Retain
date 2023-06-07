package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.viewmodels.EditTextNoteViewModel
import java.util.UUID

@Composable
fun TextNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditTextNoteViewModel = hiltViewModel(),
    onSave: (Note?, List<ChecklistItem>, List<Image>, List<UUID>, List<String>) -> Unit,
    onBackClick: () -> Unit,
    onImageCarouselStart: (UUID, String) -> Unit,
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val textFieldValue by viewModel.textFieldValue.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BaseNoteScreen(
        modifier = modifier,
        viewModel = viewModel,
        noteId = note.id,
        color = note.color,
        title = note.title,
        onTitleFieldNext = null,
        onBackClick = onBackClick,
        onBackgroundClick = {
            viewModel.moveCursorLast()
            focusRequester.requestFocus()
        },
        onSave = onSave,
        onImageCarouselStart = onImageCarouselStart,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { viewModel.setTextFieldValue(it) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(text = stringResource(R.string.note)) },
                colors = outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
        }
    }
}
