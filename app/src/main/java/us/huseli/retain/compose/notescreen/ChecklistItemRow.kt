package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import us.huseli.retain.annotation.AnnotatedTextField
import us.huseli.retain.annotation.RetainAnnotatedString
import us.huseli.retain.annotation.RetainAnnotatedStringState
import us.huseli.retain.interfaces.IChecklistItem

@Composable
fun LazyItemScope.ChecklistItemRow(
    state: IChecklistItem,
    annotatedStringState: RetainAnnotatedStringState,
    onFocusChange: (FocusState) -> Unit,
    onDeleteClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    reorderableState: ReorderableLazyListState,
    getAutocomplete: (String) -> List<IChecklistItem>,
    onAutocompleteSelect: (IChecklistItem) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onValueChange: (RetainAnnotatedString) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val autocomplete = remember(state.annotatedText.text) { getAutocomplete(state.annotatedText.text) }
    var textFieldRect by remember { mutableStateOf(Rect.Zero) }

    ReorderableItem(reorderableState, key = state.id) { isDragging ->
        Row(
            modifier = modifier.then(
                if (state.isDragging) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    shape = ShapeDefaults.ExtraSmall,
                ) else Modifier
            ),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .draggableHandle(onDragStarted = { onDragStart() }, onDragStopped = onDragEnd)
                    .width(30.dp)
                    .padding(start = 8.dp)
            ) {
                Icon(imageVector = Icons.Sharp.DragIndicator, contentDescription = null)
            }
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
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                AnnotatedTextField(
                    state = annotatedStringState,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = state.alpha)),
                    textStyle = TextStyle.Default.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = state.alpha),
                        fontSize = 16.sp,
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    onValueChange = onValueChange,
                    keyboardActions = KeyboardActions(onNext = { onNext() }),
                    modifier = Modifier
                        .onFocusChanged(onFocusChange)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPlaced { coords ->
                            val bounds = coords.boundsInWindow()
                            if (!bounds.isEmpty) textFieldRect = bounds
                        }
                        .onGloballyPositioned { if (state.isFocused) focusRequester.requestFocus() }
                )
            }
            IconButton(
                onClick = onDeleteClick,
                content = { Icon(Icons.Sharp.Close, null) },
            )
        }
    }

    if (state.showAutocomplete && autocomplete.isNotEmpty()) {
        ChecklistAutocomplete(
            items = autocomplete,
            textFieldRect = { textFieldRect },
            onItemClick = onAutocompleteSelect,
            onDismissRequest = {},
        )
    }
}
