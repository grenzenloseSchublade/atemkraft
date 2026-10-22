package app.atemkraft.ui.meditation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.min
import app.atemkraft.R
import app.atemkraft.domain.MEDITATION_MAX_MINUTES
import app.atemkraft.domain.MEDITATION_MIN_MINUTES
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.TERTIARY
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Zeitring (Variante E der Dauerwahl): ein 60-Minuten-Zifferblatt wie ein Küchenwecker, 12 Uhr
 * oben, im Uhrzeigersinn. Ziehen am Ringband oder Tippen auf Band und Zahlen setzt die Dauer
 * (am Knopf gegriffen ohne Sprung), die ∞-Marke oben – ein kleiner umrandeter Knopf – die offene
 * Sitzung (Knopf und Bogen voll Pink). Ein Tipp irgendwo in die innere Scheibe öffnet die Feinwahl
 * (− / + je Minute), ein zweiter schließt sie.
 *
 * Reine Darstellung: Wert und Regeln (Raste, ∞, Feinwahl) liegen in [DurationPickerState] und
 * [DialScale]/[DialDrag]; Mitte, Texte, TalkBack-Regler und ∞-Marke kommen aus DurationReadout.kt.
 * Eine andere Darstellung (Variante F, `TimeRuler`) braucht nur dieselbe Signatur, ihre Skala und
 * ihre Gesten.
 *
 * Füllt den gegebenen Platz quadratisch (min(maxWidth, maxHeight)), ausgerichtet nach [alignment];
 * Spur, Striche und Knopf wachsen mit der Kantenlänge, Zahlen mit der Schrift. Ist zwischen den
 * 5er-Zahlen zu wenig Platz, stehen nur die 10er. [onTick] meldet jede Wertänderung (`major` = 5er-Marke) für die
 * Haptik-Raste; ob vibriert wird, entscheidet der Aufrufer (Einstellung „Haptik“).
 */
