package us.huseli.retain.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.EditNoteViewModel

@Composable
fun TextNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditNoteViewModel = hiltViewModel(),
    onSave: ((shouldSave: Boolean, combo: NoteCombo) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
) {
    val text by viewModel.text.collectAsStateWithLifecycle("")
    val focusRequester = remember { FocusRequester() }
    var selection by remember { mutableStateOf(TextRange(0)) }
    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text = text, selection = selection))
    }
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        onDispose {
            onSave?.invoke(viewModel.shouldSave, viewModel.combo)
        }
    }

    BaseNoteScreen(
        modifier = modifier,
        viewModel = viewModel,
        onTitleFieldNext = null,
        onBackClick = { onBackClick?.invoke() },
        onBackgroundClick = {
            selection = TextRange(text.length)
            focusRequester.requestFocus()
        },
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


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TextNotePreview() {
    RetainTheme { TextNoteScreen() }
}
