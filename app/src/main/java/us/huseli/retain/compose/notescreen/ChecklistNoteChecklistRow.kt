package us.huseli.retain.compose.notescreen

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

@Composable
fun ChecklistNoteChecklistRow(
    modifier: Modifier = Modifier,
    isFocused: Boolean,
    isDragging: Boolean,
    textFieldValue: TextFieldValue,
    checked: Boolean,
    onFocus: () -> Unit,
    onDeleteClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    reorderableState: ReorderableLazyListState,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
) {
    val alpha = if (checked) 0.5f else 1f
    val focusRequester = remember { FocusRequester() }

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
            onValueChange = onTextFieldValueChange,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() }
            ),
            modifier = Modifier
                .onFocusChanged { if (it.isFocused) onFocus() }
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { if (isFocused) focusRequester.requestFocus() },
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Sharp.Close,
                contentDescription = null
            )
        }
    }
}
