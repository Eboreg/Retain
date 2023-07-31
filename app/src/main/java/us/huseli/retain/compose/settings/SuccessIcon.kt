package us.huseli.retain.compose.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.retaintheme.ui.theme.RetainColorDark
import us.huseli.retaintheme.ui.theme.RetainColorLight

@Composable
fun SuccessIcon(modifier: Modifier = Modifier, circled: Boolean = false) {
    val colors = if (isSystemInDarkTheme()) RetainColorDark else RetainColorLight

    Icon(
        modifier = modifier,
        imageVector = if (circled) Icons.Sharp.CheckCircle else Icons.Sharp.Check,
        contentDescription = null,
        tint = colors.Green,
    )
}
