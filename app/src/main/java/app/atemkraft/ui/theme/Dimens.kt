package app.atemkraft.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Zentrale Abstands-Tokens (4-dp-Raster). Quelle der Wahrheit für Layout-Rhythmus – neue Screens
 * greifen hierauf zu statt auf Streu-Literale. Die Werte entsprechen dem etablierten Muster der
 * bestehenden Screens.
 */
object Dimens {
    /** Horizontaler Screen-Rand der Listen-/Inhalts-Screens. */
    val ScreenPadding = 20.dp

    /** Innenabstand von Karten. */
    val CardPadding = 16.dp

    /** Vertikaler Abstand zwischen Karten/Listenelementen. */
    val ListGap = 12.dp

    /** Rand der Vollbild-Sitzungen (Atem-Session/Meditation). */
    val SessionPadding = 24.dp

    /** Kleine Lücke (Label ↔ Steuerung, Hint-Abstand). */
    val GapSmall = 8.dp

    /** Minimale Lücke (Hint unter einer Zeile). */
    val GapTiny = 4.dp
}
