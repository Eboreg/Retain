package us.huseli.retain.compose.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Error
import androidx.compose.material.icons.sharp.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.retaintheme.ui.theme.RetainBasicColorsDark
import us.huseli.retaintheme.ui.theme.RetainBasicColorsLight

@Composable
fun FailIcon(modifier: Modifier = Modifier, circled: Boolean = false) {
    val colors = if (isSystemInDarkTheme()) RetainBasicColorsDark else RetainBasicColorsLight

    Icon(
        modifier = modifier,
        imageVector = if (circled) Icons.Sharp.Error else Icons.Sharp.PriorityHigh,
        contentDescription = null,
        tint = colors.Red,
    )
}
