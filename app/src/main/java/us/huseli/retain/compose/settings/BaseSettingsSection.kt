package us.huseli.retain.compose.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import us.huseli.retain.viewmodels.SettingsViewModel

@Composable
fun BaseSettingsSection(
    modifier: Modifier = Modifier,
    key: String,
    title: String,
    viewModel: SettingsViewModel,
    subtitle: String? = null,
    isShownDefault: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isShown by viewModel.getSectionShown(key, isShownDefault)
    val showIconRotation by animateFloatAsState(if (isShown) 0f else 180f)

    Column(modifier = modifier.padding(bottom = 16.dp).padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            IconButton(onClick = { viewModel.toggleSectionShown(key) }) {
                Icon(
                    imageVector = Icons.Sharp.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(showIconRotation),
                )
            }
        }
        if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        if (isShown) content()
    }
}
