package us.huseli.retain.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckBox
import androidx.compose.material.icons.sharp.CheckBoxOutlineBlank
import androidx.compose.material.icons.sharp.DragIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import us.huseli.retain.Enums
import us.huseli.retain.R
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import us.huseli.retain.ui.theme.getNoteColor
import kotlin.math.min


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    note: Note,
    checklistItems: List<ChecklistItem>,
    bitmapImages: List<BitmapImage>,
    isSelected: Boolean,
    reorderableState: ReorderableLazyListState? = null,
    showDragHandle: Boolean = false,
) {
    val noteColor = getNoteColor(note.colorIdx)
    val border =
        if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))

    Box {
        OutlinedCard(
            border = border,
            shape = ShapeDefaults.ExtraSmall,
            colors = CardDefaults.outlinedCardColors(containerColor = noteColor),
            modifier = modifier
                .heightIn(min = 50.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoteImageGrid(
                    bitmapImages = bitmapImages,
                    showDeleteButton = false,
                    maxRows = 2,
                    secondaryRowHeight = 100.dp,
                )

                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    if (note.title.isNotBlank()) {
                        Text(
                            modifier = if (showDragHandle) Modifier.padding(end = 24.dp) else Modifier,
                            text = note.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    when (note.type) {
                        Enums.NoteType.TEXT -> {
                            if (note.text.isNotBlank()) {
                                if (note.title.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = note.text,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 6,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                )
                            }
                        }

                        Enums.NoteType.CHECKLIST -> {
                            NoteCardChecklist(note = note, checklistItems = checklistItems)
                        }
                    }
                }
            }
        }

        if (showDragHandle && reorderableState != null) {
            FilledTonalIconButton(
                modifier = Modifier
                    .detectReorder(reorderableState)
                    .align(Alignment.TopEnd),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (bitmapImages.isEmpty()) noteColor else Color.Transparent
                ),
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Sharp.DragIndicator,
                    contentDescription = stringResource(R.string.move_card),
                )
            }
        }
    }
}


@Composable
fun NoteCardChecklist(
    note: Note,
    checklistItems: List<ChecklistItem>,
) {
    val filteredItems = if (!note.showChecked) checklistItems.filter { !it.checked } else checklistItems
    val shownItems = checklistItems.subList(0, min(filteredItems.size, 5))
    val hiddenItemCount = checklistItems.size - shownItems.size

    if (note.title.isNotBlank() && checklistItems.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
    }

    Column {
        shownItems.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Icon(
                    imageVector = if (item.checked) Icons.Sharp.CheckBox else Icons.Sharp.CheckBoxOutlineBlank,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
                Text(
                    text = item.text,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }

        // "+ X items"
        if (hiddenItemCount > 0) {
            val text = pluralStringResource(
                if (checklistItems.minus(shownItems.toSet()).all { it.checked }) R.plurals.plus_x_checked_items
                else R.plurals.plus_x_items,
                hiddenItemCount,
                hiddenItemCount,
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
