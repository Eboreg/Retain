@file:Suppress("AnimateAsStateLabel")

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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import us.huseli.retain.R
import us.huseli.retain.dataclasses.entities.ChecklistItem
import java.util.UUID

fun LazyListScope.ChecklistNoteChecklist(
    modifier: Modifier = Modifier,
    state: ReorderableLazyListState,
    focusedItemId: UUID?,
    showChecked: Boolean,
    uncheckedItems: List<ChecklistItem>,
    checkedItems: List<ChecklistItem>,
    onItemDeleteClick: (ChecklistItem) -> Unit,
    onItemCheckedChange: (ChecklistItem, Boolean) -> Unit,
    onItemTextChange: (ChecklistItem, String) -> Unit,
    onNextItem: (ChecklistItem, TextFieldValue) -> Unit,
    onItemFocus: (ChecklistItem) -> Unit,
    onShowCheckedClick: () -> Unit,
    backgroundColor: Color,
) {
    items(uncheckedItems, key = { it.id }) { item ->
        ReorderableItem(state, key = item.id) { isDragging ->
            ChecklistNoteChecklistRow(
                item = item,
                modifier = modifier.background(backgroundColor),
                isFocused = focusedItemId == item.id,
                isDragging = isDragging,
                checked = item.checked,
                onFocus = { onItemFocus(item) },
                onDeleteClick = { onItemDeleteClick(item) },
                onCheckedChange = { onItemCheckedChange(item, it) },
                onNext = { onNextItem(item, it) },
                reorderableState = state,
                onTextChange = { onItemTextChange(item, it) },
            )
        }
    }

    // Checked items:
    if (checkedItems.isNotEmpty()) {
        item {
            val showCheckedIconRotation by animateFloatAsState(if (showChecked) 0f else 180f)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .clickable { onShowCheckedClick() },
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
            items(checkedItems, key = { it.id }) { item ->
                ReorderableItem(state, key = item.id) { isDragging ->
                    ChecklistNoteChecklistRow(
                        item = item,
                        modifier = modifier.background(backgroundColor),
                        isFocused = focusedItemId == item.id,
                        isDragging = isDragging,
                        checked = item.checked,
                        onFocus = { onItemFocus(item) },
                        onDeleteClick = { onItemDeleteClick(item) },
                        onCheckedChange = { onItemCheckedChange(item, it) },
                        onNext = { onNextItem(item, it) },
                        reorderableState = state,
                        onTextChange = { onItemTextChange(item, it) },
                    )
                }
            }
        } else {
            item {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