@Composable
fun TimeDial(
    state: DurationPickerState,
    onTick: (major: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    val currentOnTick by rememberUpdatedState(onTick)
    // Im Body gelesen: Zeichnung und Semantik folgen jeder Änderung über die Recomposition.
    val minutes = state.minutes
    val free = state.free
    val fineAdjust = state.fineAdjust

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val numeralStyle = typography.labelSmall.copy(fontFeatureSettings = TABULAR)
    val markStyle = typography.titleSmall
    val texts = durationTexts(state)
    val unitStyle = durationUnitStyle
    val units = durationUnitSamples()
    val numberStyles = durationNumberStyles

    BoxWithConstraints(modifier = modifier, contentAlignment = alignment) {
        val side: Dp = when {
            constraints.hasBoundedWidth && constraints.hasBoundedHeight -> min(maxWidth, maxHeight)
            constraints.hasBoundedWidth -> maxWidth
            constraints.hasBoundedHeight -> maxHeight
            else -> Dimens.MinTouchTarget * UNBOUNDED_SIDE_TARGETS
        }
        val sidePx = with(density) { side.toPx() }
        val touchPx = with(density) { Dimens.MinTouchTarget.toPx() }
        val padPx = with(density) { Dimens.GapSmall.toPx() }
        val readoutPadPx = with(density) { DurationReadoutPad.toPx() }
        val numerals = remember(measurer, numeralStyle) {
            (MAJOR_STEP until MEDITATION_MAX_MINUTES step MAJOR_STEP).associateWith { measurer.measure(it.toString(), numeralStyle) }
        }
        val mark = remember(measurer, markStyle, texts.freeSymbol) { measurer.measure(texts.freeSymbol, markStyle) }
        val geo = remember(sidePx, numerals, mark, touchPx) { dialGeometry(sidePx, numerals.mapValues { it.value.size }, mark.size, touchPx) }
        val numberStyle = remember(geo, numberStyles, measurer, unitStyle, units, padPx, readoutPadPx) {
            // Größte Zahl, bei der „− 60 +“ samt Einheit in die Mitte passt. Passt keine, ist der
            // Ring kleiner als [timeDialMinSide] – der Aufrufer gibt ihm diese Kante.
            fitNumberStyle(numberStyles) { style ->
                measureReadout(measurer, style, unitStyle, units, readoutPadPx, touchPx).fitsIn(geo, padPx)
            }
        }

        Box(Modifier.size(side)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, geo) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val distance = (down.position - c).getDistance()
                            if (distance > size.width / 2f) return@awaitEachGesture // Ecken bleiben frei
                            // Die ganze Mitte gehört zur Zeit: ein Tipp neben die Zahl schaltet
                            // ebenfalls die Feinwahl und setzt nie einen Wert.
                            if (distance < geo.bandInner) {
                                val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                                up.consume()
                                state.toggleFineAdjust()
                                return@awaitEachGesture
                            }
                            down.consume()
                            // Am Knopf gegriffen bleibt der Wert stehen, bis der Finger zieht;
                            // sonst springt der Tipp direkt auf den gerundeten Wert (ohne Fang,
                            // damit ein Tipp neben eine 5er-Marke den Zwischenwert trifft).
                            if (state.free || !geo.isOnKnob(down.position, c, state.minutes)) state.selectWithTick(minutesAt(down.position, c), currentOnTick)
                            // Ziehen: Fang an 5er-Marken, über 12 Uhr hinaus ∞ bzw. Halt an der 1 (DialDrag).
                            val follow = DialDrag(state.minutes, exactMinutesAt(down.position, c))
                            drag(down.id) { change ->
                                val next = follow.move(exactMinutesAt(change.position, c))
                                if (next == null) state.selectFreeWithTick(currentOnTick) else state.selectWithTick(next, currentOnTick)
                                change.consume()
                            }
                        }
                    }
                    .durationSlider(state, texts, currentOnTick),
            ) {
                val c = center
                // Spur
                drawCircle(colors.surfaceVariant, radius = geo.trackRadius, center = c, style = Stroke(geo.trackWidth))
                // Minutenstriche, 5er länger und heller
                for (m in MEDITATION_MIN_MINUTES..MEDITATION_MAX_MINUTES) {
                    val major = DialScale.isMajor(m)
                    val dir = direction(DialScale.angleOf(m))
                    val inner = geo.tickOuter - if (major) geo.tickMajor else geo.tickMinor
                    drawLine(
                        color = if (major) colors.onSurfaceVariant.copy(alpha = TERTIARY) else colors.outlineVariant,
                        start = c + dir * inner,
                        end = c + dir * geo.tickOuter,
                        strokeWidth = geo.tickWidth,
                        cap = StrokeCap.Round,
                    )
                }
                // Bogen: Cyan von 12 Uhr bis zum Wert, bei ∞ voll in Pink. Die runde Kappe am
                // Anfang beginnt genau am 12-Uhr-Strich, statt in die 55 zu ragen.
                if (free) {
                    drawCircle(colors.primary, radius = geo.trackRadius, center = c, style = Stroke(geo.trackWidth))
                } else {
                    drawArc(
                        color = colors.secondary,
                        startAngle = ARC_START + geo.capDeg,
                        sweepAngle = (DialScale.angleOf(minutes) - geo.capDeg).coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = c - Offset(geo.trackRadius, geo.trackRadius),
                        size = Size(geo.trackRadius * 2f, geo.trackRadius * 2f),
                        style = Stroke(geo.trackWidth, cap = StrokeCap.Round),
                    )
                }
                // Zahlen; die Zahl unter dem Knopf trägt den Akzent
                for (n in geo.numerals) {
                    val layout = numerals.getValue(n)
                    val accent = !free && n == minutes
                    drawCentered(layout, c + direction(DialScale.angleOf(n)) * geo.numeralRadius, if (accent) colors.secondary else colors.onSurfaceVariant)
                }
                // ∞-Marke oben als kleiner Knopf: dünn umrandet, gewählt Pink gefüllt.
                val markAt = c + direction(0f) * geo.numeralRadius
                if (free) {
                    drawCircle(colors.primary, radius = geo.markRadius, center = markAt)
                } else {
                    drawCircle(colors.outline, radius = geo.markRadius, center = markAt, style = Stroke(Dimens.BorderThin.toPx()))
                }
                drawCentered(mark, markAt, if (free) colors.onPrimary else colors.onSurfaceVariant)
                if (!free) {
                    val dir = direction(DialScale.angleOf(minutes))
                    val knob = c + dir * geo.trackRadius
                    drawCircle(colors.background, radius = geo.knobRadius + geo.knobOutline, center = knob)
                    drawCircle(colors.onSurface, radius = geo.knobRadius, center = knob)
                    // Zwischenwert (z. B. 12): kleines Cyan-Label innen am Knopf. Bei offener
                    // Feinwahl steht der Wert schon groß in der Mitte.
                    if (!fineAdjust && minutes !in geo.numerals) {
                        drawCentered(measurer.measure(minutes.toString(), numeralStyle), c + dir * geo.knobLabelRadius, colors.secondary)
                    }
                }
            }

            // ∞-Marke: runde Tippfläche (≥ 48 dp, umfasst den Knopf) mittig über dem Zeichen. Sie
            // darf oben über den Ring hinausragen; über dem Canvas gewinnt sie gegen das Ringband.
            val markHalf = geo.markTouch / 2f
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (sidePx / 2f - markHalf).roundToInt(),
                            y = (sidePx / 2f - geo.numeralRadius - markHalf).roundToInt(),
                        )
                    }
                    .size(with(density) { geo.markTouch.toDp() })
                    .freeMark(state, texts, currentOnTick),
            )

            // Mitte: die ganze innere Scheibe (bis zum Band) schaltet die Feinwahl, mit rundem
            // Tipp-Echo. Zeit und Einheit liegen darüber, ohne Tippziel und ohne Zuschnitt; − und +
            // darüber bleiben eigene Knöpfe.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(with(density) { (geo.bandInner * 2f).toDp() })
                    .fineToggle(state, texts, CircleShape),
            )

            DurationReadout(
                state = state,
                texts = texts,
                onTick = currentOnTick,
                numberStyle = numberStyle,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * Kleinste Kantenlänge des Zeitrings, mindestens [floor], bei der Zahl und breiteste Einheit im
 * kleinsten Zahlenstil ganz innerhalb der Spur stehen (LAYOUT-03). Bis fontScale 1,3 ist das
 * [floor]; bei 2,0 wird der Ring größer und die Seite scrollt als Notlösung (LAYOUT-08), statt dass
 * „Minuten“ über Spur und Striche läuft. − / + der Feinwahl zählen hier nicht: Sie sind eigene
 * Knöpfe mit Fläche und dürfen auf kleinem Ring über der Spur liegen.
 */
@Composable
internal fun timeDialMinSide(floor: Dp): Dp {
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val numeralStyle = typography.labelSmall.copy(fontFeatureSettings = TABULAR)
    val markStyle = typography.titleSmall
    val freeSymbol = stringResource(R.string.meditation_free_symbol)
    val unitStyle = durationUnitStyle
    val units = durationUnitSamples()
    val smallest = durationNumberStyles.last().copy(fontFeatureSettings = TABULAR)
    return remember(density, measurer, numeralStyle, markStyle, freeSymbol, unitStyle, units, smallest, floor) {
        with(density) {
            val touchPx = Dimens.MinTouchTarget.toPx()
            val padPx = Dimens.GapSmall.toPx()
            val numerals = (MAJOR_STEP until MEDITATION_MAX_MINUTES step MAJOR_STEP).associateWith { measurer.measure(it.toString(), numeralStyle).size }
            val mark = measurer.measure(freeSymbol, markStyle).size
            val readout = measureReadout(measurer, smallest, unitStyle, units, DurationReadoutPad.toPx(), touchPx)
            // Die Mitte wächst linear mit der Kante; pixelweise bis zur Obergrenze suchen.
            val limit = floor.toPx() * MIN_SIDE_SEARCH_LIMIT
            var side = floor.toPx()
            while (side < limit && !readout.textFitsIn(dialGeometry(side, numerals, mark, touchPx), padPx)) side += 1f
            side.toDp()
        }
    }
}

/**
 * Passt die Anzeige samt − / + in die Mitte von [geo]? Die Ecken der 48-dp-Knöpfe und die Textbox
 * bleiben im Kreis, mit [padPx] Luft zur Spur.
 */
private fun ReadoutSize.fitsIn(geo: DialGeometry, padPx: Float): Boolean = hypot(rowHalfWidth, buttonHalf) <= geo.contentRadius - padPx && textFitsIn(geo, padPx)

/** Passen Zahl und Einheit (ohne − / +) mit [padPx] Luft zur Spur in die Mitte von [geo]? */
private fun ReadoutSize.textFitsIn(geo: DialGeometry, padPx: Float): Boolean = hypot(textHalfWidth, textHalfHeight) <= geo.contentRadius - padPx

/** Minuten unter dem Punkt [p] (Mitte [c]). */
private fun minutesAt(p: Offset, c: Offset): Int = DialScale.minutesAt(DialScale.angleOf(p.x, p.y, c.x, c.y))

/** Ungerundete Minuten unter dem Punkt [p] (Mitte [c]), 0..<60. */
private fun exactMinutesAt(p: Offset, c: Offset): Float = DialScale.exactMinutesAt(DialScale.angleOf(p.x, p.y, c.x, c.y))

/**
 * Maße des Zifferblatts in px, alle aus der Kantenlänge [side] abgeleitet; nur die Zahlen
 * bringen ihre gemessene Größe mit (Systemschrift). Von außen nach innen: Zahlen, Striche,
 * Spur mit Knopf, Label am Knopf, Mitte ([contentRadius]). [markRadius] ist der Kreis um ∞ (Rand
 * fest [Dimens.BorderThin], damit er auf kleinem Ring nicht dünner wird), [markTouch] der
 * Durchmesser seiner Tippfläche.
 */
internal class DialGeometry(
    val numeralRadius: Float,
    val tickOuter: Float,
    val tickMajor: Float,
    val tickMinor: Float,
    val tickWidth: Float,
    val trackRadius: Float,
    val trackWidth: Float,
    val knobRadius: Float,
    val knobOutline: Float,
    val knobLabelRadius: Float,
    val contentRadius: Float,
    val bandInner: Float,
    val knobGrab: Float,
    val capDeg: Float,
    val markRadius: Float,
    val markTouch: Float,
    val numerals: List<Int>,
) {
    /** Liegt [p] auf dem Knopf bei [minutes] (mindestens 48 dp Durchmesser)? */
    fun isOnKnob(p: Offset, c: Offset, minutes: Int): Boolean = (p - (c + direction(DialScale.angleOf(minutes)) * trackRadius)).getDistance() <= knobGrab
}

internal fun dialGeometry(side: Float, numerals: Map<Int, IntSize>, mark: IntSize, touch: Float): DialGeometry {
    val half = side / 2f
    val widest = numerals.values.maxOf { it.width }.toFloat()
    val gap = side * GAP_FRACTION
    val tickWidth = side * TICK_WIDTH_FRACTION
    // ∞-Knopf: so hoch wie eine Zahl, das Zeichen mit einer Lücke Luft zum Rand; wächst mit
    // Schrift und Ring.
    val markRadius = max(numerals.values.maxOf { it.height } / 2f, mark.width / 2f + gap)
    val labelHalf = maxOf(numerals.values.maxOf { max(it.width, it.height) } / 2f, max(mark.width, mark.height) / 2f, markRadius + tickWidth / 2f)
    val numeralRadius = half - labelHalf - gap
    val tickOuter = numeralRadius - labelHalf - gap
    val tickMajor = side * TICK_MAJOR_FRACTION
    val trackWidth = side * TRACK_FRACTION
    val trackRadius = tickOuter - tickMajor - gap - trackWidth / 2f
    val knobRadius = trackWidth * KNOB_FRACTION
    val knobOutline = trackWidth * KNOB_OUTLINE_FRACTION
    val knobLabelRadius = trackRadius - knobRadius - gap - labelHalf
    val contentRadius = trackRadius - knobRadius - gap
    // 5er-Zahlen nur, wenn zwischen zwei Nachbarn reichlich Luft bleibt; sonst nur die 10er.
    val fiveStep = (2f * PI.toFloat() * numeralRadius) / (MEDITATION_MAX_MINUTES / MAJOR_STEP)
    val every = if (fiveStep < max(widest, markRadius * 2f) * NUMERAL_SPACING) MAJOR_STEP * 2 else MAJOR_STEP
    return DialGeometry(
        numeralRadius = numeralRadius,
        tickOuter = tickOuter,
        tickMajor = tickMajor,
        tickMinor = tickMajor * TICK_MINOR_RATIO,
        tickWidth = tickWidth,
        trackRadius = trackRadius,
        trackWidth = trackWidth,
        knobRadius = knobRadius,
        knobOutline = knobOutline,
        knobLabelRadius = knobLabelRadius,
        contentRadius = contentRadius,
        // Das Band beginnt erst außerhalb der Mitte: ein Tipp knapp neben die Zeit setzt keinen Wert.
        bandInner = max(trackRadius - max(trackWidth, touch / 2f), contentRadius),
        knobGrab = max(knobRadius + knobOutline, touch / 2f),
        capDeg = Math.toDegrees((trackWidth / 2f / trackRadius).toDouble()).toFloat(),
        markRadius = markRadius,
        // Tipp-Echo umfasst den ganzen Knopf samt Rand, mindestens 48 dp.
        markTouch = max(touch, (markRadius + tickWidth) * 2f),
        numerals = (every until MEDITATION_MAX_MINUTES step every).toList(),
    )
}

/** Einheitsvektor zum Winkel (0° = 12 Uhr, im Uhrzeigersinn, y nach unten). */
private fun direction(angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(sin(rad).toFloat(), -cos(rad).toFloat())
}

/** Zeichnet [layout] mit der Mitte auf [at]. */
private fun DrawScope.drawCentered(layout: TextLayoutResult, at: Offset, color: Color) {
    drawText(layout, color = color, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

private const val MAJOR_STEP = 5
private const val ARC_START = -90f

// Anteile an der Kantenlänge (Vorlage dauerwahl.html, Abschnitt E).
private const val GAP_FRACTION = 0.012f
private const val TRACK_FRACTION = 0.05f
private const val TICK_MAJOR_FRACTION = 0.035f
private const val TICK_MINOR_RATIO = 0.5f
private const val TICK_WIDTH_FRACTION = 0.006f

private const val KNOB_FRACTION = 0.85f
private const val KNOB_OUTLINE_FRACTION = 0.15f

// Mittenabstand zweier 5er-Zahlen in Zahlbreiten, unter dem nur die 10er stehen (ruhiger
// kleiner Ring bei großer Schrift).
private const val NUMERAL_SPACING = 3f

// Obergrenze der Suche in [timeDialMinSide] als Vielfaches der Mindestkante (Schutz vor endloser
// Suche; breiter als der Bildschirm wird der Ring ohnehin nicht).
private const val MIN_SIDE_SEARCH_LIMIT = 3f

// Kantenlänge in Touch-Zielen, falls der Ring ganz ohne Grenzen gemessen wird (Vorschau, Test).
private const val UNBOUNDED_SIDE_TARGETS = 6

@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun TimeDialPreview() {
    AtemkraftTheme {
        TimeDial(state = remember { DurationPickerState(initialMinutes = 12, initialFree = false) }, onTick = {})
    }
}

@Preview(widthDp = 360, heightDp = 360)
@Composable
private fun TimeDialFineAdjustPreview() {
    AtemkraftTheme {
        TimeDial(
            state = remember { DurationPickerState(initialMinutes = 20, initialFree = false).apply { toggleFineAdjust() } },
            onTick = {},
        )
    }
}

@Preview(widthDp = 200, heightDp = 200)
@Composable
private fun TimeDialFreeSmallPreview() {
    AtemkraftTheme {
        TimeDial(state = remember { DurationPickerState(initialMinutes = 10, initialFree = true) }, onTick = {})
    }
}
