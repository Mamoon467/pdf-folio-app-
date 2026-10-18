package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = VermilionPrimaryDark,
    onPrimary = FolioDarkBg,
    primaryContainer = VermilionContainerDark,
    onPrimaryContainer = OnVermilionContainerDark,
    secondary = AmberHighlightDark,
    onSecondary = FolioDarkBg,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,
    tertiary = CobaltAccentDark,
    onTertiary = FolioDarkBg,
    tertiaryContainer = CobaltContainerDark,
    onTertiaryContainer = OnCobaltContainerDark,
    background = FolioDarkBg,
    onBackground = FolioInkLight,
    surface = FolioSurfaceDark,
    onSurface = FolioInkLight,
    surfaceVariant = FolioSurfaceVariantDark,
    onSurfaceVariant = FolioInkMutedDark,
    outline = FolioOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = VermilionPrimary,
    onPrimary = FolioSurfaceLight,
    primaryContainer = VermilionContainerLight,
    onPrimaryContainer = OnVermilionContainerLight,
    secondary = AmberHighlight,
    onSecondary = FolioSurfaceLight,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = OnAmberContainerLight,
    tertiary = CobaltAccent,
    onTertiary = FolioSurfaceLight,
    tertiaryContainer = CobaltContainerLight,
    onTertiaryContainer = OnCobaltContainerLight,
    background = FolioPaperBg,
    onBackground = FolioInkDark,
    surface = FolioSurfaceLight,
    onSurface = FolioInkDark,
    surfaceVariant = FolioSurfaceVariantLight,
    onSurfaceVariant = FolioInkMuted,
    outline = FolioOutlineLight
)

val FolioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FolioShapes,
        content = content
    )
}
