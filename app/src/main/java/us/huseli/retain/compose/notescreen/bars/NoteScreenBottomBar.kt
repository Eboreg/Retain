package us.huseli.retain.compose.notescreen.bars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Redo
import androidx.compose.material.icons.automirrored.sharp.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.isoTime
import us.huseli.retain.ui.theme.getNoteColorVariant
import us.huseli.retain.viewmodels.AbstractNoteViewModel
import us.huseli.retaintheme.extensions.DateTimePrecision
import us.huseli.retaintheme.extensions.isoDateTime
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun NoteScreenBottomBar(viewModel: AbstractNoteViewModel<*>) {
    val isUndoPossible by viewModel.isUndoPossible.collectAsStateWithLifecycle()
    val isRedoPossible by viewModel.isRedoPossible.collectAsStateWithLifecycle()

    NoteScreenBottomBar(
        backgroundColor = getNoteColorVariant(
            viewModel.noteUiState.colorKey,
            MaterialTheme.colorScheme.surfaceContainer,
        ),
        isUndoPossible = isUndoPossible,
        isRedoPossible = isRedoPossible,
        onUndoClick = { viewModel.undo() },
        onRedoClick = { viewModel.redo() },
        updated = viewModel.noteUiState.updated,
    )
}

@Composable
fun NoteScreenBottomBar(
    backgroundColor: Color,
    updated: Instant,
    isUndoPossible: Boolean,
    isRedoPossible: Boolean,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
) {
    val insets = WindowInsets.navigationBars.union(WindowInsets.ime)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .windowInsetsPadding(insets)
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp)
    ) {
        if (isUndoPossible || isRedoPossible) {
            IconButton(onClick = onUndoClick, enabled = isUndoPossible) {
                Icon(Icons.AutoMirrored.Sharp.Undo, stringResource(R.string.undo))
            }
            IconButton(onClick = onRedoClick, enabled = isRedoPossible) {
                Icon(Icons.AutoMirrored.Sharp.Redo, stringResource(R.string.redo))
            }
        } else {
            val updatedString = if (updated.isAfter(Instant.now().truncatedTo(ChronoUnit.DAYS)))
                updated.isoTime(DateTimePrecision.MINUTE)
            else updated.isoDateTime(DateTimePrecision.MINUTE)

            Text(stringResource(R.string.updated_x, updatedString), style = MaterialTheme.typography.bodySmall)
        }
    }
}
