package us.huseli.retain.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckBox
import androidx.compose.material.icons.sharp.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retain.R
import us.huseli.retain.dataclasses.NoteCardChecklistData

@Composable
fun NoteCardChecklist(data: NoteCardChecklistData) {
    Column {
        data.shownChecklistItems.forEach { item ->
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
        if (data.hiddenChecklistItemCount > 0) {
            val text = pluralStringResource(
                if (data.hiddenChecklistItemAllChecked) R.plurals.plus_x_checked_items
                else R.plurals.plus_x_items,
                data.hiddenChecklistItemCount,
                data.hiddenChecklistItemCount,
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