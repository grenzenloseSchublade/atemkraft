package app.atemkraft.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Bewusst NUR Dark: Die Synthwave-Palette ist für abendliche Nutzung entworfen; ein helles
 * Schema mit Neon-Akzenten fiele beim Kontrast (AA) durch. Material You/dynamicColor ist
 * ebenso bewusst ausgelassen – die Marke lebt von der festen Palette.
 */
private val AtemkraftColors = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = OnNeon,
    secondary = NeonCyan,
    onSecondary = OnCyan,
    tertiary = NeonYellow,
    onTertiary = OnNeon,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    // M3-Semantik: „variant" = leiser – gedämpftes Lavendel statt Voll-Weiß.
    onSurfaceVariant = DarkOnSurfaceVariant,
    // M3-Container-Leiter aus der Indigo-Palette: Card() & Co. rendern sonst im neutralen
    // Baseline-Grau statt im Marken-Indigo.
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceVariant,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = DarkOnBackground,
    outlineVariant = OutlineVariantIndigo,
)

@Composable
fun AtemkraftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtemkraftColors,
        typography = AtemkraftTypography,
        content = content,
    )
}
