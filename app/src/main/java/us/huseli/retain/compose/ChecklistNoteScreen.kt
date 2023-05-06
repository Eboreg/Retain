package us.huseli.retain.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
    focused: Boolean,
    selectionStart: Int,
    onFocus: () -> Unit,
    onDeleteClick: () -> Unit,
    onChange: (String, Boolean) -> Unit,
    onNext: (String, String) -> Unit,
    onPrevious: (String) -> Unit,
) {
    val text by rememberSaveable(item.text) { mutableStateOf(item.text) }
    val checked by rememberSaveable(item.checked) { mutableStateOf(item.checked) }
    val alpha = if (checked) 0.5f else 1f
    val focusRequester = remember { FocusRequester() }
    val stripNullChar = { str: String -> str.filter { it != Char.MIN_VALUE } }
    val addNullChar = { str: String -> Char.MIN_VALUE + stripNullChar(str) }

    val adjustSelection = { selection: TextRange ->
        if (selection.start < 1) TextRange(1, max(selection.end, 1))
        else selection
    }

    var textFieldValue by remember(text, selectionStart) {
        mutableStateOf(TextFieldValue(text = addNullChar(text), selection = TextRange(selectionStart + 1)))
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                onChange(stripNullChar(textFieldValue.text), it)
            },
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
                textFieldValue = it.copy(
                    selection = adjustSelection(it.selection),
                    text = addNullChar(it.text),
                )
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
                    } else {
                        onChange(stripNullChar(textFieldValue.text), checked)
                    }
                }
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned {
                    if (focused) focusRequester.requestFocus()
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
    onItemChange: (ChecklistItem, String, Boolean) -> Unit,
    onItemNext: (ChecklistItem, String, String) -> Unit,
    onItemPrevious: (ChecklistItem, String) -> Unit,
    clearFocusedItemPosition: () -> Unit,
    onShowCheckedClick: () -> Unit,
) {
    val isItemFocused = { item: ChecklistItem -> focusedItemPosition == item.position }
    val checkedItems = checklistItems.filter { it.checked }
    val showCheckedIconRotation by animateFloatAsState(if (showChecked) 0f else 180f)

    LazyColumn {
        items(
            items = checklistItems.filter { !it.checked },
            key = { it.id }
        ) { item ->
            ChecklistRow(
                modifier = modifier,
                item = item,
                focused = isItemFocused(item),
                selectionStart = itemSelectionStarts[item.id] ?: 0,
                onDeleteClick = { onItemDeleteClick(item) },
                onChange = { text, checked -> onItemChange(item, text, checked) },
                onNext = { head, tail -> onItemNext(item, head, tail) },
                onPrevious = { text -> onItemPrevious(item, text) },
                onFocus = clearFocusedItemPosition,
            )
        }
    }

    // Checked items:
    if (checkedItems.isNotEmpty()) {
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
            LazyColumn {
                items(items = checkedItems, key = { it.id }) { item ->
                    ChecklistRow(
                        modifier = modifier,
                        item = item,
                        focused = isItemFocused(item),
                        selectionStart = itemSelectionStarts[item.id] ?: 0,
                        onDeleteClick = { onItemDeleteClick(item) },
                        onChange = { text, checked -> onItemChange(item, text, checked) },
                        onNext = { head, tail -> onItemNext(item, head, tail) },
                        onPrevious = { text -> onItemPrevious(item, text) },
                        onFocus = clearFocusedItemPosition,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChecklistNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditChecklistNoteViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val showChecked by viewModel.showChecked.collectAsStateWithLifecycle()
    val checklistItems by viewModel.checklistItems.collectAsStateWithLifecycle(emptyList())
    var focusedItemPosition by rememberSaveable { mutableStateOf<Int?>(null) }
    val itemSelectionStarts = remember { mutableStateMapOf<UUID, Int>() }

    Scaffold(
        topBar = {
            NoteScreenTopAppBar {
                viewModel.save()
                onClose()
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding)) {
            TitleField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = {
                    viewModel.title.value = it
                },
                onNext = {
                    if (checklistItems.isEmpty()) {
                        viewModel.insertItem(text = "", checked = false, position = 0)
                        focusedItemPosition = 0
                    }
                }
            )

            Spacer(Modifier.height(4.dp))

            Checklist(
                checklistItems = checklistItems,
                focusedItemPosition = focusedItemPosition,
                itemSelectionStarts = itemSelectionStarts,
                showChecked = showChecked,
                onItemChange = { item, text, checked ->
                    viewModel.updateItem(item, text, checked, item.position)
                },
                onItemDeleteClick = { viewModel.deleteItem(it) },
                onItemNext = { item, head, tail ->
                    viewModel.insertItem(text = tail, checked = item.checked, position = item.position + 1)
                    focusedItemPosition = item.position + 1
                    itemSelectionStarts[item.id] = 0
                    viewModel.updateItem(item, head, item.checked, item.position)
                },
                onItemPrevious = { item, text ->
                    val itemIdx = checklistItems.indexOf(item)
                    if (itemIdx > 0) {
                        val previousItem = checklistItems[itemIdx - 1]
                        focusedItemPosition = previousItem.position
                        itemSelectionStarts[previousItem.id] = previousItem.text.length
                        if (text.isNotEmpty()) {
                            viewModel.updateItem(
                                item = previousItem,
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
                    viewModel.showChecked.value = !viewModel.showChecked.value
                },
            )

            Spacer(Modifier.height(4.dp))

            // "Add item" link:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val position = checklistItems.lastOrNull()?.let { it.position + 1 } ?: 0

                    viewModel.insertItem(text = "", checked = false, position = position)
                    focusedItemPosition = position
                }
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
}
