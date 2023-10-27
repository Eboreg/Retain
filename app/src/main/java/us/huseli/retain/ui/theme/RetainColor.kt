package us.huseli.retain.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import us.huseli.retaintheme.ui.theme.RetainBasicColors
import us.huseli.retaintheme.ui.theme.RetainBasicColorsDark
import us.huseli.retaintheme.ui.theme.RetainBasicColorsLight
import kotlin.math.max

val noteColors: (RetainBasicColors, Color) -> Map<String, Color> = { colorScheme, default ->
    mapOf(
        "DEFAULT" to default,
        "RED" to colorScheme.Red,
        "ORANGE" to colorScheme.Orange,
        "YELLOW" to colorScheme.Yellow,
        "GREEN" to colorScheme.Green,
        "TEAL" to colorScheme.Teal,
        "BLUE" to colorScheme.Blue,
        "CERULEAN" to colorScheme.Cerulean,
        "PURPLE" to colorScheme.Purple,
        "PINK" to colorScheme.Pink,
        "BROWN" to colorScheme.Brown,
        "GRAY" to colorScheme.Gray,
    )
}

@Composable
fun getNoteColors(default: Color): Map<String, Color> =
    noteColors(if (isSystemInDarkTheme()) RetainBasicColorsDark else RetainBasicColorsLight, default)

fun getNoteColor(key: String, dark: Boolean, default: Color): Color {
    val colorScheme = if (dark) RetainBasicColorsDark else RetainBasicColorsLight
    return noteColors(colorScheme, default).getOrDefault(key, default)
}

fun getNoteColor(context: Context, key: String, default: Color) = getNoteColor(
    key = key,
    dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
    default = default,
)

@Composable
fun getNoteColor(key: String, default: Color): Color = getNoteColor(key, isSystemInDarkTheme(), default)

fun getAppBarColor(key: String, dark: Boolean, default: Color): Color {
    return if (key == "DEFAULT") default
    else getNoteColor(key, dark, default).let {
        it.copy(
            red = max(it.red - 0.05f, 0f),
            green = max(it.green - 0.05f, 0f),
            blue = max(it.blue - 0.05f, 0f),
        )
    }
}

fun getAppBarColor(context: Context, key: String, default: Color) = getAppBarColor(
    key = key,
    dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
    default = default,
)
