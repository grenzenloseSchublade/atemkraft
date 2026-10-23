package app.atemkraft.ui.patterns.curve

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import kotlin.math.roundToInt

/** Lücke der gestrichelten Halten-Phasen im Verhältnis zur Strichlänge (Vorlage 6/5). */
private const val DASH_GAP_RATIO = 5f / 6f

private const val NANOS_PER_SECOND = 1_000_000_000f

/** Kleinste sichtbare Breite des Bands, auch wenn der erlaubte Bereich rechts aus der Achse läuft. */
private const val MIN_BAND_SECONDS = 0.25f

/** Nur für Tests: die Phase, deren Griff gerade gezogen wird (fehlt ohne Ziehen). */
internal val CurveDragPhase = SemanticsPropertyKey<Int>("CurveDragPhase")

/** Nur für Tests: Zielwert der Zeitachse in Sekunden (ändert sich erst nach dem Loslassen). */
internal val CurveAxisSeconds = SemanticsPropertyKey<Int>("CurveAxisSeconds")

/** Nur für Tests: Beschriftung des gelben Bands, solange es zu sehen ist. */
internal val CurveBandLabel = SemanticsPropertyKey<String>("CurveBandLabel")

/** Nur für Tests: Höhen der Kurve in px ([CurveMetrics.minHeight] und [CurveMetrics.preferredHeight]). */
internal val CurveHeightRange = SemanticsPropertyKey<ClosedFloatingPointRange<Float>>("CurveHeightRange")

/**
 * Höhen der Atemkurve ([BreathCurveEditor]) mit der aktuellen Schrift: [preferred] ist die volle
 * Höhe (Abstand „voll“–„leer“ [Sizes.CurveSpan], Platz für Beschriftungen über dem Halten-voll-
 * Griff), [min] die kleinste, bei der Griffe und Beschriftungen noch sicher Platz haben
 * ([Sizes.CurveSpanMin]). [fit] wählt für den freien Platz die größte Höhe dazwischen; der Aufrufer
 * gibt sie der Kurve per Modifier (`Modifier.height`) – nie mehr als [preferred].
 */
@Immutable
data class BreathCurveHeights(val min: Dp, val preferred: Dp) {
    /** Höhe für [available] freien Platz: so viel wie möglich, aber zwischen [min] und [preferred]. */
    fun fit(available: Dp): Dp = available.coerceIn(min, preferred)
}

/** Misst [BreathCurveHeights] mit der aktuellen Schrift (für die Aufteilung des Bildschirms). */
@Composable
fun rememberBreathCurveHeights(): BreathCurveHeights {
    val metrics = rememberCurveMetrics()
    val density = LocalDensity.current
    return remember(metrics, density) {
        with(density) { BreathCurveHeights(min = metrics.minHeight.toDp(), preferred = metrics.preferredHeight.toDp()) }
    }
}

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
 * Griff am Ende jeder Phase, immer auf der Kurve (auch beim Halten: am Ende seiner Linie;
 * Tippfläche ≥ 48 dp, bei Überlappung gewinnt der nächste). Antippen eines Griffs oder einer Linie
 * (bis eine halbe Tippfläche daneben, [nearestSegment]) wählt die Phase ([onSelect]),
 * horizontales Ziehen an einem Griff ändert ihre Dauer – erst nach der waagrechten Touch-Slop, ein senkrechtes
 * Wischen auf einem Griff scrollt die Seite und ändert nichts: rastet auf ganze Sekunden,
 * geklemmt auf 1–30 s, Wert über [onChangeSeconds]; spätere Phasen rücken mit. Je Raste ein Tick als Berührungs-Rückmeldung
 * (Android-Einstellung, wie die Rasten des Zeitrings, AUDIO-05). Während des Ziehens zeigt ein
 * gelbes Band auf der Zeitleiste den erlaubten Bereich („1 bis 30 s“).
 *
 * Vorschau ([preview]): Läuft sie, wandert ein Punkt in Echtzeit über die Kurve. Die Knöpfe
 * dazu ([CurvePreviewControls]) stellt der Aufrufer neben die Kurve (Muster-Editor: in den Kopf
 * „Atemzug“), ebenso den Phasennamen und den Ton (`PreviewSoundEffect` mit [CurvePreviewState]).
 * Eine eigene Zeile unter der Kurve gibt es nicht – die Werkzeugleiste des Editors rückt direkt
 * darunter.
 *
 * Höhe: bevorzugt [BreathCurveHeights.preferred]; gibt der Aufrufer per [modifier] weniger
 * (`Modifier.height`, siehe [rememberBreathCurveHeights]), weicht zuerst der Platz für Beschriftungen
 * über „voll“ (Beschriftungen eines Haltens rücken dann dicht an die Linie), dann der Abstand „voll“–„leer“, bis
 * hinab zu [Sizes.CurveSpanMin].
 *
 * TalkBack: Die Kurve ist ein Knoten mit `contentDescription` („Atemkurve: Einatmen 4 Sekunden,
 * …“) ohne liveRegion. Ziehen ist nie Pflicht – Auswahl, Dauer, Entfernen und Hinzufügen bietet
 * der Aufrufer daneben (Werkzeugleiste, Ablauf-Liste).
 */
