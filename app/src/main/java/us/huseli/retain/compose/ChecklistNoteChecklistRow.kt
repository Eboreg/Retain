package us.huseli.retain.compose

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import us.huseli.retain.data.entities.ChecklistItem
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
fun ChecklistNoteChecklistRow(
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
                /**
                 * If the new TextFieldValue does not start with the null
                 * character, that must mean the user has just erased it by
                 * inputting a backspace at the beginning of the field. In that
                 * case, join this row with the one above. If this is the first
                 * row: just re-insert the null character move the selection
                 * start to after it.
                 */
                /**
                 * If the new TextFieldValue does not start with the null
                 * character, that must mean the user has just erased it by
                 * inputting a backspace at the beginning of the field. In that
                 * case, join this row with the one above. If this is the first
                 * row: just re-insert the null character move the selection
                 * start to after it.
                 */

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
