package app.atemkraft.ui.components

import android.animation.ValueAnimator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.rangeFor
import app.atemkraft.domain.secondsOf
import app.atemkraft.ui.theme.ALPHA_CURVE_FILL
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import kotlin.math.roundToInt

/** Feste Zeitskala der Atemkurve in Sekunden: Der längste erlaubte Atemzug (13 s) passt, nichts springt. */
internal const val CURVE_SECONDS = 14

/**
 * Zahlen an der Zeitleiste alle so viele Sekunden, Striche jede Sekunde: alle 2 s, und wenn die
 * Zahlen dafür zu breit sind (schmales Gerät, große Schrift), alle 4 s statt übereinander.
 */
private val CURVE_LABEL_STEPS = listOf(2, 4)

/** Lücke der gestrichelten Halten-Phasen im Verhältnis zur Strichlänge (Vorlage 6/5). */
private const val DASH_GAP_RATIO = 5f / 6f

private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * Atemkurve des Muster-Editors (MUSTER-10): ein Atemzug als Kurve, Hauptbedienung des Editors.
 * x = Zeit auf fester Skala 0–[CURVE_SECONDS] s, y = Lungenfüllung (leer unten, voll oben).
 * Einatmen durchgezogen in `secondary`, Halten voll gestrichelt `secondary`, Ausatmen durchgezogen
 * [BreathExhale], Halten leer gestrichelt [BreathExhale] – dieselbe Farbsprache wie die
 * Rhythmusbalken der Atemlehre (FARBE-04). Je Phase „4 s“ in Phasenfarbe, darunter eine Zeitleiste.
 *
 * Griffe an den Phasenenden ziehen horizontal; der Wert rastet auf ganze Sekunden, ist auf
 * [rangeFor] geklemmt (ein ungültiges Muster entsteht nie) und kommt über [onChange]; spätere
 * Phasen rücken mit. Je Raste ein Tick als Berührungs-Rückmeldung (Android-Einstellung, wie die
 * Rasten des Zeitrings, AUDIO-05). Ein Feld ohne Wertebereich (Halten leer bei „sanft aktivierend“)
 * hat keinen Griff.
 *
 * Darunter die Vorschau: Play/Pause lässt einen Punkt in Echtzeit über die Kurve laufen, daneben
 * der Phasenname (statt des Hinweises „Griffe ziehen“, `HintText`); nur sichtbar, kein Ton. Bei
 * abgeschalteten System-Animationen entfällt der Knopf (MOTION-02).
 *
 * TalkBack: Die Kurve ist ein Knoten mit `contentDescription` („Atemkurve: Einatmen 4 Sekunden, …“)
 * ohne liveRegion; Ziehen ist nie Pflicht – dieselben Werte stellt der Aufrufer als Stepper
 * („Genau einstellen“) direkt darunter bereit.
 */