@Composable
fun BreathCurveEditor(
    pattern: FreePattern,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onChangeSeconds: (index: Int, seconds: Int) -> Unit,
    modifier: Modifier = Modifier,
    preview: CurvePreviewState = rememberCurvePreviewState(),
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
    // Vorschau: Die Zeit läuft in Echtzeit im Kreis (Knöpfe nur mit System-Animationen, MOTION-02).
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
    Box(modifier = modifier.background(colors.surface, MaterialTheme.shapes.medium)) {
        val density = LocalDensity.current
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { metrics.preferredHeight.toDp() })
                .clipToBounds()
                .semantics {
                    contentDescription = description
                    this[CurveAxisSeconds] = axisTarget
                    this[CurveHeightRange] = metrics.minHeight..metrics.preferredHeight
                    drag?.let { this[CurveDragPhase] = it.index }
                    bandLabel?.let { this[CurveBandLabel] = it }
                }
                .pointerInput(metrics, preview) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = currentPattern
                        val layout = metrics.layout(size.width.toFloat(), currentAxis, currentAxisTarget, size.height.toFloat())
                        val handle = nearestHandle(curveHandles(start, layout), down.position, layout.touchRadius)
                        if (handle == null) {
                            // Neben den Griffen: Ein Tipp auf eine Linie wählt ihre Phase (nur
                            // ein Tipp, kein Zug; ein Wischen bleibt beim Scrollen der Seite).
                            val line = nearestSegment(start, layout, down.position, layout.touchRadius / 2f) ?: return@awaitEachGesture
                            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                            if ((up.position - down.position).getDistance() <= viewConfiguration.touchSlop) {
                                up.consume()
                                preview.pause()
                                currentOnSelect(line)
                            }
                            return@awaitEachGesture
                        }
                        val index = handle.index
                        // Erst ein waagrechter Zug über die Touch-Slop gehört der Kurve; ein
                        // senkrechtes Wischen bleibt beim Scrollen der Seite und ändert nichts.
                        var first: PointerInputChange? = null
                        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                            first = change
                        }
                        if (dragStart == null) {
                            // Losgelassen, ohne dass der Finger wanderte oder die Seite scrollte:
                            // ein Tipp wählt den Griff.
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            val still = up != null && (up.position - down.position).getDistance() <= viewConfiguration.touchSlop
                            if (up != null && still && up.changedToUp() && !up.isConsumed) {
                                up.consume()
                                preview.pause()
                                currentOnSelect(index)
                            }
                            return@awaitEachGesture
                        }
                        preview.pause()
                        currentOnSelect(index)
                        drag = CurveDrag(index, currentAxisTarget)
                        val phaseStart = layout.xAt(start.startOf(index).toFloat())
                        // Griffstelle relativ zum Phasenende: Wer neben die Mitte greift, verschiebt
                        // nichts, bis der Finger zieht.
                        val grab = down.position.x - handle.center.x
                        var last = start.phases[index].seconds
                        val follow = { change: PointerInputChange ->
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
                        try {
                            first?.let(follow)
                            drag(dragStart.id, follow)
                        } finally {
                            // Erst jetzt rechnet die Achse neu (sanft animiert).
                            drag = null
                        }
                    }
                },
        ) {
            val layout = metrics.layout(size.width, axis, axisTarget, size.height)
            // Das Band liegt auf der Zeitleiste; Zahlen unter seiner Beschriftung fallen solange weg.
            val band = drag?.let { bandGeometry(pattern, it, layout, bandLabel.orEmpty(), size.width) }
            drawCurve(pattern, selected, layout, palette, colors.outlineVariant, textColor, hideNumbersIn = band?.labelRect)
            band?.let { drawBand(it, layout) }
            if (preview.playing) {
                drawCircle(colors.onSurface, radius = Sizes.CurveDot.toPx() / 2f, center = pattern.pointAt(preview.time, layout))
            }
            // Der gewählte Griff zuletzt, damit er über Nachbarn liegt.
            curveHandles(pattern, layout).sortedBy { it.index == selected }.forEach { handle ->
                drawHandle(handle, selected = handle.index == selected, color = palette.colorOf(pattern, handle.index), fill = colors.background)
            }
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

private fun DrawScope.drawCurve(
    pattern: FreePattern,
    selected: Int?,
    layout: CurveLayout,
    palette: CurvePalette,
    lineColor: Color,
    textColor: Color,
    hideNumbersIn: Rect? = null,
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
    }
    // „4 s“ je Phase, nur wo frei (siehe [curveLabelSpots]); die Ablauf-Liste trägt alle Werte.
    val texts = pattern.phases.map { layout.measurer.measure("${it.seconds}${layout.secondsSuffix}", layout.phaseStyle) }
    val spots = curveLabelSpots(pattern, selected, layout, size.width, texts.map { Size(it.size.width.toFloat(), it.size.height.toFloat()) })
    spots.forEachIndexed { i, spot ->
        if (spot != null) drawText(texts[i], palette.colorOf(pattern, i), spot.topLeft)
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
        val topLeft = Offset(layout.xAt(s.toFloat()) - result.size.width / 2f, layout.tickLabelTop)
        val hidden = hideNumbersIn?.overlaps(Rect(topLeft, Size(result.size.width.toFloat(), result.size.height.toFloat()))) == true
        if (!hidden) drawText(result, textColor, topLeft)
    }
}

