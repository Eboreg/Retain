package us.huseli.retain.compose.notescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import us.huseli.retain.R
import us.huseli.retain.annotation.RetainAnnotatedStringState
import us.huseli.retain.annotation.rememberRetainAnnotatedStringState
import us.huseli.retain.dataclasses.uistate.ChecklistItemUiState
import us.huseli.retain.dataclasses.uistate.NoteUiState
import us.huseli.retain.ui.theme.NoteColorKey
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
    val note: NoteUiState = viewModel.noteUiState
    val listState = rememberLazyListState()
    var currentAnnotatedStringState by remember { mutableStateOf<RetainAnnotatedStringState?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to -> viewModel.switchChecklistItemPositions(from, to) },
    )

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
        onTitleFocusChanged = {
            if (it.isFocused) {
                currentAnnotatedStringState = null
                viewModel.setFocusedChecklistItemId(null)
            }
        },
        currentAnnotatedStringState = currentAnnotatedStringState,
    ) {
        ChecklistItems(
            states = uncheckedItems,
            viewModel = viewModel,
            reorderableState = reorderableState,
            noteColorKey = note.colorKey,
            onFocused = { currentAnnotatedStringState = it },
            checkedStates = checkedItems,
        )

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
                ChecklistItems(
                    states = checkedItems,
                    viewModel = viewModel,
                    reorderableState = reorderableState,
                    noteColorKey = note.colorKey,
                    onFocused = { currentAnnotatedStringState = it },
                    checkedStates = checkedItems,
                )
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


fun LazyListScope.ChecklistItems(
    states: List<ChecklistItemUiState>,
    checkedStates: List<ChecklistItemUiState>?,
    viewModel: ChecklistNoteViewModel,
    reorderableState: ReorderableLazyListState,
    noteColorKey: NoteColorKey,
    onFocused: (RetainAnnotatedStringState) -> Unit,
) {
    items(states, key = { it.id }) { state ->
        val annotatedStringState = rememberRetainAnnotatedStringState(state.serializedText)
        val scope = rememberCoroutineScope()

        ChecklistItemRow(
            state = state,
            annotatedStringState = annotatedStringState,
            onFocusChange = {
                if (it.isFocused) {
                    onFocused(annotatedStringState)
                    viewModel.setFocusedChecklistItemId(state.id)
                } else if (state.isTextChanged) viewModel.saveChecklistItem(state.id)
            },
            onDeleteClick = { viewModel.deleteChecklistItem(state.id) },
            onCheckedChange = { viewModel.setChecklistItemIsChecked(state.id, it) },
            onValueChange = { state.annotatedText = it },
            onNext = {
                scope.launch {
                    val (head, tail) = annotatedStringState.splitAtSelectionStart()

                    state.annotatedText = head.toImmutable()
                    viewModel.insertChecklistItem(
                        position = state.position + 1,
                        isChecked = state.isChecked,
                        text = tail.toImmutable().serialize(),
                    )
                }
            },
            reorderableState = reorderableState,
            getAutocomplete = { string ->
                if (string.length >= 2 && checkedStates != null)
                    checkedStates.filter { it.annotatedText.startsWith(string, ignoreCase = true) }.take(5)
                else emptyList()
            },
            onAutocompleteSelect = {
                scope.launch {
                    annotatedStringState.update(it.annotatedText)
                    annotatedStringState.jumpToLast()
                }
                state.annotatedText = it.annotatedText
                viewModel.deleteChecklistItem(it.id)
            },
            onDragStart = { state.isDragging = true },
            onDragEnd = { state.isDragging = false },
            modifier = Modifier.background(getNoteColor(noteColorKey, MaterialTheme.colorScheme.background)),
            noteColorKey = noteColorKey,
        )
    }
}