@Composable
fun BreathCurveEditor(
    spec: PatternSpec,
    onChange: (PatternField, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSpec by rememberUpdatedState(spec)
    val currentOnChange by rememberUpdatedState(onChange)
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val inhaleColor = colors.secondary
    val exhaleColor = BreathExhale
    val metrics = rememberCurveMetrics()
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val textColor = colors.onSurface.copy(alpha = TERTIARY)

    val description = curveDescription(spec)
    val phaseLabels = PatternField.entries.associateWith { field ->
        val text = stringResource(R.string.pattern_editor_curve_seconds, spec.secondsOf(field))
        remember(text, labelStyle, measurer) { measurer.measure(text, labelStyle) }
    }

    // Vorschau: nur mit System-Animationen (MOTION-02); die Zeit läuft in Echtzeit im Kreis.
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var playing by remember { mutableStateOf(false) }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val cycle = currentSpec.cycleSeconds().toFloat()
                time = if (cycle > 0f) (time + (now - last) / NANOS_PER_SECOND) % cycle else 0f
                last = now
            }
        }
    }
    val phaseNow by remember { derivedStateOf { currentSpec.phaseAt(time) } }

    Column(modifier = modifier.background(colors.surface, MaterialTheme.shapes.medium)) {
        val density = LocalDensity.current
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { metrics.height.toDp() })
                .semantics { contentDescription = description }
                .pointerInput(metrics) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val layout = metrics.layout(size.width.toFloat())
                        val start = currentSpec
                        val handle = nearestHandle(curveHandles(start, layout), down.position, layout.touchRadius)
                            ?: return@awaitEachGesture
                        down.consume()
                        playing = false
                        val field = handle.field
                        val phaseStart = layout.xAt(start.startOf(field).toFloat())
                        // Griffstelle relativ zum Phasenende: Wer neben die Mitte greift (oder den
                        // versetzten 0-s-Griff), verschiebt nichts, bis der Finger zieht.
                        val grab = down.position.x - layout.xAt((start.startOf(field) + start.secondsOf(field)).toFloat())
                        var last = start.secondsOf(field)
                        drag(down.id) { change ->
                            change.consume()
                            val range = currentSpec.rangeFor(field) ?: return@drag
                            val value = ((change.position.x - grab - phaseStart) / layout.pxPerSecond).roundToInt().coerceIn(range)
                            if (value != last) {
                                last = value
                                currentOnChange(field, value)
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                        }
                    }
                },
        ) {
            val layout = metrics.layout(size.width)
            drawCurve(
                spec = spec,
                layout = layout,
                inhaleColor = inhaleColor,
                exhaleColor = exhaleColor,
                lineColor = colors.outlineVariant,
                textColor = textColor,
                phaseLabels = phaseLabels,
            )
            if (playing) {
                drawCircle(colors.onSurface, radius = Sizes.CurveDot.toPx() / 2f, center = spec.pointAt(time, layout))
            }
            curveHandles(spec, layout).forEach { drawHandle(it, layout, if (it.field.isInhaleSide()) inhaleColor else exhaleColor, colors.background) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(start = if (animationsEnabled) Dimens.GapTiny else Dimens.CardPadding, end = Dimens.ListGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (animationsEnabled) {
                AppIconButton(onClick = { playing = !playing }) {
                    if (playing) {
                        Icon(painterResource(R.drawable.ic_pause), contentDescription = stringResource(R.string.cd_preview_pause), tint = inhaleColor)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.cd_preview_play), tint = inhaleColor)
                    }
                }
            }
            // Läuft die Vorschau, steht dort der Phasenname (ohne liveRegion, er wechselt im Takt),
            // sonst der Hinweis „Griffe ziehen“ – beim Zuschauen ist er nicht gefragt. Beide
            // brechen nur zwischen Wörtern um, nie im Wort (LAYOUT-03).
            if (playing) {
                WholeWordText(
                    text = phaseName(phaseNow),
                    modifier = Modifier.weight(1f).padding(start = Dimens.GapSmall),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (phaseNow.isInhaleSide()) inhaleColor else exhaleColor,
                )
            } else {
                CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                    HintText(
                        text = stringResource(R.string.pattern_editor_curve_hint),
                        modifier = Modifier.weight(1f).padding(start = Dimens.GapSmall).wrapContentWidth(Alignment.End),
                    )
                }
            }
        }
    }
}

/** TalkBack-Text der Kurve: „Atemkurve: Einatmen 4 Sekunden, Halten voll 1 Sekunde, …“; Halten 0 entfällt. */
@Composable
private fun curveDescription(spec: PatternSpec): String {
    val parts = PatternField.entries.mapNotNull { field ->
        val seconds = spec.secondsOf(field)
        if (seconds <= 0) return@mapNotNull null
        stringResource(
            R.string.cd_breath_curve_part,
            phaseName(field),
            pluralStringResource(R.plurals.cd_duration_seconds, seconds, seconds),
        )
    }
    return stringResource(R.string.cd_breath_curve, parts.joinToString(", "))
}

