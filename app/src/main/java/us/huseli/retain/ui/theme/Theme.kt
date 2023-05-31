package us.huseli.retain.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightColors = lightColorScheme(
    primary = RetainColorLight.Primary,
    onPrimary = RetainColorLight.OnPrimary,
    primaryContainer = RetainColorLight.PrimaryContainer,
    onPrimaryContainer = RetainColorLight.OnPrimaryContainer,
    // primaryFixed = RetainColorLight.PrimaryFixed,
    // onPrimaryFixed = RetainColorLight.OnPrimaryFixed,
    // primaryFixedDim = RetainColorLight.PrimaryFixedDim,
    // onPrimaryFixedVariant = RetainColorLight.OnPrimaryFixedVariant,
    secondary = RetainColorLight.Secondary,
    onSecondary = RetainColorLight.OnSecondary,
    secondaryContainer = RetainColorLight.SecondaryContainer,
    onSecondaryContainer = RetainColorLight.OnSecondaryContainer,
    // secondaryFixed = RetainColorLight.SecondaryFixed,
    // onSecondaryFixed = RetainColorLight.OnSecondaryFixed,
    // secondaryFixedDim = RetainColorLight.SecondaryFixedDim,
    // onSecondaryFixedVariant = RetainColorLight.OnSecondaryFixedVariant,
    tertiary = RetainColorLight.Tertiary,
    onTertiary = RetainColorLight.OnTertiary,
    tertiaryContainer = RetainColorLight.TertiaryContainer,
    onTertiaryContainer = RetainColorLight.OnTertiaryContainer,
    // tertiaryFixed = RetainColorLight.TertiaryFixed,
    // onTertiaryFixed = RetainColorLight.OnTertiaryFixed,
    // tertiaryFixedDim = RetainColorLight.TertiaryFixedDim,
    // onTertiaryFixedVariant = RetainColorLight.OnTertiaryFixedVariant,
    error = RetainColorLight.Error,
    onError = RetainColorLight.OnError,
    errorContainer = RetainColorLight.ErrorContainer,
    onErrorContainer = RetainColorLight.OnErrorContainer,
    outline = RetainColorLight.Outline,
    background = RetainColorLight.Background,
    onBackground = RetainColorLight.OnBackground,
    surface = RetainColorLight.Surface,
    onSurface = RetainColorLight.OnSurface,
    surfaceVariant = RetainColorLight.SurfaceVariant,
    onSurfaceVariant = RetainColorLight.OnSurfaceVariant,
    inverseSurface = RetainColorLight.InverseSurface,
    inverseOnSurface = RetainColorLight.InverseOnSurface,
    inversePrimary = RetainColorLight.InversePrimary,
    surfaceTint = RetainColorLight.SurfaceTint,
    outlineVariant = RetainColorLight.OutlineVariant,
    scrim = RetainColorLight.Scrim,
    // surfaceContainerHighest = RetainColorLight.SurfaceContainerHighest,
    // surfaceContainerHigh = RetainColorLight.SurfaceContainerHigh,
    // surfaceContainer = RetainColorLight.SurfaceContainer,
    // surfaceContainerLow = RetainColorLight.SurfaceContainerLow,
    // surfaceContainerLowest = RetainColorLight.SurfaceContainerLowest,
    // surfaceBright = RetainColorLight.SurfaceBright,
    // surfaceDim = RetainColorLight.SurfaceDim,
)

private val DarkColors = darkColorScheme(
    primary = RetainColorDark.Primary,
    onPrimary = RetainColorDark.OnPrimary,
    primaryContainer = RetainColorDark.PrimaryContainer,
    onPrimaryContainer = RetainColorDark.OnPrimaryContainer,
    // primaryFixed = RetainColorDark.PrimaryFixed,
    // onPrimaryFixed = RetainColorDark.OnPrimaryFixed,
    // primaryFixedDim = RetainColorDark.PrimaryFixedDim,
    // onPrimaryFixedVariant = RetainColorDark.OnPrimaryFixedVariant,
    secondary = RetainColorDark.Secondary,
    onSecondary = RetainColorDark.OnSecondary,
    secondaryContainer = RetainColorDark.SecondaryContainer,
    onSecondaryContainer = RetainColorDark.OnSecondaryContainer,
    // secondaryFixed = RetainColorDark.SecondaryFixed,
    // onSecondaryFixed = RetainColorDark.OnSecondaryFixed,
    // secondaryFixedDim = RetainColorDark.SecondaryFixedDim,
    // onSecondaryFixedVariant = RetainColorDark.OnSecondaryFixedVariant,
    tertiary = RetainColorDark.Tertiary,
    onTertiary = RetainColorDark.OnTertiary,
    tertiaryContainer = RetainColorDark.TertiaryContainer,
    onTertiaryContainer = RetainColorDark.OnTertiaryContainer,
    // tertiaryFixed = RetainColorDark.TertiaryFixed,
    // onTertiaryFixed = RetainColorDark.OnTertiaryFixed,
    // tertiaryFixedDim = RetainColorDark.TertiaryFixedDim,
    // onTertiaryFixedVariant = RetainColorDark.OnTertiaryFixedVariant,
    error = RetainColorDark.Error,
    onError = RetainColorDark.OnError,
    errorContainer = RetainColorDark.ErrorContainer,
    onErrorContainer = RetainColorDark.OnErrorContainer,
    outline = RetainColorDark.Outline,
    background = RetainColorDark.Background,
    onBackground = RetainColorDark.OnBackground,
    surface = RetainColorDark.Surface,
    onSurface = RetainColorDark.OnSurface,
    surfaceVariant = RetainColorDark.SurfaceVariant,
    onSurfaceVariant = RetainColorDark.OnSurfaceVariant,
    inverseSurface = RetainColorDark.InverseSurface,
    inverseOnSurface = RetainColorDark.InverseOnSurface,
    inversePrimary = RetainColorDark.InversePrimary,
    surfaceTint = RetainColorDark.SurfaceTint,
    outlineVariant = RetainColorDark.OutlineVariant,
    scrim = RetainColorDark.Scrim,
    // surfaceContainerHighest = RetainColorDark.SurfaceContainerHighest,
    // surfaceContainerHigh = RetainColorDark.SurfaceContainerHigh,
    // surfaceContainer = RetainColorDark.SurfaceContainer,
    // surfaceContainerLow = RetainColorDark.SurfaceContainerLow,
    // surfaceContainerLowest = RetainColorDark.SurfaceContainerLowest,
    // surfaceBright = RetainColorDark.SurfaceBright,
    // surfaceDim = RetainColorDark.SurfaceDim,
)

@Suppress("unused")
@Composable
fun RetainThemeOld(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

@Composable
fun RetainTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) LightColors else DarkColors
    val systemUiController = rememberSystemUiController()

    systemUiController.setStatusBarColor(colors.surface)
    systemUiController.setNavigationBarColor(colors.background)

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
