package app.atemkraft.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Untergrenze: Kleiner werden Bedienelemente nie, sonst sind sie bei Standardschrift kaum noch
// zu erkennen (Switch, Stepper-Kreis). Obergrenze: die Material-3-Größe – bei großer
// Systemschrift wachsen Buttons und Chips über heightIn ohnehin mit ihrer Beschriftung.
private const val CONTROL_SCALE_MIN = 0.8f
private const val CONTROL_SCALE_MAX = 1f

/**
 * Faktor k der SICHTBAREN Bedienelement-Größe für eine Systemschriftgröße: folgt der
 * effektiven Schrift ([TEXT_SCALE] × Systemschrift), begrenzt auf 0,8 … 1,0. Damit stehen
 * Bedienelemente und Beschriftung wieder im Material-3-Verhältnis (Button 40 dp zu 14 sp),
 * ohne bei großer Schrift zu klein zu wirken. Die Tippfläche hängt nie hieran (A11Y-05).
 */
fun controlScale(fontScale: Float): Float = (TEXT_SCALE * fontScale).coerceIn(CONTROL_SCALE_MIN, CONTROL_SCALE_MAX)

/**
 * Material-3-Grundmaße der Bedienelemente bei k = 1. Sichtbar wird `m3 × k`, auf ganze dp
 * gerundet. Eine Liste, damit der Verhältnis-Test (SizesTest) kein Token vergessen kann.
 */
internal enum class ControlSize(val m3: Float) {
    BUTTON_HEIGHT(40f),
    CHIP_HEIGHT(32f),
    ICON_BUTTON(40f),
    ICON_DEFAULT(24f),
    ICON_IN_BUTTON(18f),
    ICON_SMALL(16f),
    PROGRESS_INLINE(20f),
    STEPPER_VALUE(48f),
    ;

    fun at(k: Float): Dp = (m3 * k).roundToInt().dp
}

// k kommt direkt aus der Systemschrift (LocalDensity), nicht aus einem eigenen
// CompositionLocal: eine Quelle, kein Screen kann ihn versehentlich überschreiben.
@Composable
@ReadOnlyComposable
private fun currentControlScale(): Float = controlScale(LocalDensity.current.fontScale)

@Composable
@ReadOnlyComposable
private fun scaled(size: ControlSize): Dp = size.at(currentControlScale())

/**
 * Größen-Tokens der Bedienelemente (STYLEGUIDE §4.2, LAYOUT-04). Jedes ist M3-Wert × k
 * ([controlScale]); gesetzt werden sie nur in den Komponenten unter `ui/components`, nie am
 * Aufrufort. An M3-Elementen wirken sie nur als Mindestwert (`heightIn(min = …)`), damit das
 * Element bei großer Schrift mitwächst.
 */
object Sizes {
    /** Faktor k für Komponenten ohne Größenparameter (Switch, RadioButton → `scaledLayout`). */
    val ControlScale: Float
        @Composable @ReadOnlyComposable
        get() = currentControlScale()

    /** Sichtbare Mindesthöhe von Button, TextButton, SegmentedButton, Session- und Start-Buttons (M3 40). */
    val ButtonHeight: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.BUTTON_HEIGHT)

    /** Sichtbare Mindesthöhe der Auswahl-Chips (M3 32). */
    val ChipHeight: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.CHIP_HEIGHT)

    /** Sichtbarer Kreis von `AppIconButton` und Stepper-Knöpfen (M3 40); Tippfläche bleibt 48. */
    val IconButtonSize: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.ICON_BUTTON)

    /** Standard-Icon: Icon-Buttons, Zurück, Zahnrad, Expander-Caret, Mini-Leiste (M3 24). */
    val IconDefault: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.ICON_DEFAULT)

    /** Icon neben einer Beschriftung im Button: Reset, Segment-Häkchen, Ausklapp-Caret (M3 18). */
    val IconInButton: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.ICON_IN_BUTTON)

    /** Kleines Inline-Icon neben `label*`-Text (16). */
    val IconSmall: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.ICON_SMALL)

    /** Inline-Fortschrittskreis neben einer Zeile (20). */
    val ProgressInline: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.PROGRESS_INLINE)

    /** Mindestbreite des Stepper-Werts, damit „−“/„+“ bei 1–3 Ziffern nicht springen (48). */
    val StepperValueMinWidth: Dp
        @Composable @ReadOnlyComposable
        get() = scaled(ControlSize.STEPPER_VALUE)
}