/** Name einer Phase für Vorschau und TalkBack; Halten mit „voll“/„leer“, weil beide sonst nur „Halten“ heißen. */
@Composable
private fun phaseName(field: PatternField): String = stringResource(
    when (field) {
        PatternField.INHALE -> R.string.phase_inhale
        PatternField.HOLD_FULL -> R.string.cd_hold_full
        PatternField.EXHALE -> R.string.phase_exhale
        PatternField.HOLD_EMPTY -> R.string.cd_hold_empty
    },
)

/** Cyan-Seite (Einatmen, Halten voll) oder Koralle-Seite (Ausatmen, Halten leer). */
private fun PatternField.isInhaleSide(): Boolean = this == PatternField.INHALE || this == PatternField.HOLD_FULL

/** Beginn der Phase [field] in Sekunden ab Beginn des Atemzugs. */
internal fun PatternSpec.startOf(field: PatternField): Int = PatternField.entries.takeWhile { it != field }.sumOf { secondsOf(it) }

/** Länge eines Atemzugs in ganzen Sekunden. */
internal fun PatternSpec.cycleSeconds(): Int = PatternField.entries.sumOf { secondsOf(it) }

/** Phase zum Zeitpunkt [time] (Sekunden ab Beginn des Atemzugs); Halten mit 0 s wird übersprungen. */
internal fun PatternSpec.phaseAt(time: Float): PatternField = PatternField.entries.firstOrNull { time < startOf(it) + secondsOf(it) } ?: PatternField.HOLD_EMPTY

/** Ort des Vorschau-Punkts auf der Kurve zum Zeitpunkt [time]. */
private fun PatternSpec.pointAt(time: Float, layout: CurveLayout): Offset {
    val field = phaseAt(time)
    val progress = ((time - startOf(field)) / secondsOf(field).coerceAtLeast(1)).coerceIn(0f, 1f)
    val y = when (field) {
        PatternField.INHALE -> layout.yEmpty - (layout.yEmpty - layout.yFull) * progress
        PatternField.HOLD_FULL -> layout.yFull
        PatternField.EXHALE -> layout.yFull + (layout.yEmpty - layout.yFull) * progress
        PatternField.HOLD_EMPTY -> layout.yEmpty
    }
    return Offset(layout.xAt(time), y)
}

/**
 * Maße der Kurve, die nur von Dichte und Schrift abhängen (in px). [layout] ergänzt die Breite.
 * Von oben: Beschriftung „Halten voll“, Halten-voll-Griff, Linie „voll“, Kurve, Linie „leer“,
 * Halten-leer-Griff, seine Beschriftung, Zeitleiste mit Zahlen.
 */
@Immutable
internal class CurveMetrics(
    private val labelHeight: Float,
    private val tickLabels: List<TextLayoutResult>,
    private val fullLabel: TextLayoutResult,
    private val emptyLabel: TextLayoutResult,
    private val gapTiny: Float,
    private val gapSmall: Float,
    private val hairline: Float,
    private val inset: Float,
    private val span: Float,
    private val holdOffset: Float,
    private val handle: Float,
    private val zeroShift: Float,
    private val touch: Float,
) {
    private val yFull = gapTiny + labelHeight + hairline + holdOffset + handle / 2f
    private val yEmpty = yFull + span
    private val emptyLabelTop = yEmpty + holdOffset + handle / 2f + hairline
    private val ticksTop = emptyLabelTop + labelHeight + gapTiny
    private val tickLabelTop = ticksTop + gapSmall + hairline

    /** Gesamthöhe der Zeichenfläche. */
    val height: Float = tickLabelTop + (tickLabels.maxOfOrNull { it.size.height } ?: 0) + gapTiny

    fun layout(width: Float): CurveLayout {
        val first = tickLabels.firstOrNull()?.size?.width ?: 0
        val lastWidth = tickLabels.lastOrNull()?.size?.width ?: 0
        val x0 = inset + first / 2f
        val x1 = width - gapSmall - lastWidth / 2f
        val pxPerSecond = (x1 - x0) / CURVE_SECONDS
        // Erste Schrittweite, bei der zwischen zwei Zahlen doppelt so viel frei bleibt wie zwischen
        // Zahl und Einheit – sonst liest sich „10 s 12 s“ als eine Reihe.
        val labelStep = CURVE_LABEL_STEPS.firstOrNull { step ->
            val widest = (0..CURVE_SECONDS step step).maxOf { tickLabels[it].size.width }
            widest + 2 * gapSmall <= step * pxPerSecond
        } ?: CURVE_LABEL_STEPS.last()
        return CurveLayout(
            x0 = x0,
            x1 = x1,
            labelStep = labelStep,
            yFull = yFull,
            yEmpty = yEmpty,
            holdOffset = holdOffset,
            zeroShift = zeroShift,
            handle = handle,
            touchRadius = touch / 2f,
            holdLabelTop = gapTiny,
            emptyLabelTop = emptyLabelTop,
            ticksTop = ticksTop,
            tickLabelTop = tickLabelTop,
            gapTiny = gapTiny,
            gapSmall = gapSmall,
            hairline = hairline,
            tickLabels = tickLabels,
            fullLabel = fullLabel,
            emptyLabel = emptyLabel,
        )
    }
}

