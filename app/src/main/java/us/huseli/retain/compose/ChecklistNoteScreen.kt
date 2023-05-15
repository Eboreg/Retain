package us.huseli.retain.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.R
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.viewmodels.EditChecklistNoteViewModel
import java.util.UUID
import kotlin.math.max

/**
 * A row with a checkbox and a textfield. Multiple rows of these emulate
 * multiline text editing behaviour, in that pressing enter in the middle of
 * a row will bring the characters after the cursor down to a new row, and
 * pressing backspace at the beginning of a row will paste the entire row
 * contents to the end of the previous row.
 *
 * This is done via a hack where the textfield internally has an invisible
 * null character at the beginning. Normally, there is no way for us to know
 * that the user has pressed backspace when there are no characters before the
 * cursor, but in this way, there actually are, even though it looks like the
 * cursor is at the beginning of the row.
 */
@Composable
fun ChecklistRow(
    modifier: Modifier = Modifier,
    item: ChecklistItem,
    isFocused: Boolean,
    isDragging: Boolean,
    selectionStart: Int,
    onFocus: () -> Unit,
    onDeleteClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onNext: (String, String) -> Unit,
    onPrevious: (String) -> Unit,
    reorderableState: ReorderableLazyListState,
) {
    val text by rememberSaveable(item.text) { mutableStateOf(item.text) }
    val checked by rememberSaveable(item.checked) { mutableStateOf(item.checked) }
    val alpha = if (checked) 0.5f else 1f
    val focusRequester = remember { FocusRequester() }
    val stripNullChar = { str: String -> str.filter { it != Char.MIN_VALUE } }
    val addNullChar = { str: String -> Char.MIN_VALUE + stripNullChar(str) }
    var selection by remember(selectionStart) { mutableStateOf(TextRange(selectionStart + 1)) }

    val adjustSelection = { range: TextRange ->
        if (range.start < 1) TextRange(1, max(range.end, 1))
        else range
    }

    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text = addNullChar(text), selection = selection))
    }

    val realModifier =
        if (isDragging) modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            shape = ShapeDefaults.ExtraSmall
        )
        else modifier

    Row(
        modifier = realModifier, //.offset { IntOffset(0, offsetY.roundToInt()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DragIndicator,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .detectReorder(reorderableState)
        )
        Checkbox(
            modifier = Modifier.padding(start = 0.dp),
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha),
            )
        )
        BasicTextField(
            value = textFieldValue,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
            textStyle = TextStyle.Default.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                fontSize = 16.sp,
            ),
            onValueChange = {
                /**
                 * If the new TextFieldValue does not start with the null
                 * character, that must mean the user has just erased it by
                 * inputting a backspace at the beginning of the field. In that
                 * case, join this row with the one above. If this is the first
                 * row: just re-insert the null character move the selection
                 * start to after it.
                 */
                if (it.text.isEmpty() || it.text[0] != Char.MIN_VALUE) {
                    onPrevious(stripNullChar(it.text))
                }
                selection = adjustSelection(it.selection)
                textFieldValue = it.copy(
                    selection = adjustSelection(selection),
                    text = addNullChar(it.text),
                )
                onTextChange(stripNullChar(it.text))
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    val head = textFieldValue.text.substring(0, textFieldValue.selection.start)
                    val tail = textFieldValue.text.substring(textFieldValue.selection.start)
                    onNext(stripNullChar(head), stripNullChar(tail))
                }
            ),
            modifier = Modifier
                .onFocusChanged {
                    if (it.isFocused) {
                        textFieldValue = textFieldValue.copy(selection = adjustSelection(textFieldValue.selection))
                        onFocus()
                    }
                }
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned {
                    if (isFocused) focusRequester.requestFocus()
                },
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null
            )
        }
    }
}


