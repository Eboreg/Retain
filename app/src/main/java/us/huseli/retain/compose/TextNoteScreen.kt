package us.huseli.retain.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.EditTextNoteViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditTextNoteViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onSave: (id: UUID, title: String, text: String) -> Unit,
    onClose: () -> Unit,
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            NoteScreenTopAppBar {
                onSave(viewModel.noteId, title, text)
                onClose()
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding)) {
            TitleField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = {
                    viewModel.title.value = it
                }
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = text,
                onValueChange = {
                    viewModel.text.value = it
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.note)) },
                colors = outlinedTextFieldColors(),
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditTextNotePreview() {
    val snackbarHostState = remember { SnackbarHostState() }

    RetainTheme {
        EditTextNoteScreen(
            snackbarHostState = snackbarHostState,
            onSave = { _, _, _ -> },
            onClose = {},
        )
    }
}
