package us.huseli.retain.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import us.huseli.retaintheme.ui.theme.RetainBasicColorsDark
import us.huseli.retaintheme.ui.theme.RetainBasicColorsLight

enum class NoteColorKey { DEFAULT, RED, ORANGE, YELLOW, GREEN, TEAL, BLUE, CERULEAN, PURPLE, PINK, BROWN, GRAY }

fun noteColors(dark: Boolean): Map<NoteColorKey, Color> {
    val colorScheme = if (dark) RetainBasicColorsDark else RetainBasicColorsLight

    return mapOf(
        NoteColorKey.RED to colorScheme.Red,
        NoteColorKey.ORANGE to colorScheme.Orange,
        NoteColorKey.YELLOW to colorScheme.Yellow,
        NoteColorKey.GREEN to colorScheme.Green,
        NoteColorKey.TEAL to colorScheme.Teal,
        NoteColorKey.BLUE to colorScheme.Blue,
        NoteColorKey.CERULEAN to colorScheme.Cerulean,
        NoteColorKey.PURPLE to colorScheme.Purple,
        NoteColorKey.PINK to colorScheme.Pink,
        NoteColorKey.BROWN to colorScheme.Brown,
        NoteColorKey.GRAY to colorScheme.Gray,
    )
}

@Composable
fun getNoteColors(): Map<NoteColorKey, Color> {
    val dark = isSystemInDarkTheme()
    return remember(dark) { noteColors(dark) }
}

fun getNoteColor(key: NoteColorKey?, dark: Boolean): Color? = noteColors(dark)[key]

@Composable
fun getNoteColorVariant(key: NoteColorKey, default: Color): Color {
    val dark = isSystemInDarkTheme()
    return remember(key, dark, default) { getNoteColor(key, dark)?.darken() ?: default }
}

@Composable
fun getNoteColor(key: NoteColorKey, default: Color): Color {
    val dark = isSystemInDarkTheme()
    return remember(key, dark, default) { getNoteColor(key, dark) ?: default }
}

fun Color.darken() = copy(
    red = (red - 0.05f).coerceAtLeast(0f),
    green = (green - 0.05f).coerceAtLeast(0f),
    blue = (blue - 0.05f).coerceAtLeast(0f),
)

fun String.colorKeyOrNull(): NoteColorKey? {
    return try {
        NoteColorKey.valueOf(this)
    } catch (_: Throwable) {
        null
    }
}

fun Context.getNoteColor(key: NoteColorKey): Color? {
    val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    return getNoteColor(key, dark)
}
