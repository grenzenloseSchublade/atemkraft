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

    /**
     * Mindestbreite eines M3-Buttons (`ButtonDefaults.MinWidth` 58): Breite des Reset neben
     * „Starten“, dessen Icon mit Polster (2 × 16 + 18 = 50) darunter bleibt.
     */
    val ButtonMinWidth = 58.dp

    /** Sichtbare Mindesthöhe der Auswahl-Chips (M3 32). */
    val ChipHeight = 32.dp

    /** Breite des M3-Switch (SwitchTokens.TrackWidth): Der Hinweis einer `ToggleRow` endet davor. */
    val SwitchWidth = 52.dp

    /** Sichtbarer Kreis von `AppIconButton` und Stepper-Knöpfen (M3 40); Tippfläche bleibt 48. */
    val IconButtonSize = 40.dp

    /**
     * Griff, der in der Tiefe die Tab-Leiste auf- und zuklappt (`TabBarToggle`): ein schmaler Strich
     * wie oben an einem Bottom Sheet (M3 `BottomSheetDefaults.DragHandle`: 32 × 4, ganz gerundet).
     */
    val TabBarGripWidth = 32.dp
    val TabBarGripHeight = 4.dp

    /**
     * So weit muss ein Finger nach oben bzw. unten wischen, bis die Tab-Leiste aufgeht bzw. zugeht
     * (24, Schwelle ab dem Aufsetzen; gewertet wird erst nach dem Touch-Slop, Tipps bleiben Tipps).
     */
    val TabBarSwipeDistance = 24.dp

    /** Standard-Icon: Icon-Buttons, Zurück, Zahnrad, Expander-Caret, Mini-Leiste (M3 24). */
    val IconDefault = 24.dp

    /** Icon neben einer Beschriftung im Button: Reset, Ausklapp-Caret (M3 18). */
    val IconInButton = 18.dp

    /** Kleines Inline-Icon neben `label*`-Text (16). */
    val IconSmall = 16.dp

    /** Inline-Fortschrittskreis neben einer Zeile (20). */
    val ProgressInline = 20.dp

    /** Strichstärke des Inline-Fortschrittskreises ([ProgressInline]) (2). */
    val ProgressStroke = 2.dp

    /** Tagespunkt der 7-Tage-Übersicht im Logbuch (18). */
    val WeekDot = 18.dp

    /** Familienpunkt vor jedem Logbuch-Eintrag (10), mittig zur ersten Namenszeile. */
    val FamilyDot = 10.dp

    /**
     * Pause-/Play-Symbol, das beim Antippen des Atemkreises kurz aufblinkt (`PauseFlash`, 150);
     * nie größer als der Kreis selbst.
     */
    val PauseFlashSize = 150.dp

    /**
     * Überstand der 48-dp-Tippfläche über ein [IconDefault]-Icon auf jeder Seite (12): Um ihn
     * melden `Modifier.flushStart()`/`flushEnd()` einen Icon-Button schmaler, damit sein Glyph an
     * der Inhaltskante steht und die Tippfläche in den Rand ragt (LAYOUT-07).
     */
    val TouchOverhang = (Dimens.MinTouchTarget - IconDefault) / 2

    /**
     * Überstand der 48-dp-Tippfläche über einen [IconButtonSize]-Kreis auf jeder Seite (4): Um ihn
     * rückt ein Stepper ab bzw. meldet sich schmaler, damit der sichtbare Kreis von „+“ an der
     * Inhaltskante steht (LAYOUT-07).
     */
    val CircleOverhang = (Dimens.MinTouchTarget - IconButtonSize) / 2

    /** Mindestbreite des Stepper-Werts, damit „−“/„+“ bei 1–3 Ziffern nicht springen (48). */
    val StepperValueMinWidth = 48.dp

    /**
     * Platz, den M3-`SegmentedButton` (1.3) für das Häkchen immer reserviert, auch ohne Icon:
     * Icon 18 + Abstand 8. `SegmentedChoiceRow` (ohne Häkchen) gibt ihn an die Beschriftung zurück.
     */
    val SegmentIconReserve = 26.dp

    // Atemkurve des Muster-Editors (`BreathCurveEditor`, Vorlage „Frei.dc.html“). Die Breite
    // folgt dem Platz, die Zeitachse der Länge der Folge; Beschriftungen wachsen mit der Schrift.

    /** Abstand der Linien „voll“ und „leer“, also die Höhe der Kurve (88). */
    val CurveSpan = 88.dp

    /**
     * Kleinster Abstand „voll“–„leer“, wenn der Bildschirm für [CurveSpan] zu niedrig ist (48 =
     * [Dimens.MinTouchTarget]): Ein-Ende oben und Aus-Ende unten liegen dann noch eine ganze
     * Tippfläche auseinander, Halten-Griffe über und unter den Linien bleiben getrennt greifbar,
     * und „4 s“ passt neben die Schräge zwischen die Linien (sonst rückt es über bzw. unter den
     * Griff oder entfällt – Beschriftungen überlappen nie).
     */
    val CurveSpanMin = 48.dp

    /** Sichtbarer Griff an einem Phasenende (18); die Tippfläche ist [Dimens.MinTouchTarget]. */
    val CurveHandle = 18.dp

    /** Griff der gewählten Phase, etwas größer als [CurveHandle] (22). */
    val CurveHandleSelected = 22.dp

    /** Mindestabstand zweier Zahlen an der Zeitleiste; danach richtet sich ihr Schritt (30, Schwelle der Vorlage). */
    val CurveLabelMinSpacing = 30.dp

    /** Feine Striche je Sekunde nur, solange sie mindestens so weit auseinander liegen (6, Schwelle der Vorlage). */
    val CurveTickMinSpacing = 6.dp

    /** Strich von Ein- und Ausatmen (4). */
    val CurveStroke = 4.dp

    /** Strich der gewählten Phase, dicker als [CurveStroke] (6). */
    val CurveStrokeSelected = 6.dp

    /** Strich der gestrichelten Halten-Phasen und Ring der Griffe (3, Raster-Ausnahme). */
    val CurveHoldStroke = 3.dp

    /** Strichlänge der Halten-Phasen (6; die Lücke ist 5/6 davon, wie in der Vorlage). */
    val CurveDash = 6.dp

    /** Punkt der Vorschau, der über die Kurve wandert (14). */
    val CurveDot = 14.dp

    /** Mini-Kurve je Vorlage im Startpunkt-Sheet des Muster-Editors: Breite (64) und Höhe (24 = [IconDefault]). */
    val MiniCurveWidth = 64.dp
    val MiniCurveHeight = IconDefault

    // Rhythmuskurve oben auf der Detailseite (`RhythmCurve`): ein Atemzug, ruhig und klein; die
    // Breite folgt dem Platz.

    /**
     * Höhe der Rhythmuskurve samt Rand für den Punkt oben und unten (56): Abstand leer–voll 48,
     * so lesen sich auch drei Stufen (Viloma) mit ihren Pausen.
     */
    val RhythmCurveHeight = 56.dp

    /** Strich der Rhythmuskurve (2), halb so stark wie die Kurve des Editors ([CurveStroke]). */
    val RhythmCurveStroke = 2.dp

    /** Punkt, der im Takt der Übung über die Rhythmuskurve läuft (8), kleiner als [CurveDot]. */
    val RhythmCurveDot = 8.dp

    /** Höchstbreite des vāyu-Bilds der Atemlehre (280, ein Fünftel unter der Breite der Vorlage); schmaler skaliert es mit. */
    val VayuFigureMaxWidth = 280.dp
}