/** Alle Lagen der Kurve in px für eine Breite (siehe [CurveMetrics]). */
internal class CurveLayout(
    val x0: Float,
    val x1: Float,
    /** Zahlen an der Zeitleiste alle so viele Sekunden ([CURVE_LABEL_STEPS]). */
    val labelStep: Int,
    val yFull: Float,
    val yEmpty: Float,
    val holdOffset: Float,
    val zeroShift: Float,
    val handle: Float,
    val touchRadius: Float,
    val holdLabelTop: Float,
    val emptyLabelTop: Float,
    val ticksTop: Float,
    val tickLabelTop: Float,
    val gapTiny: Float,
    val gapSmall: Float,
    val hairline: Float,
    val tickLabels: List<TextLayoutResult>,
    val fullLabel: TextLayoutResult,
    val emptyLabel: TextLayoutResult,
) {
    val pxPerSecond: Float = (x1 - x0) / CURVE_SECONDS

    fun xAt(seconds: Float): Float = x0 + seconds * pxPerSecond
}

/** Misst die Beschriftungen der Kurve mit der aktuellen Schrift; auch für Tests, die Griffe treffen wollen. */
@Composable
internal fun rememberCurveMetrics(): CurveMetrics {
    val density = LocalDensity.current
    val measurer: TextMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val smallStyle = MaterialTheme.typography.labelSmall
    // Je Sekunde gemessen, damit die Zeitleiste ihre Schrittweite nach der Breite wählen kann.
    val ticks = (0..CURVE_SECONDS).map { stringResource(R.string.pattern_editor_curve_seconds, it) }
    val full = stringResource(R.string.pattern_editor_curve_full)
    val empty = stringResource(R.string.pattern_editor_curve_empty)
    return remember(density, measurer, labelStyle, smallStyle, ticks, full, empty) {
        with(density) {
            CurveMetrics(
                labelHeight = measurer.measure(ticks.last(), labelStyle).size.height.toFloat(),
                tickLabels = ticks.map { measurer.measure(it, smallStyle) },
                fullLabel = measurer.measure(full, smallStyle),
                emptyLabel = measurer.measure(empty, smallStyle),
                gapTiny = Dimens.GapTiny.toPx(),
                gapSmall = Dimens.GapSmall.toPx(),
                hairline = Dimens.GapHairline.toPx(),
                inset = Dimens.CardPadding.toPx(),
                span = Sizes.CurveSpan.toPx(),
                holdOffset = Sizes.CurveHoldHandleOffset.toPx(),
                handle = Sizes.CurveHandle.toPx(),
                zeroShift = Sizes.CurveZeroShift.toPx(),
                touch = Dimens.MinTouchTarget.toPx(),
            )
        }
    }
}

