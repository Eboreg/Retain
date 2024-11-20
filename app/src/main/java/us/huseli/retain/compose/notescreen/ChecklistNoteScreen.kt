package us.huseli.retain.compose.notescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.rememberReorderableLazyListState
import us.huseli.retain.Logger
import us.huseli.retain.R
import us.huseli.retain.dataclasses.uistate.IChecklistItemUiState
import us.huseli.retain.dataclasses.uistate.MutableChecklistItemUiState
import us.huseli.retain.dataclasses.uistate.MutableNoteUiState
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.ChecklistNoteViewModel
import java.util.UUID

@Composable
fun ChecklistNoteScreen(
    onBackClick: () -> Unit,
    onImageCarouselStart: (UUID, String) -> Unit,
    viewModel: ChecklistNoteViewModel = hiltViewModel(),
) {
    val items by viewModel.itemUiStates.collectAsStateWithLifecycle()
    val checkedItems = remember(items) { items.filter { it.isChecked } }
    val uncheckedItems = remember(items) { items.filter { !it.isChecked } }
    val note: MutableNoteUiState = viewModel.noteUiState
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            Logger.log(
                message = "onMove: from.key=${from.key}, from.index=${from.index}, to.key=${to.key}, to.index=${to.index}",
                tag = "onMove"
            )
            viewModel.switchChecklistItemPositions(from, to)
        },
    )
    val getAutocomplete: (String) -> List<IChecklistItemUiState> = remember(checkedItems) {
        { string ->
            if (string.length >= 2) checkedItems.filter { it.text.startsWith(string, ignoreCase = true) }.take(5)
            else emptyList()
        }
    }
    val onNext: (MutableChecklistItemUiState) -> Unit = { state ->
        val head = state.text.substring(0, state.selection.start)
        val tail = state.text.substring(state.selection.start)

        state.text = head
        viewModel.insertChecklistItem(
            position = state.position + 1,
            isChecked = state.isChecked,
            text = tail,
        )
    }
    val onAutocompleteSelect: (MutableChecklistItemUiState, IChecklistItemUiState) -> Unit = { state, otherState ->
        state.text = otherState.text
        state.selection = TextRange(otherState.text.length)
        viewModel.deleteChecklistItem(otherState.id)
    }

    NoteScreenScaffold(
        listState = listState,
        onImageCarouselStart = onImageCarouselStart,
        onBackClick = onBackClick,
        onTitleNext = { if (checkedItems.isEmpty() && uncheckedItems.isEmpty()) viewModel.insertChecklistItem() },
        viewModel = viewModel,
        noteContextMenu = {
            ChecklistNoteContextMenu(
                onDeleteCheckedClick = { viewModel.deleteCheckedItems() },
                onUncheckAllClick = { viewModel.uncheckAllItems() },
            )
        },
        onTitleFocusChanged = { if (it.isFocused) viewModel.setFocusedChecklistItemId(null) }
    ) {
        items(uncheckedItems, key = { it.id }) { state ->
            ChecklistItemRow(
                state = state,
                onFocusChange = {
                    if (it.isFocused) viewModel.setFocusedChecklistItemId(state.id)
                    else if (state.isTextChanged) viewModel.saveChecklistItem(state.id)
                },
                onDeleteClick = { viewModel.deleteChecklistItem(state.id) },
                onCheckedChange = { viewModel.setChecklistItemIsChecked(state.id, it) },
                onTextChange = {
                    state.text = it.text
                    state.selection = it.selection
                },
                onNext = { onNext(state) },
                reorderableState = reorderableState,
                getAutocomplete = getAutocomplete,
                onAutocompleteSelect = { onAutocompleteSelect(state, it) },
                onDragStart = { state.isDragging = true },
                onDragEnd = { state.isDragging = false },
                modifier = Modifier.background(getNoteColor(note.colorKey, MaterialTheme.colorScheme.background)),
            )
        }

        if (checkedItems.isNotEmpty()) {
            item {
                val showCheckedIconRotation by animateFloatAsState(if (note.showChecked) 0f else 180f)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleShowCheckedItems() }
                        .padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Sharp.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .rotate(showCheckedIconRotation),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.x_checked_items,
                            checkedItems.size,
                            checkedItems.size
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            if (note.showChecked) {
                items(checkedItems, key = { it.id }) { state ->
                    ChecklistItemRow(
                        state = state,
                        onFocusChange = {
                            if (it.isFocused) viewModel.setFocusedChecklistItemId(state.id)
                            else if (state.isTextChanged) viewModel.saveChecklistItem(state.id)
                        },
                        onDeleteClick = { viewModel.deleteChecklistItem(state.id) },
                        onCheckedChange = { viewModel.setChecklistItemIsChecked(state.id, it) },
                        onTextChange = {
                            state.text = it.text
                            state.selection = it.selection
                        },
                        onNext = { onNext(state) },
                        reorderableState = reorderableState,
                        getAutocomplete = getAutocomplete,
                        onAutocompleteSelect = { onAutocompleteSelect(state, it) },
                        onDragStart = { state.isDragging = true },
                        onDragEnd = { state.isDragging = false },
                    )
                }
            } else item { Spacer(Modifier.height(4.dp)) }
        }

        item {
            // "Add item" link:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { viewModel.insertChecklistItem(position = uncheckedItems.size, isChecked = false) }
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
