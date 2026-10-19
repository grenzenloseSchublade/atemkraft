package app.atemkraft.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Zentrale Abstands-Tokens (4-dp-Raster). Quelle der Wahrheit für Layout-Rhythmus – Screens
 * greifen hierauf zu statt auf Streu-Literale. Passend zur Material-Schrift (seit 2026-10-19):
 * Ränder und Karten-Innenabstand wie bei Material 3 (16), Lücken eine Stufe luftiger als zur
 * Zeit der verkleinerten Schrift. Touch-Ziele hängen NICHT an diesen Werten, sondern an
 * [MinTouchTarget].
 */
object Dimens {
    /** Horizontaler Screen-Rand der Listen-/Inhalts-Screens (M3-Rand für kompakte Breiten). */
    val ScreenPadding = 16.dp

    /** Innenabstand von Karten (M3). */
    val CardPadding = 16.dp

    /** Karte ↔ Karte, Listen, Button-Reihen. */
    val ListGap = 12.dp

    /** Rand der Vollbild-Sitzungen (Atem-Session/Meditation). */
    val SessionPadding = 24.dp

    /** Oben auf Tab-Screens. */
    val ScreenTop = 16.dp

    /** Oben auf Push-Screens, über dem Zurück-Button (der selbst 48 dp Tippfläche mitbringt). */
    val ScreenTopSub = 8.dp

    /** Unten auf allen Scroll-Screens. */
    val ScreenBottom = 24.dp

    /** Nur zwischen Blöcken verschiedenen Typs. */
    val SectionGap = 16.dp

    /** Über jedem SectionHeader. */
    val SectionHeaderTop = 24.dp

    /** Kleine Lücke: Label ↔ Steuerung. */
    val GapSmall = 8.dp

    /** Minimale Lücke: Hint unter einer Zeile, Icon ↔ Text. */
    val GapTiny = 4.dp

    /** Haarlinie: Eyebrow → Titel, Divider (2, Raster-Ausnahme). */
    val GapHairline = 2.dp

    /**
     * Mindestkante des Dauer-Reglers im Meditations-Tab (Zeitring). Kleiner wird er nie – reicht
     * der Platz nicht, scrollt die Seite als Notlösung. Bei großer Schrift liegt die Mindestkante
     * höher (`timeDialMinSide`), damit Zahl und Einheit innerhalb der Spur bleiben.
     */
    val DurationPickerMin = 200.dp

    /** Rand navigierender Inhaltskarten (KOMP-03, `accentBorder`). */
    val BorderThin = 1.dp

    /** Mindesthöhe eigener klickbarer Flächen (Material-Touch-Ziel), unabhängig vom Padding. */
    val MinTouchTarget = 48.dp

    /** Strichstärke von Spur und Fortschrittsbogen um den Atemkreis (3, Raster-Ausnahme). */
    val SessionRingStroke = 3.dp

    /** Abstand der Knöpfe in der Session-Steuerung (Von vorne · Pause · Beenden). */
    val SessionControlGap = 40.dp

    // Session-Steuerung: fest wie die OverlayChrome-Icons, nicht mit der Schrift verkleinert –
    // Bedienung mit halb geschlossenen Augen; Maße wie im Mockup Variante B (Nutzerwunsch 2026-10-19).

    /** Kreis von Pause/Weiter in der Session-Steuerung. */
    val SessionMainButton = 76.dp

    /** Kreis von „Von vorne“/„Beenden“ neben Pause/Weiter (selbst ≥ 48, also Tippfläche). */
    val SessionSideButton = 56.dp

    /** Pause-/Play-Symbol im großen Session-Knopf. */
    val SessionMainIcon = 32.dp

    /** Kreispfeil und Stopp in den Seitenknöpfen. */
    val SessionSideIcon = 28.dp
}
