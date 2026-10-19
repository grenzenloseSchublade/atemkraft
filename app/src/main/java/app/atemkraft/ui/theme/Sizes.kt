package app.atemkraft.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Größen-Tokens der Bedienelemente (STYLEGUIDE §4.2, LAYOUT-04) = Material-3-Maße. Seit die
 * Schrift wieder Material-Standard ist (Nutzerentscheidung 2026-10-19), stehen Bedienelement und
 * Beschriftung ohne eigenen Faktor im M3-Verhältnis (Button 40 dp zu `labelLarge` 14 sp).
 * Gesetzt werden sie nur in den Komponenten unter `ui/components`, nie am Aufrufort. An
 * M3-Elementen wirken sie nur als Mindestwert (`heightIn(min = …)`), damit das Element bei großer
 * Systemschrift mit seiner Beschriftung mitwächst. Die Tippfläche ([Dimens.MinTouchTarget])
 * hängt nie hieran (A11Y-05).
 */
object Sizes {
    /** Sichtbare Mindesthöhe von Button, TextButton, SegmentedButton, Session- und Start-Buttons (M3 40). */
    val ButtonHeight = 40.dp

    /** Sichtbare Mindesthöhe der Auswahl-Chips (M3 32). */
    val ChipHeight = 32.dp

    /** Sichtbarer Kreis von `AppIconButton` und Stepper-Knöpfen (M3 40); Tippfläche bleibt 48. */
    val IconButtonSize = 40.dp

    /** Standard-Icon: Icon-Buttons, Zurück, Zahnrad, Expander-Caret, Mini-Leiste (M3 24). */
    val IconDefault = 24.dp

    /** Icon neben einer Beschriftung im Button: Reset, Segment-Häkchen, Ausklapp-Caret (M3 18). */
    val IconInButton = 18.dp

    /** Kleines Inline-Icon neben `label*`-Text (16). */
    val IconSmall = 16.dp

    /** Inline-Fortschrittskreis neben einer Zeile (20). */
    val ProgressInline = 20.dp

    /** Strichstärke des Inline-Fortschrittskreises ([ProgressInline]) (2). */
    val ProgressStroke = 2.dp

    /**
     * Überstand der 48-dp-Tippfläche über ein [IconDefault]-Icon auf jeder Seite (12): Um ihn
     * melden `Modifier.flushStart()`/`flushEnd()` einen Icon-Button schmaler, damit sein Glyph an
     * der Inhaltskante steht und die Tippfläche in den Rand ragt (LAYOUT-07).
     */
    val TouchOverhang = (Dimens.MinTouchTarget - IconDefault) / 2

    /** Mindestbreite des Stepper-Werts, damit „−“/„+“ bei 1–3 Ziffern nicht springen (48). */
    val StepperValueMinWidth = 48.dp

    /**
     * Platz, den M3-`SegmentedButton` (1.3) für das Häkchen immer reserviert, auch ohne Icon:
     * Icon 18 + Abstand 8. `SegmentedChoiceRow` gibt ihn ohne Häkchen an die Beschriftung zurück.
     */
    val SegmentIconReserve = 26.dp
}
