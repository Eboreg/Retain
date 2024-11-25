package us.huseli.retain.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import us.huseli.retain.Enums
import us.huseli.retain.R
import us.huseli.retain.annotation.RetainAnnotatedText
import us.huseli.retain.dataclasses.NotePojo
import us.huseli.retain.ui.theme.getNoteColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    pojo: NotePojo,
    isSelected: Boolean,
    isDragging: Boolean,
    secondaryImageGridRowHeight: Dp = 100.dp,
    scope: ReorderableCollectionItemScope,
    onDragEnd: () -> Unit,
) {
    val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
    val noteColor = getNoteColor(pojo.note.colorKey, MaterialTheme.colorScheme.background)
    val border =
        if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))

    Box {
        OutlinedCard(
            border = border,
            shape = ShapeDefaults.ExtraSmall,
            colors = CardDefaults.outlinedCardColors(containerColor = noteColor),
            modifier = modifier
                .shadow(elevation)
                .heightIn(min = 50.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoteImageGrid(
                    images = pojo.images,
                    maxRows = 2,
                    secondaryRowHeight = secondaryImageGridRowHeight,
                )

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    if (pojo.note.title.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            modifier = Modifier.padding(end = if (pojo.images.isEmpty()) 24.dp else 0.dp),
                            text = pojo.note.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    when (pojo.note.type) {
                        Enums.NoteType.TEXT -> {
                            if (pojo.note.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(if (pojo.note.title.isNotBlank()) 8.dp else 16.dp))
                                RetainAnnotatedText(
                                    text = pojo.note.annotatedText,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 6,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                )
                            }
                            if (pojo.note.text.isNotBlank() || pojo.note.title.isNotBlank())
                                Spacer(modifier = Modifier.height(16.dp))
                        }

                        Enums.NoteType.CHECKLIST -> {
                            val checklistData = pojo.getCardChecklist()

                            if (checklistData.shownChecklistItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(if (pojo.note.title.isNotBlank()) 8.dp else 16.dp))
                                NoteCardChecklist(data = checklistData)
                            }
                            if (checklistData.shownChecklistItems.isNotEmpty() || pojo.note.title.isNotBlank())
                                Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        with(scope) {
            FilledTonalIconButton(
                modifier = Modifier
                    .draggableHandle(onDragStopped = onDragEnd)
                    .align(Alignment.TopEnd),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = noteColor.copy(alpha = 0.25f)),
                onClick = {},
            ) { Icon(Icons.Sharp.DragIndicator, stringResource(R.string.move_card)) }
        }
    }
}