/** Ein Griff: Feld, Mittelpunkt in px und ob er für ein Halten mit 0 s versetzt sitzt. */
internal data class CurveHandle(val field: PatternField, val center: Offset, val zero: Boolean)

/**
 * Griffe der Kurve: Einatmen-Ende auf der Linie „voll“, Ausatmen-Ende auf „leer“, Halten voll
 * darüber, Halten leer darunter; ein Halten mit 0 s rückt nach rechts. Felder ohne Wertebereich
 * ([rangeFor] `null`, z. B. Halten leer bei „sanft aktivierend“) haben keinen Griff.
 */
internal fun curveHandles(spec: PatternSpec, layout: CurveLayout): List<CurveHandle> = PatternField.entries.filter { spec.rangeFor(it) != null }.map { field ->
    val hold = field == PatternField.HOLD_FULL || field == PatternField.HOLD_EMPTY
    val zero = hold && spec.secondsOf(field) == 0
    val end = layout.xAt((spec.startOf(field) + spec.secondsOf(field)).toFloat())
    val y = when (field) {
        PatternField.INHALE -> layout.yFull
        PatternField.HOLD_FULL -> layout.yFull - layout.holdOffset
        PatternField.EXHALE -> layout.yEmpty
        PatternField.HOLD_EMPTY -> layout.yEmpty + layout.holdOffset
    }
    CurveHandle(field, Offset(end + if (zero) layout.zeroShift else 0f, y), zero)
}

/** Der Griff, dessen Tippfläche (Kreis mit [radius]) [position] trifft; bei Überlappung der nächste. */
internal fun nearestHandle(handles: List<CurveHandle>, position: Offset, radius: Float): CurveHandle? = handles
    .map { it to (it.center - position).getDistance() }
    .filter { it.second <= radius }
    .minByOrNull { it.second }
    ?.first

