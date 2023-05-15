package us.huseli.retain.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retain.R

@Composable
fun FAB(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddTextNoteClick: () -> Unit,
    onAddChecklistClick: () -> Unit,
    onClose: () -> Unit,
) {
    if (expanded) {
        ExtendedFloatingActionButton(
            onClick = {
                onAddTextNoteClick()
                onClose()
            },
            shape = CircleShape,
            text = { Text(stringResource(R.string.add_text_note)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.EditNote,
                    contentDescription = null,
                )
            },
            modifier = modifier.padding(bottom = 8.dp),
        )
        ExtendedFloatingActionButton(
            onClick = {
                onAddChecklistClick()
                onClose()
            },
            shape = CircleShape,
            text = { Text(stringResource(R.string.add_checklist)) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Checklist,
                    contentDescription = null,
                )
            },
            modifier = modifier.padding(bottom = 8.dp),
        )
    }
    FloatingActionButton(
        onClick = { onExpandedChange(!expanded) },
        shape = CircleShape,
        modifier = modifier,
    ) {
        val rotation by animateFloatAsState(if (expanded) 45f else 0f)

        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
    }
}