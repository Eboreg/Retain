package us.huseli.retain.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import us.huseli.retain.R
import us.huseli.retain.data.entities.ChecklistItem
import java.util.UUID

fun ChecklistNoteChecklist(
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
            ChecklistNoteChecklistRow(
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
                    ChecklistNoteChecklistRow(
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
