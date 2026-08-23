package app.atemkraft.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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

/**
 * Weiche, moderne Formsprache: großzügige Rundungen (ruhig, freundlich – passend zur
 * Yoga-/Abend-Stimmung). Bewusst KEINE geschnittenen Ecken – die wirkten altbacken.
 */
private val AtemkraftShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AtemkraftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtemkraftColors,
        typography = AtemkraftTypography,
        shapes = AtemkraftShapes,
        content = content,
    )
}