/** Gelbes Band auf der Zeitleiste ([rect]) und seine Beschriftung [label] auf der Zeile der Zahlen ([labelRect]). */
private class CurveBand(val rect: Rect, val label: TextLayoutResult, val labelRect: Rect)

/**
 * Lage des Bands: erlaubter Bereich der gezogenen Phase (Ende = Start + 1 bis 30 s) über Strichen
 * und Zahlen der Zeitleiste, die Beschriftung mittig darin auf der Zeile der Zahlen (innerhalb des
 * Innenrands; die Zahlen darunter fallen solange weg, [labelRect] mit `GapSmall` Luft).
 */
private fun bandGeometry(pattern: FreePattern, drag: CurveDrag, layout: CurveLayout, label: String, width: Float): CurveBand {
    val start = pattern.startOf(drag.index)
    val left = layout.xAt((start + FreePattern.MIN_SECONDS).toFloat())
    val right = layout.xAt(minOf((start + FreePattern.MAX_SECONDS).toFloat(), maxOf(layout.axis, start + FreePattern.MIN_SECONDS + MIN_BAND_SECONDS)))
    val rect = Rect(Offset(left, layout.bandTop), Size((right - left).coerceAtLeast(layout.gapTiny), layout.bandHeight))
    val text = layout.measurer.measure(label, layout.smallStyle)
    val x = ((left + right) / 2f - text.size.width / 2f).coerceIn(layout.gapSmall, (width - layout.gapSmall - text.size.width).coerceAtLeast(layout.gapSmall))
    val labelRect = Rect(Offset(x, layout.tickLabelTop), Size(text.size.width.toFloat(), text.size.height.toFloat()))
    return CurveBand(rect, text, labelRect.inflate(layout.gapSmall))
}

private fun DrawScope.drawBand(band: CurveBand, layout: CurveLayout) {
    val radius = CornerRadius(layout.gapTiny, layout.gapTiny)
    drawRoundRect(WarnAmber.copy(alpha = CHIP_FILL), band.rect.topLeft, band.rect.size, radius)
    drawRoundRect(WarnAmber.copy(alpha = TERTIARY), band.rect.topLeft, band.rect.size, radius, style = Stroke(Dimens.BorderThin.toPx()))
    drawText(band.label, WarnAmber, Offset(band.labelRect.left + layout.gapSmall, layout.tickLabelTop))
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
