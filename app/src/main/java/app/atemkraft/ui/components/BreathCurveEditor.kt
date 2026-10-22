package app.atemkraft.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.ui.theme.ALPHA_CURVE_FILL
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.CHIP_FILL
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Schrittweiten der Zahlen an der Zeitleiste in Sekunden: Es gilt die kleinste, bei der zwei
 * Zahlen mindestens [Sizes.CurveLabelMinSpacing] auseinander stehen und zwischen ihnen mindestens
 * `GapSmall` frei bleibt (gemessen an der größten Zahl, mit und ohne „s“).
 */
internal val CURVE_LABEL_STEPS = listOf(1, 2, 5, 10, 15, 30, 60)

/** Lücke der gestrichelten Halten-Phasen im Verhältnis zur Strichlänge (Vorlage 6/5). */
private const val DASH_GAP_RATIO = 5f / 6f

/** Ab halb voll gehört ein Halten zur Einatmen-Seite (Cyan, Griff über der Linie). */
private const val HALF = 0.5f

/** Feine Striche zwischen den Zahlen ab Schritt 10 s: ein Fünftel des Schritts. */
private const val MINOR_DIVISOR = 5

private const val MINOR_FROM_STEP = 10

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val MILLIS_PER_SECOND = 1000f

/** Kleinste sichtbare Breite des Bands, auch wenn der erlaubte Bereich rechts aus der Achse läuft. */
private const val MIN_BAND_SECONDS = 0.25f

/** Rundungsrest beim Vergleich mit der (animierten) Achse. */
private const val AXIS_EPSILON = 1e-3f

/** Nur für Tests: die Phase, deren Griff gerade gezogen wird (fehlt ohne Ziehen). */
internal val CurveDragPhase = SemanticsPropertyKey<Int>("CurveDragPhase")

/** Nur für Tests: Zielwert der Zeitachse in Sekunden (ändert sich erst nach dem Loslassen). */
internal val CurveAxisSeconds = SemanticsPropertyKey<Int>("CurveAxisSeconds")

/** Nur für Tests: Beschriftung des gelben Bands, solange es zu sehen ist. */
internal val CurveBandLabel = SemanticsPropertyKey<String>("CurveBandLabel")