@Composable
fun Checklist(
    modifier: Modifier = Modifier,
    checklistItems: List<ChecklistItem>,
    focusedItemPosition: Int?,
    itemSelectionStarts: Map<UUID, Int>,
    showChecked: Boolean,
    onItemDeleteClick: (ChecklistItem) -> Unit,
    onItemCheckedChange: (ChecklistItem, Boolean) -> Unit,
    onItemTextChange: (ChecklistItem, String) -> Unit,
    onItemNext: (ChecklistItem, String, String) -> Unit,
    onItemPrevious: (ChecklistItem, String) -> Unit,
    onSwitchPositions: (UUID, UUID) -> Unit,
    clearFocusedItemPosition: () -> Unit,
    onShowCheckedClick: () -> Unit,
    backgroundColor: Color,
) {
    val isItemFocused = { item: ChecklistItem -> focusedItemPosition == item.position }
    val uncheckedItems = checklistItems.filter { !it.checked }
    val checkedItems = checklistItems.filter { it.checked }
    val onMove: (from: ItemPosition, to: ItemPosition) -> Unit = { from, to ->
        val fromKey = from.key
        val toKey = to.key
        if (fromKey is UUID && toKey is UUID) onSwitchPositions(fromKey, toKey)
    }
    val uncheckedState = rememberReorderableLazyListState(onMove = onMove)
    val checkedState = rememberReorderableLazyListState(onMove = onMove)

    LazyColumn(
        modifier = Modifier.reorderable(uncheckedState),
        state = uncheckedState.listState
    ) {
        items(uncheckedItems, key = { it.id }) { item ->
            ReorderableItem(uncheckedState, key = item.id) { isDragging ->
                ChecklistRow(
                    modifier = modifier.background(backgroundColor),
                    item = item,
                    isFocused = isItemFocused(item),
                    isDragging = isDragging,
                    selectionStart = itemSelectionStarts[item.id] ?: 0,
                    onDeleteClick = { onItemDeleteClick(item) },
                    onTextChange = { onItemTextChange(item, it) },
                    onCheckedChange = { onItemCheckedChange(item, it) },
                    onNext = { head, tail -> onItemNext(item, head, tail) },
                    onPrevious = { text -> onItemPrevious(item, text) },
                    onFocus = clearFocusedItemPosition,
                    reorderableState = uncheckedState,
                )
            }
        }
    }

    // Checked items:
    if (checkedItems.isNotEmpty()) {
        val showCheckedIconRotation by animateFloatAsState(if (showChecked) 0f else 180f)

        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onShowCheckedClick() },
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 12.dp).rotate(showCheckedIconRotation),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                text = pluralStringResource(R.plurals.x_checked_items, checkedItems.size, checkedItems.size),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        if (showChecked) {
            LazyColumn(
                modifier = Modifier.reorderable(checkedState),
                state = checkedState.listState,
            ) {
                items(checkedItems, key = { it.id }) { item ->
                    ReorderableItem(checkedState, key = item.id) { isDragging ->
                        ChecklistRow(
                            modifier = modifier.background(backgroundColor),
                            item = item,
                            isFocused = isItemFocused(item),
                            isDragging = isDragging,
                            selectionStart = itemSelectionStarts[item.id] ?: 0,
                            onFocus = clearFocusedItemPosition,
                            onDeleteClick = { onItemDeleteClick(item) },
                            onCheckedChange = { onItemCheckedChange(item, it) },
                            onTextChange = { onItemTextChange(item, it) },
                            onNext = { head, tail -> onItemNext(item, head, tail) },
                            onPrevious = { text -> onItemPrevious(item, text) },
                            reorderableState = checkedState,
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}


@Composable
fun ChecklistNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditChecklistNoteViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onSave: (Boolean, UUID, String, Boolean, Int, Collection<ChecklistItem>) -> Unit,
    onClose: () -> Unit,
) {
    val showChecked by viewModel.showChecked.collectAsStateWithLifecycle()
    val checklistItems by viewModel.checklistItems.collectAsStateWithLifecycle(emptyList())
    var focusedItemPosition by rememberSaveable { mutableStateOf<Int?>(null) }
    val itemSelectionStarts = remember { mutableStateMapOf<UUID, Int>() }

    BaseNoteScreen(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        viewModel = viewModel,
        onTitleFieldNext = {
            if (checklistItems.isEmpty()) {
                viewModel.insertItem(text = "", checked = false, position = 0)
                focusedItemPosition = 0
            }
        },
        onClose = {
            onSave(
                viewModel.shouldSave,
                viewModel.noteId,
                viewModel.title.value,
                showChecked,
                viewModel.colorIdx.value,
                viewModel.updatedItems
            )
            onClose()
        },
    ) { backgroundColor ->
        Checklist(
            checklistItems = checklistItems,
            focusedItemPosition = focusedItemPosition,
            itemSelectionStarts = itemSelectionStarts,
            showChecked = showChecked,
            onItemCheckedChange = { item, checked -> viewModel.updateItemChecked(item.id, checked) },
            onItemTextChange = { item, text -> viewModel.updateItemText(item.id, text) },
            onItemDeleteClick = { viewModel.deleteItem(it) },
            backgroundColor = backgroundColor,
            onItemNext = { item, head, tail ->
                viewModel.insertItem(text = tail, checked = item.checked, position = item.position + 1)
                focusedItemPosition = item.position + 1
                itemSelectionStarts[item.id] = 0
                viewModel.updateItem(item.id, head, item.checked, item.position)
            },
            onItemPrevious = { item, text ->
                val itemIdx = checklistItems.indexOf(item)
                if (itemIdx > 0) {
                    val previousItem = checklistItems[itemIdx - 1]
                    focusedItemPosition = previousItem.position
                    itemSelectionStarts[previousItem.id] = previousItem.text.length
                    if (text.isNotEmpty()) {
                        viewModel.updateItem(
                            id = previousItem.id,
                            text = previousItem.text + text,
                            checked = previousItem.checked,
                            position = previousItem.position
                        )
                    }
                    viewModel.deleteItem(item)
                } else if (text.isEmpty()) {
                    viewModel.deleteItem(item)
                }
            },
            clearFocusedItemPosition = { focusedItemPosition = null },
            onShowCheckedClick = {
                viewModel.toggleShowChecked()
            },
            onSwitchPositions = { fromId, toId ->
                viewModel.switchItemPositions(fromId, toId)
            }
        )

        Spacer(Modifier.height(4.dp))

        // "Add item" link:
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    val position = checklistItems.lastOrNull()?.let { it.position + 1 } ?: 0
                    viewModel.insertItem(text = "", checked = false, position = position)
                    focusedItemPosition = position
                }
                .padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
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
