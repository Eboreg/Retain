package us.huseli.retain.compose.notescreen

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.DragIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import us.huseli.retain.dataclasses.entities.ChecklistItem

@Composable
fun ChecklistNoteChecklistRow(
    modifier: Modifier = Modifier,
    item: ChecklistItem,
    isFocused: Boolean,
    isDragging: Boolean,
    checked: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onNext: (TextFieldValue) -> Unit,
    reorderableState: ReorderableLazyListState,
    getAutocomplete: (String) -> List<ChecklistItem>,
    onAutocompleteSelect: (ChecklistItem) -> Unit,
) {
    val alpha = remember(checked) { if (checked) 0.5f else 1f }
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val autocomplete = remember(textFieldValue) { getAutocomplete(textFieldValue.text) }
    var textFieldRect by remember { mutableStateOf(Rect.Zero) }
    val showAutocomplete = remember(isFocused, isDragging, checked) { isFocused && !isDragging && !checked }

    LaunchedEffect(item.text) {
        textFieldValue = textFieldValue.copy(text = item.text)
    }

    val realModifier =
        if (isDragging) modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            shape = ShapeDefaults.ExtraSmall,
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
                textFieldValue = it
                onTextChange(it.text)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext(textFieldValue) }),
            modifier = Modifier
                .onFocusChanged {
                    Log.i("ChecklistNoteChecklistRow", "onFocusChanged: ${item.text}, .isFocused=${it.isFocused}")
                    onFocusChange(it.isFocused)
                }
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPlaced { coords ->
                    val bounds = coords.boundsInWindow()

                    Log.i(
                        "ChecklistNoteChecklistRow",
                        "boundsInParent=${coords.boundsInParent()}, boundsInRoot=${coords.boundsInRoot()}, boundsInWindow=${coords.boundsInWindow()}, size=${coords.size}"
                    )
                    if (!bounds.isEmpty) textFieldRect = bounds
                }
                .onGloballyPositioned { if (isFocused) focusRequester.requestFocus() },
        )
        IconButton(
            onClick = onDeleteClick,
            content = { Icon(Icons.Sharp.Close, null) },
        )
    }

    if (showAutocomplete) {
        ChecklistAutocomplete(
            items = autocomplete,
            textFieldRect = { textFieldRect },
            onItemClick = {
                textFieldValue = TextFieldValue(text = it.text, selection = TextRange(it.text.length))
                onAutocompleteSelect(it)
            },
            onDismissRequest = {},
        )
    }
}
