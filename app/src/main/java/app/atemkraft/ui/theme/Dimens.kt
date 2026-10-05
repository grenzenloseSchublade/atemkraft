package app.atemkraft.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Zentrale Abstands-Tokens (4-dp-Raster). Quelle der Wahrheit für Layout-Rhythmus – Screens
 * greifen hierauf zu statt auf Streu-Literale. Seit TEXT_SCALE 0,75 eine Stufe kompakter als
 * ursprünglich (Werte vorher in Klammern), damit Luft und Schrift im Verhältnis bleiben.
 * Touch-Ziele hängen NICHT an diesen Werten, sondern an [MinTouchTarget].
 */
object Dimens {
    /** Horizontaler Screen-Rand der Listen-/Inhalts-Screens (20). */
    val ScreenPadding = 16.dp

    /** Innenabstand von Karten (16). */
    val CardPadding = 12.dp

    /** Karte ↔ Karte, Listen, Button-Reihen (12). */
    val ListGap = 8.dp

    /** Rand der Vollbild-Sitzungen (Atem-Session/Meditation) (24). */
    val SessionPadding = 20.dp

    /** Oben auf Tab-Screens (20). */
    val ScreenTop = 16.dp

    /** Oben auf Push-Screens, über dem Zurück-Button (12). */
    val ScreenTopSub = 8.dp

    /** Unten auf allen Scroll-Screens (24). */
    val ScreenBottom = 16.dp

    /** Nur zwischen Blöcken verschiedenen Typs (16). */
    val SectionGap = 12.dp

    /** Über jedem SectionHeader (28). */
    val SectionHeaderTop = 20.dp

    /** Kleine Lücke: Label ↔ Steuerung (8). */
    val GapSmall = 8.dp

    /** Minimale Lücke: Hint unter einer Zeile, Icon ↔ Text (4). */
    val GapTiny = 4.dp

    /** Haarlinie: Eyebrow → Titel, Divider (2, Raster-Ausnahme). */
    val GapHairline = 2.dp

    /** Mindesthöhe eigener klickbarer Flächen (Material-Touch-Ziel), unabhängig vom Padding. */
    val MinTouchTarget = 48.dp
}
