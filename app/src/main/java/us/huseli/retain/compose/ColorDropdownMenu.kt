package us.huseli.retain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FormatColorReset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.retain.R
import us.huseli.retain.ui.theme.NoteColorKey

@Composable
fun ColorCircle(modifier: Modifier = Modifier, color: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f, true)
            .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorDropdownMenu(
    modifier: Modifier = Modifier,
    colors: Map<NoteColorKey, Color>,
    isExpanded: Boolean,
    width: Dp = 200.dp,
    circleHeight: Dp = 50.dp,
    padding: Dp = 5.dp,
    onDismiss: () -> Unit,
    onColorClick: (NoteColorKey) -> Unit,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = isExpanded,
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(stringResource(R.string.select_note_colour), modifier = Modifier.padding(horizontal = padding))
            FlowRow(modifier = Modifier.width(width)) {
                Surface(
                    shape = CircleShape,
                    modifier = modifier
                        .height(circleHeight)
                        .padding(padding)
                        .aspectRatio(1f, true)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { onColorClick(NoteColorKey.DEFAULT) },
                    content = { Icon(Icons.Sharp.FormatColorReset, null) },
                )
                colors.forEach { (key, color) ->
                    ColorCircle(
                        modifier = Modifier.height(circleHeight).padding(padding),
                        color = color,
                        onClick = { onColorClick(key) }
                    )
                }
            }
        }
    }
}
