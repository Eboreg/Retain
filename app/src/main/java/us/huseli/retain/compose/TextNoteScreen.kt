package us.huseli.retain.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.EditNoteViewModel
import java.util.UUID

@Composable
fun TextNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditNoteViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onSave: ((shouldSave: Boolean, id: UUID, title: String, text: String, colorIdx: Int) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val text by viewModel.text.collectAsStateWithLifecycle()

    BaseNoteScreen(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        viewModel = viewModel,
        onTitleFieldNext = null,
        onClose = {
            if (onSave != null) {
                onSave(
                    viewModel.shouldSave,
                    viewModel.noteId,
                    viewModel.title.value,
                    viewModel.text.value,
                    viewModel.colorIdx.value
                )
            }
            if (onClose != null) onClose()
        },
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                viewModel.setText(it)
            },
            modifier = Modifier.fillMaxSize().heightIn(min = 200.dp),
            placeholder = { Text(text = stringResource(R.string.note)) },
            colors = outlinedTextFieldColors(),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
            ),
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TextNotePreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    RetainTheme {
        TextNoteScreen(snackbarHostState = snackbarHostState)
    }
}
