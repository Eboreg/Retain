package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import us.huseli.retain.R
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.Note
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.EditChecklistNoteViewModel
import java.util.UUID

@Composable
fun ChecklistNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditChecklistNoteViewModel = hiltViewModel(),
    onSave: (Note?, List<ChecklistItem>, List<Image>, List<UUID>, List<String>) -> Unit,
    onBackClick: () -> Unit,
    onImageCarouselStart: (UUID, String) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val note by viewModel.note.collectAsStateWithLifecycle()
    val trashedChecklistItems by viewModel.trashedItems.collectAsStateWithLifecycle()
    val checkedItems by viewModel.checkedItems.collectAsStateWithLifecycle(emptyList())
    val uncheckedItems by viewModel.uncheckedItems.collectAsStateWithLifecycle(emptyList())
    val focusedItemId by viewModel.focusedItemId.collectAsStateWithLifecycle()
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.switchItemPositions(from, to) },
    )
    val noteColor by remember(note.color) { mutableStateOf(getNoteColor(context, note.color)) }

    LaunchedEffect(trashedChecklistItems) {
        if (trashedChecklistItems.isNotEmpty()) {
            val message = context.resources.getQuantityString(
                R.plurals.x_checklistitems_trashed,
                trashedChecklistItems.size,
                trashedChecklistItems.size
            )
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.resources.getString(R.string.undo).uppercase(),
                duration = SnackbarDuration.Long,
                withDismissAction = true,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDeleteItems()
                SnackbarResult.Dismissed -> viewModel.clearTrashedItems()
            }
        }
    }

    BaseNoteScreen(
        modifier = modifier,
        viewModel = viewModel,
        noteId = note.id,
        color = note.color,
        title = note.title,
        reorderableState = reorderableState,
        onTitleFieldNext = {
            if (checkedItems.isEmpty() && uncheckedItems.isEmpty()) {
                viewModel.insertItem(text = "", checked = false, index = 0)
            }
        },
        onBackClick = onBackClick,
        onSave = onSave,
        onImageCarouselStart = onImageCarouselStart,
        snackbarHostState = snackbarHostState,
        contextMenu = {
            ChecklistNoteContextMenu(
                onDeleteCheckedClick = { viewModel.deleteCheckedItems() },
                onUncheckAllClick = { viewModel.uncheckAllItems() },
            )
        }
    ) {
        ChecklistNoteChecklist(
            scope = this,
            state = reorderableState,
            showChecked = note.showChecked,
            uncheckedItems = uncheckedItems,
            checkedItems = checkedItems,
            onItemDeleteClick = { viewModel.deleteItem(it) },
            onItemCheckedChange = { item, value -> viewModel.updateItemChecked(item, value) },
            onItemTextFieldValueChange = { item, textFieldValue ->
                viewModel.onTextFieldValueChange(item, textFieldValue)
            },
            onNextItem = { item -> viewModel.onNextItem(item) },
            onShowCheckedClick = { viewModel.toggleShowChecked() },
            backgroundColor = noteColor,
            focusedItemId = focusedItemId,
            onItemFocus = { viewModel.onItemFocus(it) }
        )

        item {
            // "Add item" link:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        val index = uncheckedItems.size
                        viewModel.insertItem(text = "", checked = false, index = index)
                    }
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Sharp.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Text(
                    text = stringResource(R.string.add_item),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}
