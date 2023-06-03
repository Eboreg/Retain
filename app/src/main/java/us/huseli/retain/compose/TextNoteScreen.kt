package us.huseli.retain.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
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
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var selection by remember { mutableStateOf(TextRange(0)) }
    var textFieldValue by remember(note.text) {
        mutableStateOf(TextFieldValue(text = note.text, selection = selection))
    }
    val snackbarHostState = remember { SnackbarHostState() }

    BaseNoteScreen(
        modifier = modifier,
        viewModel = viewModel,
        note = note,
        onTitleFieldNext = null,
        onBackClick = onBackClick,
        onBackgroundClick = {
            selection = TextRange(note.text.length)
            focusRequester.requestFocus()
        },
        onSave = onSave,
        snackbarHostState = snackbarHostState,
    ) {
        item {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = {
                    selection = it.selection
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
}
