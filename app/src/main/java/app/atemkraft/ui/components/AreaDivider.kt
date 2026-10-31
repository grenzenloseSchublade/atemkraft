package app.atemkraft.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.atemkraft.ui.theme.Dimens

/**
 * Trenner zwischen zwei Bereichen einer Seite (kein Karten-Trenner, dafür `DividerSubtle`):
 * `BorderSection` in `outlineVariant`. Atmen-Tab unter „Muster des Tages ›“ (Einstieg | Übungen),
 * Tab „Meins“ vor den Tabs (Verlauf | Sammlungen; Eigentümer 2026-10-31: „die Grenze fehlt dort“).
 * Darüber und darunter sichtbar `SectionGap`, das setzt der Aufrufer.
 */
@Composable
fun AreaDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = Dimens.BorderSection, color = MaterialTheme.colorScheme.outlineVariant) // Abweichung KOMP-03: Bereichs-Trenner in der Haarlinie des Seitenkopfs, DividerSubtle war hier kaum zu sehen
}
