@file:Suppress("unused")

package us.huseli.retain.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object RetainColor {
    val Green = Color(0xFF008000)
    val Red = Color(220, 20, 60)

    val LightBackground = Color(0xFFFAFDFD)
    val LightError = Color(0xFFBA1A1A)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightInverseOnSurface = Color(0xFFEFF1F1)
    val LightInversePrimary = Color(0xFF85CFFF)
    val LightInverseSurface = Color(0xFF2E3132)
    val LightOnBackground = Color(0xFF191C1D)
    val LightOnError = Color(0xFFFFFFFF)
    val LightOnErrorContainer = Color(0xFF410002)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightOnPrimaryContainer = Color(0xFF001E2E)
    val LightOnPrimaryFixed = Color(0xFF001E2E)
    val LightOnPrimaryFixedVariant = Color(0xFF004C6C)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightOnSecondaryContainer = Color(0xFF0B1D29)
    val LightOnSecondaryFixed = Color(0xFF0B1D29)
    val LightOnSecondaryFixedVariant = Color(0xFF374955)
    val LightOnSurface = Color(0xFF191C1D)
    val LightOnSurfaceVariant = Color(0xFF3F484A)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightOnTertiaryContainer = Color(0xFF171E00)
    val LightOnTertiaryFixed = Color(0xFF171E00)
    val LightOnTertiaryFixedVariant = Color(0xFF3D4D00)
    val LightOutline = Color(0xFF6F797A)
    val LightOutlineVariant = Color(0xFFBFC8CA)
    val LightPrimary = Color(0xFF00658F)
    val LightPrimaryContainer = Color(0xFFC7E7FF)
    val LightPrimaryFixed = Color(0xFFC7E7FF)
    val LightPrimaryFixedDim = Color(0xFF85CFFF)
    val LightScrim = Color(0xFF000000)
    val LightSecondary = Color(0xFF4F616E)
    val LightSecondaryContainer = Color(0xFFD2E5F5)
    val LightSecondaryFixed = Color(0xFFD2E5F5)
    val LightSecondaryFixedDim = Color(0xFFB6C9D8)
    val LightShadow = Color(0xFF000000)
    val LightSurface = Color(0xFFF8FAFA)
    val LightSurfaceBright = Color(0xFFF8FAFA)
    val LightSurfaceContainer = Color(0xFFECEEEF)
    val LightSurfaceContainerHigh = Color(0xFFE6E8E9)
    val LightSurfaceContainerHighest = Color(0xFFE1E3E3)
    val LightSurfaceContainerLow = Color(0xFFF2F4F4)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceDim = Color(0xFFD8DADB)
    val LightSurfaceTint = Color(0xFF00658F)
    val LightSurfaceVariant = Color(0xFFDBE4E6)
    val LightTertiary = Color(0xFF526600)
    val LightTertiaryContainer = Color(0xFFD4ED7F)
    val LightTertiaryFixed = Color(0xFFD4ED7F)
    val LightTertiaryFixedDim = Color(0xFFB8D166)

    val DarkBackground = Color(0xFF191C1D)
    val DarkError = Color(0xFFFFB4AB)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkInverseOnSurface = Color(0xFF191C1D)
    val DarkInversePrimary = Color(0xFF00658F)
    val DarkInverseSurface = Color(0xFFE1E3E3)
    val DarkOnBackground = Color(0xFFE1E3E3)
    val DarkOnError = Color(0xFF690005)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)
    val DarkOnPrimary = Color(0xFF00344C)
    val DarkOnPrimaryContainer = Color(0xFFC7E7FF)
    val DarkOnPrimaryFixed = Color(0xFF001E2E)
    val DarkOnPrimaryFixedVariant = Color(0xFF004C6C)
    val DarkOnSecondary = Color(0xFF21323E)
    val DarkOnSecondaryContainer = Color(0xFFD2E5F5)
    val DarkOnSecondaryFixed = Color(0xFF0B1D29)
    val DarkOnSecondaryFixedVariant = Color(0xFF374955)
    val DarkOnSurface = Color(0xFFC4C7C7)
    val DarkOnSurfaceVariant = Color(0xFFBFC8CA)
    val DarkOnTertiary = Color(0xFF293500)
    val DarkOnTertiaryContainer = Color(0xFFD4ED7F)
    val DarkOnTertiaryFixed = Color(0xFF171E00)
    val DarkOnTertiaryFixedVariant = Color(0xFF3D4D00)
    val DarkOutline = Color(0xFF899294)
    val DarkOutlineVariant = Color(0xFF3F484A)
    val DarkPrimary = Color(0xFF85CFFF)
    val DarkPrimaryContainer = Color(0xFF004C6C)
    val DarkPrimaryFixed = Color(0xFFC7E7FF)
    val DarkPrimaryFixedDim = Color(0xFF85CFFF)
    val DarkScrim = Color(0xFF000000)
    val DarkSecondary = Color(0xFFB6C9D8)
    val DarkSecondaryContainer = Color(0xFF374955)
    val DarkSecondaryFixed = Color(0xFFD2E5F5)
    val DarkSecondaryFixedDim = Color(0xFFB6C9D8)
    val DarkShadow = Color(0xFF000000)
    val DarkSurface = Color(0xFF101415)
    val DarkSurfaceBright = Color(0xFF363A3A)
    val DarkSurfaceContainer = Color(0xFF1D2021)
    val DarkSurfaceContainerHigh = Color(0xFF272B2B)
    val DarkSurfaceContainerHighest = Color(0xFF323536)
    val DarkSurfaceContainerLow = Color(0xFF191C1D)
    val DarkSurfaceContainerLowest = Color(0xFF0B0F0F)
    val DarkSurfaceDim = Color(0xFF101415)
    val DarkSurfaceTint = Color(0xFF85CFFF)
    val DarkSurfaceVariant = Color(0xFF3F484A)
    val DarkTertiary = Color(0xFFB8D166)
    val DarkTertiaryContainer = Color(0xFF3D4D00)
    val DarkTertiaryFixed = Color(0xFFD4ED7F)
    val DarkTertiaryFixedDim = Color(0xFFB8D166)
}

val seed = Color(0xFF4E6C81)

val noteColorsDark = listOf(
    RetainColor.DarkBackground,
    Color(0xff77172e),
    Color(0xff692b17),
    Color(0xff7c4a03),
    Color(0xff264d3b),
    Color(0xff0c625d),
    Color(0xff256377),
    Color(0xff284255),
    Color(0xff472e5b),
    Color(0xff6c394f),
    Color(0xff4b443a),
    Color(0xff232427),
)

val noteColorsLight = listOf(
    RetainColor.LightBackground,
    Color(0xfffaafa8),
    Color(0xfff39f76),
    Color(0xfffff8b8),
    Color(0xffe2f6d4),
    Color(0xffb4ddd3),
    Color(0xffd4e4ed),
    Color(0xffaeccdc),
    Color(0xffd3bfdb),
    Color(0xfff6e2dd),
    Color(0xffe9e3d4),
    Color(0xffefeff1),
)

fun getNoteColors(context: Context): List<Color> =
    if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        noteColorsDark
    else noteColorsLight

fun getNoteColor(context: Context, index: Int): Color = getNoteColors(context)[index]

@Composable
fun getNoteColors(): List<Color> = if (isSystemInDarkTheme()) noteColorsDark else noteColorsLight

@Composable
fun getNoteColor(index: Int): Color = getNoteColors()[index]
