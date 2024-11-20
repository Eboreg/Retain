package us.huseli.retain.compose.notescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckBox
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import us.huseli.retain.dataclasses.uistate.IChecklistItemUiState

@Composable
fun ChecklistAutocomplete(
    items: List<IChecklistItemUiState>,
    textFieldRect: () -> Rect,
    onItemClick: (IChecklistItemUiState) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Popup(
        properties = PopupProperties(dismissOnClickOutside = true),
        onDismissRequest = onDismissRequest,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val rect = textFieldRect().roundToIntRect()
                val availableHeight = windowSize.height - rect.bottom
                return if (availableHeight > popupContentSize.height + 20) rect.bottomLeft + IntOffset(0, 20)
                else IntOffset(x = rect.left, y = rect.top - popupContentSize.height - 20)
            }
        }
    ) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(modifier = Modifier.padding(5.dp)) {
                for (item in items) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .clickable(
                                onClick = { onItemClick(item) },
                                indication = ripple(),
                                interactionSource = remember { MutableInteractionSource() },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Sharp.CheckBox, null)
                        Text(item.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
