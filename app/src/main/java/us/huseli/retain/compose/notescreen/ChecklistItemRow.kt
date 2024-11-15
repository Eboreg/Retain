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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import us.huseli.retain.dataclasses.uistate.ChecklistItemUiState

@Composable
fun ChecklistItemRow(
    state: ChecklistItemUiState,
    onFocusChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onNext: (TextFieldValue) -> Unit,
    reorderableState: ReorderableLazyListState,
    getAutocomplete: (String) -> List<ChecklistItem>,
    onAutocompleteSelect: (ChecklistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(state.text) { mutableStateOf(state.text) }
    var selection by remember(state.text) { mutableStateOf(TextRange(state.text.length)) }
    val textFieldValue by remember(text, selection) { mutableStateOf(TextFieldValue(text = text, selection = selection)) }
    val focusRequester = remember { FocusRequester() }
    val autocomplete = remember(state.text) { getAutocomplete(state.text) }
    var textFieldRect by remember { mutableStateOf(Rect.Zero) }

    Row(
        modifier = modifier.then(
            if (state.isDragging) Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                shape = ShapeDefaults.ExtraSmall,
            ) else Modifier
        ),
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
            checked = state.isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary.copy(alpha = state.alpha),
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = state.alpha),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = state.alpha),
            )
        )
        BasicTextField(
            value = textFieldValue,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = state.alpha)),
            textStyle = TextStyle.Default.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = state.alpha),
                fontSize = 16.sp,
            ),
            onValueChange = {
                text = it.text
                selection = it.selection
                if (it.text != state.text) onTextChange(it.text)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext(textFieldValue) }),
            modifier = Modifier
                .onFocusChanged {
                    Log.i(
                        "ChecklistNoteChecklistRow",
                        "onFocusChanged: ${textFieldValue.text}, .isFocused=${it.isFocused}"
                    )
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
                .onGloballyPositioned { if (state.isFocused) focusRequester.requestFocus() },
        )
        IconButton(
            onClick = onDeleteClick,
            content = { Icon(Icons.Sharp.Close, null) },
        )
    }

    if (state.showAutocomplete) {
        ChecklistAutocomplete(
            items = autocomplete,
            textFieldRect = { textFieldRect },
            onItemClick = onAutocompleteSelect,
            onDismissRequest = {},
        )
    }
}
