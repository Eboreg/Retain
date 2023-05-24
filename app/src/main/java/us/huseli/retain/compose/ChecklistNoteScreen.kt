package us.huseli.retain.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.DragIndicator
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import us.huseli.retain.R
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.viewmodels.EditChecklistNoteViewModel
import java.util.UUID
import kotlin.math.max

@Composable
fun ChecklistNoteContextMenu(
    modifier: Modifier = Modifier,
    onUncheckAllClick: () -> Unit,
    onDeleteCheckedClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Sharp.MoreVert,
                contentDescription = null,
            )
        }
        DropdownMenu(
            modifier = modifier,
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuItem(
                onClick = {
                    onUncheckAllClick()
                    isExpanded = false
                },
                text = { Text(stringResource(R.string.uncheck_all)) },
            )
            DropdownMenuItem(
                onClick = {
                    onDeleteCheckedClick()
                    isExpanded = false
                },
                text = { Text(stringResource(R.string.delete_checked)) },
            )
        }
    }
}

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
    fun stripNullChar(str: String): String = str.filter { it != Char.MIN_VALUE }
    fun addNullChar(str: String): String = Char.MIN_VALUE + stripNullChar(str)
    fun adjustSelection(range: TextRange): TextRange = if (range.start < 1) TextRange(1, max(range.end, 1)) else range

    val alpha = if (item.checked) 0.5f else 1f
    val focusRequester = remember { FocusRequester() }
    var selection by remember(selectionStart) { mutableStateOf(TextRange(selectionStart + 1)) }
    val text by remember(item.text) { mutableStateOf(addNullChar(item.text)) }
    var textFieldValue by remember(item.text) {
        mutableStateOf(TextFieldValue(text = text, selection = selection))
    }

    val realModifier =
        if (isDragging) modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                shape = ShapeDefaults.ExtraSmall
            )
        else modifier

    Row(
        modifier = realModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Sharp.DragIndicator,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .detectReorder(reorderableState)
        )
        Checkbox(
            modifier = Modifier.padding(start = 0.dp),
            checked = item.checked,
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
                if ((it.text.isEmpty() || it.text[0] != Char.MIN_VALUE)) {
                    if (it.text != textFieldValue.text) onPrevious(stripNullChar(it.text))
                } else {
                    if (it.text != textFieldValue.text) onTextChange(stripNullChar(it.text))
                }
                selection = it.selection
                textFieldValue = it.copy(
                    selection = adjustSelection(it.selection),
                    text = it.text,
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences,
            ),
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
                imageVector = Icons.Sharp.Close,
                contentDescription = null
            )
        }
    }
}


fun Checklist(
    modifier: Modifier = Modifier,
    scope: LazyListScope,
    state: ReorderableLazyListState,
    checklistItems: List<ChecklistItem>,
    focusedItemIndex: Int?,
    itemSelectionStarts: Map<UUID, Int>,
    showChecked: Boolean,
    onItemDeleteClick: (ChecklistItem) -> Unit,
    onItemCheckedChange: (ChecklistItem, Boolean) -> Unit,
    onItemTextChange: (ChecklistItem, String) -> Unit,
    onItemNext: (Int, ChecklistItem, String, String) -> Unit,
    onItemPrevious: (Int, ChecklistItem, String) -> Unit,
    clearFocusedItemIndex: () -> Unit,
    onShowCheckedClick: () -> Unit,
    backgroundColor: Color,
) {
    val uncheckedItems = checklistItems.filter { !it.checked }
    val checkedItems = checklistItems.filter { it.checked }

    scope.itemsIndexed(uncheckedItems, key = { _, item -> item.id }) { index, item ->
        ReorderableItem(state, key = item.id) { isDragging ->
            ChecklistRow(
                modifier = modifier.background(backgroundColor),
                item = item,
                isFocused = focusedItemIndex == index,
                isDragging = isDragging,
                selectionStart = itemSelectionStarts[item.id] ?: 0,
                onDeleteClick = { onItemDeleteClick(item) },
                onTextChange = { onItemTextChange(item, it) },
                onCheckedChange = { onItemCheckedChange(item, it) },
                onNext = { head, tail -> onItemNext(index, item, head, tail) },
                onPrevious = { text -> onItemPrevious(index, item, text) },
                onFocus = clearFocusedItemIndex,
                reorderableState = state,
            )
        }
    }

    // Checked items:
    if (checkedItems.isNotEmpty()) {
        scope.item {
            val showCheckedIconRotation by animateFloatAsState(if (showChecked) 0f else 180f)

            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onShowCheckedClick() },
            ) {
                Icon(
                    imageVector = Icons.Sharp.ExpandMore,
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
        }

        if (showChecked) {
            scope.itemsIndexed(checkedItems, key = { _, item -> item.id }) { index, item ->
                ReorderableItem(state, key = item.id) { isDragging ->
                    ChecklistRow(
                        modifier = modifier.background(backgroundColor),
                        item = item,
                        isFocused = focusedItemIndex == index + uncheckedItems.size,
                        isDragging = isDragging,
                        selectionStart = itemSelectionStarts[item.id] ?: 0,
                        onFocus = clearFocusedItemIndex,
                        onDeleteClick = { onItemDeleteClick(item) },
                        onCheckedChange = { onItemCheckedChange(item, it) },
                        onTextChange = { onItemTextChange(item, it) },
                        onNext = { head, tail -> onItemNext(index + uncheckedItems.size, item, head, tail) },
                        onPrevious = { text -> onItemPrevious(index + uncheckedItems.size, item, text) },
                        reorderableState = state,
                    )
                }
            }
        } else {
            scope.item {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}


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
        Checklist(
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
