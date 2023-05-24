package us.huseli.retain.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import us.huseli.retain.R
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.viewmodels.EditChecklistNoteViewModel
import java.util.UUID

@Composable
fun ChecklistNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditChecklistNoteViewModel = hiltViewModel(),
    onSave: (Boolean, NoteCombo) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val showChecked by viewModel.showChecked.collectAsStateWithLifecycle(true)
    val checklistItems by viewModel.items.collectAsStateWithLifecycle()
    val trashedChecklistItems by viewModel.trashedItems.collectAsStateWithLifecycle()
    var focusedItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val itemSelectionStarts = remember { mutableStateMapOf<UUID, Int>() }
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.switchItemPositions(from, to) },
    )

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
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoTrashItems()
                SnackbarResult.Dismissed -> viewModel.clearTrashItems()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSave(viewModel.shouldSave, viewModel.combo)
        }
    }

    BaseNoteScreen(
        modifier = modifier,
        viewModel = viewModel,
        reorderableState = reorderableState,
        onTitleFieldNext = {
            if (checklistItems.isEmpty()) {
                viewModel.insertItem(text = "", checked = false, index = 0)
                focusedItemIndex = 0
            }
        },
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        contextMenu = {
            ChecklistNoteContextMenu(
                onDeleteCheckedClick = { viewModel.deleteCheckedItems() },
                onUncheckAllClick = { viewModel.uncheckAllItems() },
            )
        }
    ) { backgroundColor ->
        ChecklistNoteChecklist(
            scope = this,
            state = reorderableState,
            checklistItems = checklistItems,
            focusedItemIndex = focusedItemIndex,
            itemSelectionStarts = itemSelectionStarts,
            showChecked = showChecked,
            onItemDeleteClick = { viewModel.deleteItem(it) },
            onItemCheckedChange = { item, checked -> viewModel.updateItemChecked(item.id, checked) },
            onItemTextChange = { item, text -> viewModel.updateItemText(item.id, text) },
            onItemNext = { index, item, head, tail ->
                viewModel.insertItem(text = tail, checked = item.checked, index = index + 1)
                focusedItemIndex = index + 1
                itemSelectionStarts[item.id] = 0
                viewModel.updateItemText(item.id, head)
            },
            onItemPrevious = { index, item, text ->
                if (index > 0) {
                    val previousItem = checklistItems[index - 1]
                    focusedItemIndex = index - 1
                    itemSelectionStarts[previousItem.id] = previousItem.text.length
                    if (text.isNotEmpty()) viewModel.updateItemText(previousItem.id, previousItem.text + text)
                    viewModel.deleteItem(item)
                } else if (text.isEmpty()) {
                    viewModel.deleteItem(item)
                }
            },
            clearFocusedItemIndex = { focusedItemIndex = null },
            onShowCheckedClick = {
                viewModel.toggleShowChecked()
            },
            backgroundColor = backgroundColor
        )

        item {
            Spacer(Modifier.height(4.dp))
            // "Add item" link:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        val index = checklistItems.filter { !it.checked }.size
                        viewModel.insertItem(text = "", checked = false, index = index)
                        focusedItemIndex = index
                    }
                    .padding(bottom = 8.dp)
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