/**
 * Zustand der Vorschau unter der Atemkurve: ob sie läuft und wo sie im Atemzug steht ([time],
 * Sekunden ab Beginn des Durchlaufs). Hochgezogen, damit der Aufrufer sie anhalten kann (Seite
 * nicht mehr sichtbar) und den Ton danach taktet:
 *
 * ```
 * val phase by remember(pattern) { derivedStateOf { preview.phaseIndexIn(pattern) } }
 * PreviewSoundEffect(sound, preview.playing, phase) { /* PhaseAudio mit preview.remainingMillisIn(pattern) */ }
 * ```
 * Anlegen mit [rememberCurvePreviewState].
 */
@Stable
class CurvePreviewState internal constructor() {
    /** Läuft die Vorschau? */
    var playing by mutableStateOf(false)
        private set

    /** Zeit im Atemzug in Sekunden (0, solange die Vorschau nicht lief). */
    var time by mutableFloatStateOf(0f)
        internal set

    /** Startet die Vorschau am Anfang des Atemzugs (wie die Vorlage). */
    fun play() {
        time = 0f
        playing = true
    }

    /** Hält die Vorschau an (Ziehen, Seite verlassen, App im Hintergrund). */
    fun pause() {
        playing = false
    }

    /** Wechselt zwischen [play] und [pause]. */
    fun toggle() = if (playing) pause() else play()

    /** Index der Phase von [pattern], in der die Vorschau steht. */
    fun phaseIndexIn(pattern: FreePattern): Int = pattern.phaseIndexAt(time)

    /** Restdauer der aktuellen Phase von [pattern] in ms (für den Dauerton beim Einschalten mitten in der Phase). */
    fun remainingMillisIn(pattern: FreePattern): Long {
        val index = phaseIndexIn(pattern)
        val end = pattern.startOf(index) + (pattern.phases.getOrNull(index)?.seconds ?: 0)
        return ((end - time) * MILLIS_PER_SECOND).roundToLong().coerceAtLeast(0L)
    }
}

/** Legt den Zustand der Vorschau für [BreathCurveEditor] an. */
@Composable
fun rememberCurvePreviewState(): CurvePreviewState = remember { CurvePreviewState() }

/**
 * Atemkurve des Muster-Editors (MUSTER-10, Nachtrag 4 „Frei gestalten“): die freie Phasenfolge
 * [pattern] als Kurve, Hauptbedienung des Editors. x = Zeit, y = Lungenfüllung (leer unten, voll
 * oben); mehrere Ein bzw. Aus hintereinander steigen in Stufen ([FreePattern.levels]). Einatmen
 * durchgezogen in `secondary`, Ausatmen durchgezogen in [BreathExhale], Halten gestrichelt in der
 * Farbe seiner Höhe (ab halb voll `secondary`, darunter [BreathExhale]) – dieselbe Farbsprache wie
 * die Rhythmusbalken der Atemlehre (FARBE-04). Je Phase „4 s“ in Phasenfarbe, darunter eine
 * Zeitleiste; die gewählte Phase [selected] ist dicker gezeichnet, ihr Griff größer.
 *
 * Zeitachse: max(12, gerade(Gesamt + 6)) s ([FreePattern.axisSeconds]), neu erst nach dem
 * Loslassen (oder wenn sich die Folge anders ändert) und dann sanft animiert – während des Ziehens
 * springt nichts. Zahlen im Schritt aus [CURVE_LABEL_STEPS], feine Striche je Sekunde, solange sie
 * mindestens [Sizes.CurveTickMinSpacing] auseinander liegen, „s“ nur an der letzten Zahl.
 *
 * Griff am Ende jeder Phase (Ein-/Aus-Ende auf der Kurve, Halten über bzw. unter seiner Linie,
 * Tippfläche ≥ 48 dp, bei Überlappung gewinnt der nächste). Antippen wählt die Phase ([onSelect]),
 * horizontales Ziehen ändert ihre Dauer: rastet auf ganze Sekunden, geklemmt auf 1–30 s, Wert über
 * [onChangeSeconds]; spätere Phasen rücken mit. Je Raste ein Tick als Berührungs-Rückmeldung
 * (Android-Einstellung, wie die Rasten des Zeitrings, AUDIO-05). Während des Ziehens zeigt ein
 * gelbes Band über der Zeitleiste den erlaubten Bereich („1 bis 30 s“).
 *
 * Darunter die Vorschau ([preview]): Play/Pause lässt einen Punkt in Echtzeit über die Kurve
 * laufen, daneben der Phasenname (statt des Hinweises „Griffe ziehen“). Der Ton kommt vom Aufrufer
 * (`PreviewSoundEffect` mit [CurvePreviewState]), sein Lautsprecher-Schalter steht in
 * [previewTrailing] am Ende der Zeile. Bei abgeschalteten System-Animationen entfallen Knopf und
 * Schalter (MOTION-02).
 *
 * TalkBack: Die Kurve ist ein Knoten mit `contentDescription` („Atemkurve: Einatmen 4 Sekunden,
 * …“) ohne liveRegion; Ziehen ist nie Pflicht – Auswahl, Dauer und Entfernen bietet der Aufrufer
 * in der Ablauf-Liste darunter.
 */
@Composable
fun BreathCurveEditor(
    pattern: FreePattern,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onChangeSeconds: (index: Int, seconds: Int) -> Unit,
    modifier: Modifier = Modifier,
    preview: CurvePreviewState = rememberCurvePreviewState(),
    previewTrailing: @Composable RowScope.() -> Unit = {},
) {
    val currentPattern by rememberUpdatedState(pattern)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnChange by rememberUpdatedState(onChangeSeconds)
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val palette = CurvePalette(inhale = colors.secondary, exhale = BreathExhale)
    val metrics = rememberCurveMetrics()
    val textColor = colors.onSurface.copy(alpha = TERTIARY)

    // Ziehen: welche Phase, und die Achse, die bis zum Loslassen stehen bleibt.
    var drag by remember { mutableStateOf<CurveDrag?>(null) }
    val axisTarget = drag?.axis ?: pattern.axisSeconds()
    val axis by animateFloatAsState(axisTarget.toFloat(), label = "curveAxis")
    val currentAxis by rememberUpdatedState(axis)
    val currentAxisTarget by rememberUpdatedState(axisTarget)

    val description = stringResource(R.string.cd_breath_curve, pattern.spokenDescription())
    val bandLabel = drag?.let { d ->
        val start = pattern.startOf(d.index)
        if (start + FreePattern.MAX_SECONDS > d.axis) {
            stringResource(R.string.pattern_editor_curve_band_beyond, FreePattern.MIN_SECONDS, FreePattern.MAX_SECONDS)
        } else {
            stringResource(R.string.pattern_editor_curve_band, FreePattern.MIN_SECONDS, FreePattern.MAX_SECONDS)
        }
    }
    // Vorschau: nur mit System-Animationen (MOTION-02); die Zeit läuft in Echtzeit im Kreis.
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    LaunchedEffect(preview, preview.playing) {
        if (!preview.playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val cycle = currentPattern.totalSeconds.toFloat()
                preview.time = if (cycle > 0f) (preview.time + (now - last) / NANOS_PER_SECOND) % cycle else 0f
                last = now
            }
        }
    }
    val phaseNow by remember(pattern) { derivedStateOf { pattern.phaseIndexAt(preview.time) } }

    Column(modifier = modifier.background(colors.surface, MaterialTheme.shapes.medium)) {
        val density = LocalDensity.current
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { metrics.height.toDp() })
                .clipToBounds()
                .semantics {
                    contentDescription = description
                    this[CurveAxisSeconds] = axisTarget
                    drag?.let { this[CurveDragPhase] = it.index }
                    bandLabel?.let { this[CurveBandLabel] = it }
                }
                .pointerInput(metrics, preview) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = currentPattern
                        val layout = metrics.layout(size.width.toFloat(), currentAxis, currentAxisTarget)
                        val handle = nearestHandle(curveHandles(start, layout), down.position, layout.touchRadius)
                            ?: return@awaitEachGesture
                        down.consume()
                        preview.pause()
                        val index = handle.index
                        currentOnSelect(index)
                        drag = CurveDrag(index, currentAxisTarget)
                        val phaseStart = layout.xAt(start.startOf(index).toFloat())
                        // Griffstelle relativ zum Phasenende: Wer neben die Mitte greift, verschiebt
                        // nichts, bis der Finger zieht.
                        val grab = down.position.x - handle.center.x
                        var last = start.phases[index].seconds
                        try {
                            drag(down.id) { change ->
                                change.consume()
                                val value = ((change.position.x - grab - phaseStart) / layout.pxPerSecond)
                                    .roundToInt()
                                    .coerceIn(FreePattern.SECONDS_RANGE)
                                if (value != last) {
                                    last = value
                                    currentOnChange(index, value)
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                        } finally {
                            // Erst jetzt rechnet die Achse neu (sanft animiert).
                            drag = null
                        }
                    }
                },
        ) {
            val layout = metrics.layout(size.width, axis, axisTarget)
            drawCurve(pattern, selected, layout, palette, colors.outlineVariant, textColor)
            drag?.let { drawBand(pattern, it, layout, bandLabel.orEmpty()) }
            if (preview.playing) {
                drawCircle(colors.onSurface, radius = Sizes.CurveDot.toPx() / 2f, center = pattern.pointAt(preview.time, layout))
            }
            // Der gewählte Griff zuletzt, damit er über Nachbarn liegt.
            curveHandles(pattern, layout).sortedBy { it.index == selected }.forEach { handle ->
                drawHandle(handle, selected = handle.index == selected, color = palette.colorOf(pattern, handle.index), fill = colors.background)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(start = if (animationsEnabled) Dimens.GapTiny else Dimens.CardPadding, end = if (animationsEnabled) Dimens.GapTiny else Dimens.ListGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (animationsEnabled) {
                AppIconButton(onClick = preview::toggle) {
                    if (preview.playing) {
                        Icon(painterResource(R.drawable.ic_pause), contentDescription = stringResource(R.string.cd_preview_pause), tint = palette.inhale)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.cd_preview_play), tint = palette.inhale)
                    }
                }
            }
            // Läuft die Vorschau, steht dort der Phasenname (ohne liveRegion, er wechselt im Takt),
            // sonst der Hinweis „Griffe ziehen“ – beim Zuschauen ist er nicht gefragt. Beide
            // brechen nur zwischen Wörtern um, nie im Wort (LAYOUT-03).
            if (preview.playing && phaseNow in pattern.phases.indices) {
                WholeWordText(
                    text = pattern.labelAt(phaseNow),
                    modifier = Modifier.weight(1f).padding(start = Dimens.GapSmall),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.colorOf(pattern, phaseNow),
                )
            } else {
                CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                    HintText(
                        text = stringResource(R.string.pattern_editor_curve_hint),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Dimens.GapSmall, end = if (animationsEnabled) Dimens.GapSmall else Dimens.GapTiny)
                            .wrapContentWidth(Alignment.End),
                    )
                }
            }
            if (animationsEnabled) previewTrailing()
        }
    }
}

/** Ein laufender Zug: Phase [index], und die Achse [axis] in Sekunden, die bis zum Loslassen gilt. */
private data class CurveDrag(val index: Int, val axis: Int)

/** Die beiden Phasenfarben der Kurve. */
@Immutable
private class CurvePalette(val inhale: Color, val exhale: Color) {
    /** Farbe der Phase [index]: Ein Cyan, Aus Koralle, Halten nach seiner Höhe (ab halb voll Cyan). */
    fun colorOf(pattern: FreePattern, index: Int): Color = if (pattern.isInhaleSide(index)) inhale else exhale
}

/** Cyan-Seite: Einatmen, oder ein Halten ab halb voll. */
private fun FreePattern.isInhaleSide(index: Int): Boolean = when (phases[index].type) {
    FreePhaseType.IN -> true
    FreePhaseType.OUT -> false
    FreePhaseType.HOLD -> levels()[index].from >= HALF
}

/** Beginn der Phase [index] in Sekunden ab Beginn des Durchlaufs. */
internal fun FreePattern.startOf(index: Int): Int = phases.take(index.coerceIn(0, phases.size)).sumOf { it.seconds }

/** Index der Phase zum Zeitpunkt [time] (Sekunden ab Beginn des Durchlaufs); hinter dem Ende die letzte. */
internal fun FreePattern.phaseIndexAt(time: Float): Int {
    var end = 0
    phases.forEachIndexed { i, phase ->
        end += phase.seconds
        if (time < end) return i
    }
    return phases.lastIndex.coerceAtLeast(0)
}

/** Ort des Vorschau-Punkts auf der Kurve zum Zeitpunkt [time]. */
private fun FreePattern.pointAt(time: Float, layout: CurveLayout): Offset {
    val index = phaseIndexAt(time)
    val level = levels()[index]
    val progress = ((time - startOf(index)) / phases[index].seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    return Offset(layout.xAt(time), layout.yAt(level.from + (level.to - level.from) * progress))
}

/** Zahlen-Schritt der Zeitleiste: der kleinste aus [CURVE_LABEL_STEPS], bei dem Zahlen [minSpacing] px auseinander liegen. */
internal fun curveLabelStep(pxPerSecond: Float, minSpacing: Float): Int = CURVE_LABEL_STEPS.firstOrNull { it * pxPerSecond >= minSpacing } ?: CURVE_LABEL_STEPS.last()

/**
 * Abstand der Striche in Sekunden: je Sekunde, solange die Striche [fineMin] px auseinander
 * liegen; sonst ab Schritt 10 s ein Fünftel des Schritts, darunter nur an den Zahlen.
 */
internal fun curveMinorStep(labelStep: Int, pxPerSecond: Float, fineMin: Float): Int = when {
    pxPerSecond >= fineMin -> 1
    labelStep >= MINOR_FROM_STEP -> labelStep / MINOR_DIVISOR
    else -> labelStep
}

/**
 * Maße der Kurve, die nur von Dichte und Schrift abhängen (in px). [layout] ergänzt Breite und
 * Achse. Von oben: Beschriftung über dem Halten-voll-Griff, Griff, Linie „voll“, Kurve, Linie
 * „leer“, Halten-leer-Griff, seine Beschriftung, Band des erlaubten Bereichs, Zeitleiste mit Zahlen.
 */
@Immutable
internal class CurveMetrics(
    private val measurer: TextMeasurer,
    /** Stil der Beschriftung „4 s“ an den Phasen. */
    val phaseStyle: TextStyle,
    /** Stil der Zahlen, der Linien „voll“/„leer“ und des Bands. */
    val smallStyle: TextStyle,
    private val secondsSuffix: String,
    private val fullLabel: String,
    private val emptyLabel: String,
    private val labelHeight: Float,
    private val smallHeight: Float,
    private val gapTiny: Float,
    private val gapSmall: Float,
    private val hairline: Float,
    private val inset: Float,
    private val span: Float,
    private val holdOffset: Float,
    private val handle: Float,
    private val touch: Float,
    private val labelSpacing: Float,
    private val tickSpacing: Float,
) {
    private val yFull = gapTiny + labelHeight + hairline + holdOffset + handle / 2f
    private val yEmpty = yFull + span
    private val emptyLabelTop = yEmpty + holdOffset + handle / 2f + hairline
    private val bandTop = emptyLabelTop + labelHeight + gapTiny
    private val bandHeight = smallHeight + 2 * hairline
    private val ticksTop = bandTop + bandHeight + gapTiny
    private val tickLabelTop = ticksTop + gapSmall + hairline

    /** Gesamthöhe der Zeichenfläche. */
    val height: Float = tickLabelTop + smallHeight + gapTiny

    /** Lagen für die Breite [width] und die (ggf. gerade animierte) Achse [axis]; [axisTarget] bestimmt den rechten Rand. */
    fun layout(width: Float, axis: Float, axisTarget: Int = axis.roundToInt()): CurveLayout {
        // Die breitesten Zahlen: die größte ohne und mit „s“ (nur die letzte trägt es).
        val number = measurer.measure("$axisTarget", smallStyle).size.width
        val withUnit = measurer.measure("$axisTarget$secondsSuffix", smallStyle).size.width
        val x0 = inset + measurer.measure("0", smallStyle).size.width / 2f
        val x1 = width - gapSmall - withUnit / 2f
        val pxPerSecond = (x1 - x0) / axis.coerceAtLeast(1f)
        val step = curveLabelStep(pxPerSecond, maxOf(labelSpacing, (number + withUnit) / 2f + gapSmall))
        return CurveLayout(
            x0 = x0,
            x1 = x1,
            axis = axis,
            pxPerSecond = pxPerSecond,
            labelStep = step,
            minorStep = curveMinorStep(step, pxPerSecond, tickSpacing),
            yFull = yFull,
            yEmpty = yEmpty,
            holdOffset = holdOffset,
            handle = handle,
            touchRadius = touch / 2f,
            bandTop = bandTop,
            bandHeight = bandHeight,
            ticksTop = ticksTop,
            tickLabelTop = tickLabelTop,
            gapTiny = gapTiny,
            gapSmall = gapSmall,
            hairline = hairline,
            measurer = measurer,
            phaseStyle = phaseStyle,
            smallStyle = smallStyle,
            secondsSuffix = secondsSuffix,
            fullLabel = fullLabel,
            emptyLabel = emptyLabel,
        )
    }
}

/** Alle Lagen der Kurve in px für eine Breite und Achse (siehe [CurveMetrics]). */
internal class CurveLayout(
    val x0: Float,
    val x1: Float,
    /** Länge der Zeitachse in Sekunden (während der Animation auch dazwischen). */
    val axis: Float,
    val pxPerSecond: Float,
    /** Zahlen an der Zeitleiste alle so viele Sekunden ([CURVE_LABEL_STEPS]). */
    val labelStep: Int,
    /** Striche an der Zeitleiste alle so viele Sekunden ([curveMinorStep]). */
    val minorStep: Int,
    val yFull: Float,
    val yEmpty: Float,
    val holdOffset: Float,
    val handle: Float,
    val touchRadius: Float,
    val bandTop: Float,
    val bandHeight: Float,
    val ticksTop: Float,
    val tickLabelTop: Float,
    val gapTiny: Float,
    val gapSmall: Float,
    val hairline: Float,
    val measurer: TextMeasurer,
    val phaseStyle: TextStyle,
    val smallStyle: TextStyle,
    val secondsSuffix: String,
    /** Beschriftung der Linien „voll“ und „leer“. */
    val fullLabel: String,
    val emptyLabel: String,
) {
    fun xAt(seconds: Float): Float = x0 + seconds * pxPerSecond

    /** Höhe zum Füllstand [level] (0 = leer, 1 = voll). */
    fun yAt(level: Float): Float = yEmpty - (yEmpty - yFull) * level

    /** Die Zahlen der Zeitleiste in Sekunden; die letzte trägt das „s“. */
    fun labelSeconds(): List<Int> = (0..floor(axis + AXIS_EPSILON).toInt() step labelStep).toList()
}

/** Misst die Beschriftungen der Kurve mit der aktuellen Schrift; auch für Tests, die Griffe treffen wollen. */
@Composable
internal fun rememberCurveMetrics(): CurveMetrics {
    val density = LocalDensity.current
    val measurer: TextMeasurer = rememberTextMeasurer(cacheSize = MEASURE_CACHE)
    val labelStyle = MaterialTheme.typography.labelMedium
    val smallStyle = MaterialTheme.typography.labelSmall
    // „ s“ mit geschütztem Leerzeichen aus der Vorlage „%1$d s“, damit Zahl und Einheit beisammen bleiben.
    val suffix = stringResource(R.string.pattern_editor_curve_seconds, 0).removePrefix("0")
    val full = stringResource(R.string.pattern_editor_curve_full)
    val empty = stringResource(R.string.pattern_editor_curve_empty)
    return remember(density, measurer, labelStyle, smallStyle, suffix, full, empty) {
        with(density) {
            CurveMetrics(
                measurer = measurer,
                phaseStyle = labelStyle,
                smallStyle = smallStyle,
                secondsSuffix = suffix,
                fullLabel = full,
                emptyLabel = empty,
                labelHeight = measurer.measure("30$suffix", labelStyle).size.height.toFloat(),
                smallHeight = measurer.measure("30$suffix", smallStyle).size.height.toFloat(),
                gapTiny = Dimens.GapTiny.toPx(),
                gapSmall = Dimens.GapSmall.toPx(),
                hairline = Dimens.GapHairline.toPx(),
                inset = Dimens.CardPadding.toPx(),
                span = Sizes.CurveSpan.toPx(),
                holdOffset = Sizes.CurveHoldHandleOffset.toPx(),
                handle = Sizes.CurveHandle.toPx(),
                touch = Dimens.MinTouchTarget.toPx(),
                labelSpacing = Sizes.CurveLabelMinSpacing.toPx(),
                tickSpacing = Sizes.CurveTickMinSpacing.toPx(),
            )
        }
    }
}

/** Zahlen, Phasen-Beschriftungen und Band einer Kurve mit 12 Phasen passen in den Cache. */
private const val MEASURE_CACHE = 48

/** Ein Griff: Phase [index] und Mittelpunkt in px. */
internal data class CurveHandle(val index: Int, val center: Offset)

/**
 * Griffe der Kurve, einer am Ende jeder Phase: Ein- und Aus-Ende auf der Kurve, ein Halten ab
 * halb voll über seiner Linie, darunter unter ihr.
 */
internal fun curveHandles(pattern: FreePattern, layout: CurveLayout): List<CurveHandle> {
    val levels = pattern.levels()
    var end = 0
    return pattern.phases.mapIndexed { i, phase ->
        end += phase.seconds
        val y = layout.yAt(levels[i].to)
        val shift = when {
            phase.type != FreePhaseType.HOLD -> 0f
            levels[i].to >= HALF -> -layout.holdOffset
            else -> layout.holdOffset
        }
        CurveHandle(i, Offset(layout.xAt(end.toFloat()), y + shift))
    }
}

/** Der Griff, dessen Tippfläche (Kreis mit [radius]) [position] trifft; bei Überlappung der nächste. */
internal fun nearestHandle(handles: List<CurveHandle>, position: Offset, radius: Float): CurveHandle? = handles
    .map { it to (it.center - position).getDistance() }
    .filter { it.second <= radius }
    .minByOrNull { it.second }
    ?.first

private fun DrawScope.drawCurve(
    pattern: FreePattern,
    selected: Int?,
    layout: CurveLayout,
    palette: CurvePalette,
    lineColor: Color,
    textColor: Color,
) {
    val levels = pattern.levels()
    val stroke = Sizes.CurveStroke.toPx()
    val holdStroke = Sizes.CurveHoldStroke.toPx()
    val selectedStroke = Sizes.CurveStrokeSelected.toPx()
    val dash = Sizes.CurveDash.toPx()
    val dashed = PathEffect.dashPathEffect(floatArrayOf(dash, dash * DASH_GAP_RATIO))
    val thin = Dimens.BorderThin.toPx()

    // Zarte Füllung unter der Kurve (Lungenfüllung).
    val fill = Path().apply {
        moveTo(layout.x0, layout.yEmpty)
        var t = 0
        pattern.phases.forEachIndexed { i, phase ->
            lineTo(layout.xAt(t.toFloat()), layout.yAt(levels[i].from))
            t += phase.seconds
            lineTo(layout.xAt(t.toFloat()), layout.yAt(levels[i].to))
        }
        lineTo(layout.xAt(t.toFloat()), layout.yEmpty)
        close()
    }
    drawPath(fill, palette.inhale.copy(alpha = ALPHA_CURVE_FILL))

    // Linien „voll“ und „leer“ (dekorativ) mit ihrer Beschriftung links.
    listOf(layout.yFull, layout.yEmpty).forEach { y ->
        drawLine(lineColor, Offset(layout.gapSmall, y), Offset(size.width - layout.gapSmall, y), strokeWidth = thin)
    }
    val full = layout.measurer.measure(layout.fullLabel, layout.smallStyle)
    val empty = layout.measurer.measure(layout.emptyLabel, layout.smallStyle)
    drawText(full, textColor, Offset(layout.gapSmall, layout.yFull - layout.hairline - full.size.height))
    drawText(empty, textColor, Offset(layout.gapSmall, layout.yEmpty + layout.hairline))

    // Phasen: Ein-/Ausatmen durchgezogen, Halten gestrichelt; die gewählte dicker. Daneben „4 s“.
    var t = 0
    pattern.phases.forEachIndexed { i, phase ->
        val from = Offset(layout.xAt(t.toFloat()), layout.yAt(levels[i].from))
        t += phase.seconds
        val to = Offset(layout.xAt(t.toFloat()), layout.yAt(levels[i].to))
        val color = palette.colorOf(pattern, i)
        val hold = phase.type == FreePhaseType.HOLD
        val width = when {
            i == selected -> selectedStroke
            hold -> holdStroke
            else -> stroke
        }
        drawLine(color, from, to, width, StrokeCap.Round, if (hold) dashed else null)
        drawPhaseLabel(phase, from, to, layout, color, upper = levels[i].from >= HALF)
    }

    // Zeitleiste: feine Striche, längere an den Zahlen; „s“ nur an der letzten Zahl.
    val labels = layout.labelSeconds()
    var second = 0
    while (second <= layout.axis + AXIS_EPSILON) {
        val x = layout.xAt(second.toFloat())
        val major = second % layout.labelStep == 0
        drawLine(textColor, Offset(x, layout.ticksTop), Offset(x, layout.ticksTop + if (major) layout.gapSmall else layout.gapTiny), thin)
        second += layout.minorStep
    }
    labels.forEach { s ->
        val text = if (s == labels.last()) "$s${layout.secondsSuffix}" else s.toString()
        val result = layout.measurer.measure(text, layout.smallStyle)
        drawText(result, textColor, Offset(layout.xAt(s.toFloat()) - result.size.width / 2f, layout.tickLabelTop))
    }
}

/** „4 s“ an einer Phase: Ein/Aus neben der Schräge, Halten über (obere Hälfte) bzw. unter seinem Griff. */
private fun DrawScope.drawPhaseLabel(
    phase: FreePhase,
    from: Offset,
    to: Offset,
    layout: CurveLayout,
    color: Color,
    upper: Boolean,
) {
    val text = layout.measurer.measure("${phase.seconds}${layout.secondsSuffix}", layout.phaseStyle)
    val w = text.size.width
    val h = text.size.height
    val midX = (from.x + to.x) / 2f
    val midY = (from.y + to.y) / 2f
    val reach = layout.holdOffset + layout.handle / 2f + layout.hairline
    val topLeft = when (phase.type) {
        FreePhaseType.IN -> Offset(midX - layout.gapSmall - w, midY - h)
        FreePhaseType.OUT -> Offset(midX + layout.gapSmall, midY - h)
        FreePhaseType.HOLD -> if (upper) Offset(midX - w / 2f, from.y - reach - h) else Offset(midX - w / 2f, from.y + reach)
    }
    drawText(text, color, topLeft)
}

/** Gelbes Band über der Zeitleiste: erlaubter Bereich der gezogenen Phase (Ende = Start + 1 bis 30 s). */
private fun DrawScope.drawBand(pattern: FreePattern, drag: CurveDrag, layout: CurveLayout, label: String) {
    val start = pattern.startOf(drag.index)
    val left = layout.xAt((start + FreePattern.MIN_SECONDS).toFloat())
    val right = layout.xAt(minOf((start + FreePattern.MAX_SECONDS).toFloat(), maxOf(layout.axis, start + FreePattern.MIN_SECONDS + MIN_BAND_SECONDS)))
    val radius = CornerRadius(layout.gapTiny, layout.gapTiny)
    val topLeft = Offset(left, layout.bandTop)
    val bandSize = Size((right - left).coerceAtLeast(layout.gapTiny), layout.bandHeight)
    drawRoundRect(WarnAmber.copy(alpha = CHIP_FILL), topLeft, bandSize, radius)
    drawRoundRect(WarnAmber.copy(alpha = TERTIARY), topLeft, bandSize, radius, style = Stroke(Dimens.BorderThin.toPx()))
    val text = layout.measurer.measure(label, layout.smallStyle)
    val x = ((left + right) / 2f - text.size.width / 2f).coerceIn(layout.gapSmall, (size.width - layout.gapSmall - text.size.width).coerceAtLeast(layout.gapSmall))
    drawText(text, WarnAmber, Offset(x, layout.bandTop + layout.hairline))
}

/** Griff: Kreis im Grundton mit Ring in Phasenfarbe; der gewählte größer. */
private fun DrawScope.drawHandle(handle: CurveHandle, selected: Boolean, color: Color, fill: Color) {
    val ring = Sizes.CurveHoldStroke.toPx()
    val radius = (if (selected) Sizes.CurveHandleSelected else Sizes.CurveHandle).toPx() / 2f
    drawCircle(fill, radius = radius, center = handle.center)
    drawCircle(color, radius = radius - ring / 2f, center = handle.center, style = Stroke(width = ring))
}

@Preview
@Composable
private fun BreathCurveEditorPreview() {
    AtemkraftTheme {
        BreathCurveEditor(
            pattern = FreePattern(
                listOf(
                    FreePhase(FreePhaseType.IN, 3),
                    FreePhase(FreePhaseType.OUT, 3),
                    FreePhase(FreePhaseType.IN, 4),
                    FreePhase(FreePhaseType.HOLD, 2),
                    FreePhase(FreePhaseType.OUT, 8),
                ),
            ),
            selected = 3,
            onSelect = {},
            onChangeSeconds = { _, _ -> },
        )
    }
}
