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
    primary = RetainColor.LightPrimary,
    onPrimary = RetainColor.LightOnPrimary,
    primaryContainer = RetainColor.LightPrimaryContainer,
    onPrimaryContainer = RetainColor.LightOnPrimaryContainer,
    // primaryFixed = RetainColor.LightPrimaryFixed,
    // onPrimaryFixed = RetainColor.LightOnPrimaryFixed,
    // primaryFixedDim = RetainColor.LightPrimaryFixedDim,
    // onPrimaryFixedVariant = RetainColor.LightOnPrimaryFixedVariant,
    secondary = RetainColor.LightSecondary,
    onSecondary = RetainColor.LightOnSecondary,
    secondaryContainer = RetainColor.LightSecondaryContainer,
    onSecondaryContainer = RetainColor.LightOnSecondaryContainer,
    // secondaryFixed = RetainColor.LightSecondaryFixed,
    // onSecondaryFixed = RetainColor.LightOnSecondaryFixed,
    // secondaryFixedDim = RetainColor.LightSecondaryFixedDim,
    // onSecondaryFixedVariant = RetainColor.LightOnSecondaryFixedVariant,
    tertiary = RetainColor.LightTertiary,
    onTertiary = RetainColor.LightOnTertiary,
    tertiaryContainer = RetainColor.LightTertiaryContainer,
    onTertiaryContainer = RetainColor.LightOnTertiaryContainer,
    // tertiaryFixed = RetainColor.LightTertiaryFixed,
    // onTertiaryFixed = RetainColor.LightOnTertiaryFixed,
    // tertiaryFixedDim = RetainColor.LightTertiaryFixedDim,
    // onTertiaryFixedVariant = RetainColor.LightOnTertiaryFixedVariant,
    error = RetainColor.LightError,
    onError = RetainColor.LightOnError,
    errorContainer = RetainColor.LightErrorContainer,
    onErrorContainer = RetainColor.LightOnErrorContainer,
    outline = RetainColor.LightOutline,
    background = RetainColor.LightBackground,
    onBackground = RetainColor.LightOnBackground,
    surface = RetainColor.LightSurface,
    onSurface = RetainColor.LightOnSurface,
    surfaceVariant = RetainColor.LightSurfaceVariant,
    onSurfaceVariant = RetainColor.LightOnSurfaceVariant,
    inverseSurface = RetainColor.LightInverseSurface,
    inverseOnSurface = RetainColor.LightInverseOnSurface,
    inversePrimary = RetainColor.LightInversePrimary,
    surfaceTint = RetainColor.LightSurfaceTint,
    outlineVariant = RetainColor.LightOutlineVariant,
    scrim = RetainColor.LightScrim,
    // surfaceContainerHighest = RetainColor.LightSurfaceContainerHighest,
    // surfaceContainerHigh = RetainColor.LightSurfaceContainerHigh,
    // surfaceContainer = RetainColor.LightSurfaceContainer,
    // surfaceContainerLow = RetainColor.LightSurfaceContainerLow,
    // surfaceContainerLowest = RetainColor.LightSurfaceContainerLowest,
    // surfaceBright = RetainColor.LightSurfaceBright,
    // surfaceDim = RetainColor.LightSurfaceDim,
)

private val DarkColors = darkColorScheme(
    primary = RetainColor.DarkPrimary,
    onPrimary = RetainColor.DarkOnPrimary,
    primaryContainer = RetainColor.DarkPrimaryContainer,
    onPrimaryContainer = RetainColor.DarkOnPrimaryContainer,
    // primaryFixed = RetainColor.DarkPrimaryFixed,
    // onPrimaryFixed = RetainColor.DarkOnPrimaryFixed,
    // primaryFixedDim = RetainColor.DarkPrimaryFixedDim,
    // onPrimaryFixedVariant = RetainColor.DarkOnPrimaryFixedVariant,
    secondary = RetainColor.DarkSecondary,
    onSecondary = RetainColor.DarkOnSecondary,
    secondaryContainer = RetainColor.DarkSecondaryContainer,
    onSecondaryContainer = RetainColor.DarkOnSecondaryContainer,
    // secondaryFixed = RetainColor.DarkSecondaryFixed,
    // onSecondaryFixed = RetainColor.DarkOnSecondaryFixed,
    // secondaryFixedDim = RetainColor.DarkSecondaryFixedDim,
    // onSecondaryFixedVariant = RetainColor.DarkOnSecondaryFixedVariant,
    tertiary = RetainColor.DarkTertiary,
    onTertiary = RetainColor.DarkOnTertiary,
    tertiaryContainer = RetainColor.DarkTertiaryContainer,
    onTertiaryContainer = RetainColor.DarkOnTertiaryContainer,
    // tertiaryFixed = RetainColor.DarkTertiaryFixed,
    // onTertiaryFixed = RetainColor.DarkOnTertiaryFixed,
    // tertiaryFixedDim = RetainColor.DarkTertiaryFixedDim,
    // onTertiaryFixedVariant = RetainColor.DarkOnTertiaryFixedVariant,
    error = RetainColor.DarkError,
    onError = RetainColor.DarkOnError,
    errorContainer = RetainColor.DarkErrorContainer,
    onErrorContainer = RetainColor.DarkOnErrorContainer,
    outline = RetainColor.DarkOutline,
    background = RetainColor.DarkBackground,
    onBackground = RetainColor.DarkOnBackground,
    surface = RetainColor.DarkSurface,
    onSurface = RetainColor.DarkOnSurface,
    surfaceVariant = RetainColor.DarkSurfaceVariant,
    onSurfaceVariant = RetainColor.DarkOnSurfaceVariant,
    inverseSurface = RetainColor.DarkInverseSurface,
    inverseOnSurface = RetainColor.DarkInverseOnSurface,
    inversePrimary = RetainColor.DarkInversePrimary,
    surfaceTint = RetainColor.DarkSurfaceTint,
    outlineVariant = RetainColor.DarkOutlineVariant,
    scrim = RetainColor.DarkScrim,
    // surfaceContainerHighest = RetainColor.DarkSurfaceContainerHighest,
    // surfaceContainerHigh = RetainColor.DarkSurfaceContainerHigh,
    // surfaceContainer = RetainColor.DarkSurfaceContainer,
    // surfaceContainerLow = RetainColor.DarkSurfaceContainerLow,
    // surfaceContainerLowest = RetainColor.DarkSurfaceContainerLowest,
    // surfaceBright = RetainColor.DarkSurfaceBright,
    // surfaceDim = RetainColor.DarkSurfaceDim,
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
