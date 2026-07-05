package app.atemkraft.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = OnNeon,
    secondary = NeonCyan,
    onSecondary = OnCyan,
    tertiary = NeonYellow,
    onTertiary = OnNeon,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnBackground,
)

private val LightColors = lightColorScheme(
    primary = NeonMagentaDark,
    onPrimary = LightSurface,
    secondary = NeonMagentaDark,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
)

@Composable
fun AtemkraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AtemkraftTypography,
        content = content,
    )
}