private fun DrawScope.drawCurve(
    spec: PatternSpec,
    layout: CurveLayout,
    inhaleColor: Color,
    exhaleColor: Color,
    lineColor: Color,
    textColor: Color,
    phaseLabels: Map<PatternField, TextLayoutResult>,
) {
    val ends = PatternField.entries.map { layout.xAt((spec.startOf(it) + spec.secondsOf(it)).toFloat()) }
    val (xIn, xHold, xEx, xEmpty) = ends
    val stroke = Sizes.CurveStroke.toPx()
    val holdStroke = Sizes.CurveHoldStroke.toPx()
    val dash = Sizes.CurveDash.toPx()
    val dashed = PathEffect.dashPathEffect(floatArrayOf(dash, dash * DASH_GAP_RATIO))
    val thin = Dimens.BorderThin.toPx()

    // Zarte Füllung unter der Kurve (Lungenfüllung).
    val fill = Path().apply {
        moveTo(layout.x0, layout.yEmpty)
        lineTo(xIn, layout.yFull)
        lineTo(xHold, layout.yFull)
        lineTo(xEx, layout.yEmpty)
        close()
    }
    drawPath(fill, inhaleColor.copy(alpha = ALPHA_CURVE_FILL))

    // Linien „voll“ und „leer“ (dekorativ) mit ihrer Beschriftung links.
    listOf(layout.yFull, layout.yEmpty).forEach { y ->
        drawLine(lineColor, Offset(layout.gapSmall, y), Offset(size.width - layout.gapSmall, y), strokeWidth = thin)
    }
    drawText(layout.fullLabel, textColor, Offset(layout.gapSmall, layout.yFull - layout.hairline - layout.fullLabel.size.height))
    drawText(layout.emptyLabel, textColor, Offset(layout.gapSmall, layout.yEmpty + layout.hairline))

    // Phasen: Ein-/Ausatmen durchgezogen, Halten gestrichelt.
    drawLine(inhaleColor, Offset(layout.x0, layout.yEmpty), Offset(xIn, layout.yFull), stroke, StrokeCap.Round)
    if (xHold > xIn) drawLine(inhaleColor, Offset(xIn, layout.yFull), Offset(xHold, layout.yFull), holdStroke, StrokeCap.Round, dashed)
    drawLine(exhaleColor, Offset(xHold, layout.yFull), Offset(xEx, layout.yEmpty), stroke, StrokeCap.Round)
    if (xEmpty > xEx) drawLine(exhaleColor, Offset(xEx, layout.yEmpty), Offset(xEmpty, layout.yEmpty), holdStroke, StrokeCap.Round, dashed)

    // Beschriftung „4 s“ je Phase in Phasenfarbe: Ein- und Ausatmen neben der Schräge, Halten
    // über bzw. unter dem Abschnitt.
    val midY = (layout.yFull + layout.yEmpty) / 2f
    phaseLabels.forEach { (field, text) ->
        if (spec.secondsOf(field) <= 0) return@forEach
        val from = layout.xAt(spec.startOf(field).toFloat())
        val to = layout.xAt((spec.startOf(field) + spec.secondsOf(field)).toFloat())
        val mid = (from + to) / 2f
        val w = text.size.width
        val h = text.size.height
        val topLeft = when (field) {
            PatternField.INHALE -> Offset(mid - layout.gapSmall - w, midY - h)
            PatternField.EXHALE -> Offset(mid + layout.gapSmall, midY - h)
            PatternField.HOLD_FULL -> Offset(mid - w / 2f, layout.holdLabelTop)
            PatternField.HOLD_EMPTY -> Offset(mid - w / 2f, layout.emptyLabelTop)
        }
        drawText(text, if (field.isInhaleSide()) inhaleColor else exhaleColor, topLeft)
    }

    // Zeitleiste: Strich je Sekunde (gerade länger), Zahl alle zwei Sekunden – bei Platzmangel alle vier.
    for (second in 0..CURVE_SECONDS) {
        val x = layout.xAt(second.toFloat())
        val major = second % 2 == 0
        drawLine(textColor, Offset(x, layout.ticksTop), Offset(x, layout.ticksTop + if (major) layout.gapSmall else layout.gapTiny), thin)
        if (second % layout.labelStep == 0) {
            val label = layout.tickLabels[second]
            drawText(label, textColor, Offset(x - label.size.width / 2f, layout.tickLabelTop))
        }
    }

    // Führungslinien der Griffe, dezent; gestrichelt bei einem Halten mit 0 s.
    curveHandles(spec, layout).forEach { handle ->
        val color = (if (handle.field.isInhaleSide()) inhaleColor else exhaleColor).copy(alpha = TERTIARY)
        drawLine(
            color,
            Offset(handle.center.x, layout.yFull - layout.holdOffset),
            Offset(handle.center.x, layout.yEmpty + layout.holdOffset),
            thin,
            pathEffect = if (handle.zero) dashed else null,
        )
    }
}

/** Griff: Kreis im Grundton mit Ring in Phasenfarbe; bei 0 s gestrichelter Ring. */
private fun DrawScope.drawHandle(handle: CurveHandle, layout: CurveLayout, color: Color, fill: Color) {
    val ring = Sizes.CurveHoldStroke.toPx()
    val radius = layout.handle / 2f
    drawCircle(fill, radius = radius, center = handle.center)
    drawCircle(
        color,
        radius = radius - ring / 2f,
        center = handle.center,
        style = Stroke(
            width = ring,
            pathEffect = if (handle.zero) PathEffect.dashPathEffect(floatArrayOf(Dimens.GapHairline.toPx(), ring)) else null,
        ),
    )
}

@Preview
@Composable
private fun BreathCurveEditorPreview() {
    AtemkraftTheme {
        BreathCurveEditor(spec = PatternSpec(inhale = 4.0, holdFull = 1.0, exhale = 6.0, holdEmpty = null, activating = false), onChange = { _, _ -> })
    }
}
