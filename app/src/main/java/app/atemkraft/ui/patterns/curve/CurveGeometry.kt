package app.atemkraft.ui.patterns.curve

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Schrittweiten der Zahlen an der Zeitleiste in Sekunden: Es gilt die kleinste, bei der zwei
 * Zahlen mindestens [Sizes.CurveLabelMinSpacing] auseinander stehen und zwischen ihnen mindestens
 * `GapSmall` frei bleibt (gemessen an der größten Zahl, mit und ohne „s“).
 */
internal val CURVE_LABEL_STEPS = listOf(1, 2, 5, 10, 15, 30, 60)

/** Feine Striche zwischen den Zahlen ab Schritt 10 s: ein Fünftel des Schritts. */
private const val MINOR_DIVISOR = 5

private const val MINOR_FROM_STEP = 10

/** Rundungsrest beim Vergleich mit der (animierten) Achse. */
internal const val AXIS_EPSILON = 1e-3f

/**
 * Maße der Kurve, die nur von Dichte und Schrift abhängen (in px). [layout] ergänzt Breite, Höhe
 * und Achse. Von oben: Beschriftung über einem Halten auf „voll“ (nur bei voller Höhe), Linie
 * „voll“, Kurve, Linie „leer“, darunter nur so viel wie der gewählte Griff auf „leer“ bzw. das Wort „leer“
 * braucht, dann die Zeitleiste mit Zahlen; das gelbe Band liegt beim Ziehen auf der Zeitleiste.
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
    private val spanMin: Float,
    private val handle: Float,
    private val handleSelected: Float,
    private val touch: Float,
    private val labelSpacing: Float,
    private val tickSpacing: Float,
) {
    /**
     * Platz unter „leer“ (und knapp über „voll“): die obere bzw. untere Hälfte des gewählten Griffs
     * auf der Linie bzw. das Wort an der Linie, dazu `GapTiny`. Die Tippfläche des Griffs darf in die
     * Zeitleiste reichen, der sichtbare Griff nicht.
     */
    private val zone = maxOf(handleSelected / 2f, hairline + smallHeight) + gapTiny

    /**
     * Über „voll“ bei voller Höhe: zusätzlich die Beschriftung eines Haltens auf „voll“, über den
     * Griffen an seinen Enden (so steht sie auch bei kurzem Halten frei).
     */
    private val topRoomy = maxOf(gapTiny + labelHeight + hairline * 2 + handleSelected / 2f, zone)

    /** Unter der Linie „leer“: [zone], Striche und Zahlen der Zeitleiste. */
    private val bottom = zone + gapSmall + hairline + smallHeight + gapTiny

    /** Volle Höhe: Abstand „voll“–„leer“ [Sizes.CurveSpan], Platz für Beschriftungen über einem Halten auf „voll“. */
    val preferredHeight: Float = topRoomy + span + bottom

    /** Kleinste Höhe: Abstand „voll“–„leer“ [Sizes.CurveSpanMin], über „voll“ nur [zone]. */
    val minHeight: Float = zone + spanMin + bottom

    /**
     * Lagen für die Breite [width], die Höhe [height] und die (ggf. gerade animierte) Achse
     * [axis]; [axisTarget] bestimmt den rechten Rand. Fehlt Höhe, weicht zuerst der Platz über dem
     * Halten auf „voll“, dann der Abstand „voll“–„leer“ (nie unter [Sizes.CurveSpanMin]).
     */
    fun layout(width: Float, axis: Float, axisTarget: Int = axis.roundToInt(), height: Float = preferredHeight): CurveLayout {
        val curveSpan = (height - zone - bottom).coerceIn(spanMin, span)
        val yFull = (height - curveSpan - bottom).coerceIn(zone, topRoomy)
        val yEmpty = yFull + curveSpan
        val ticksTop = yEmpty + zone
        val tickLabelTop = ticksTop + gapSmall + hairline
        // Das Band deckt Striche und Zahlen der Zeitleiste, mit Haarlinie Rand.
        val bandTop = ticksTop - hairline
        val bandHeight = tickLabelTop + smallHeight + hairline - bandTop
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
            handle = handle,
            handleSelected = handleSelected,
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
    val handle: Float,
    /** Durchmesser des gewählten Griffs. */
    val handleSelected: Float,
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
                spanMin = Sizes.CurveSpanMin.toPx(),
                handle = Sizes.CurveHandle.toPx(),
                handleSelected = Sizes.CurveHandleSelected.toPx(),
                touch = Dimens.MinTouchTarget.toPx(),
                labelSpacing = Sizes.CurveLabelMinSpacing.toPx(),
                tickSpacing = Sizes.CurveTickMinSpacing.toPx(),
            )
        }
    }
}

/** Zahlen, Phasen-Beschriftungen und Band einer Kurve mit 12 Phasen passen in den Cache. */
private const val MEASURE_CACHE = 48

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
internal fun FreePattern.pointAt(time: Float, layout: CurveLayout): Offset {
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

/** Ein Griff: Phase [index] und Mittelpunkt in px. */
internal data class CurveHandle(val index: Int, val center: Offset)

/**
 * Griffe der Kurve, einer am Ende jeder Phase, alle auf der Kurve – auch ein Halten (Nutzer
 * 2026-10-23: ein frei schwebender Halten-Griff las sich als verrutscht). Bei sehr kurzem Halten
 * berühren sich sein Griff und der davor; die Tippfläche entscheidet dann der nähere.
 */
internal fun curveHandles(pattern: FreePattern, layout: CurveLayout): List<CurveHandle> {
    val levels = pattern.levels()
    var end = 0
    return pattern.phases.mapIndexed { i, phase ->
        end += phase.seconds
        CurveHandle(i, Offset(layout.xAt(end.toFloat()), layout.yAt(levels[i].to)))
    }
}

/** Der Griff, dessen Tippfläche (Kreis mit [radius]) [position] trifft; bei Überlappung der nächste. */
internal fun nearestHandle(handles: List<CurveHandle>, position: Offset, radius: Float): CurveHandle? = handles
    .map { it to (it.center - position).getDistance() }
    .filter { it.second <= radius }
    .minByOrNull { it.second }
    ?.first

/**
 * Die Phase, deren Linie [position] höchstens [radius] px nahe kommt (Abstand zur Strecke vom
 * Beginn zum Ende der Phase); bei mehreren die nächste, sonst null.
 */
internal fun nearestSegment(pattern: FreePattern, layout: CurveLayout, position: Offset, radius: Float): Int? {
    val levels = pattern.levels()
    var start = 0
    return pattern.phases.mapIndexed { i, phase ->
        val from = Offset(layout.xAt(start.toFloat()), layout.yAt(levels[i].from))
        start += phase.seconds
        val to = Offset(layout.xAt(start.toFloat()), layout.yAt(levels[i].to))
        i to distanceToSegment(position, from, to)
    }
        .filter { it.second <= radius }
        .minByOrNull { it.second }
        ?.first
}

/** Abstand von [p] zur Strecke [a]–[b] in px. */
private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val lengthSquared = ab.x * ab.x + ab.y * ab.y
    val t = if (lengthSquared == 0f) 0f else (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / lengthSquared).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}

/**
 * Wo die Beschriftung „4 s“ jeder Phase steht ([sizes] je Phase in px), `null` = weggelassen.
 * Ein/Aus stehen bevorzugt außen neben der Schräge (Ein links oben, Aus rechts oben), sonst innen
 * auf der anderen Seite, notfalls über bzw. unter dem Griff am Ende oder rechts neben ihm; ein Halten mittig
 * dicht über (obere Hälfte) bzw. unter seiner Linie, wenn es zwischen die Griffe an seinen Enden
 * passt, sonst mit Abstand über bzw. unter den Griffen, sonst auf der anderen Seite seiner Linie,
 * notfalls links neben dem Griff am Ende. Eine Beschriftung darf keinen Griff, keine andere Beschriftung, nicht die Wörter
 * „voll“/„leer“ und nicht deren Linien berühren (alles mit Rand), nicht die Zeitleiste darunter und
 * nicht über den Innenrand ([CurveLayout.gapSmall]) hinaus ragen – passt keine Stelle (viele sehr
 * kurze Phasen), entfällt sie, statt zu überlappen.
 * Die gewählte Phase kommt zuerst dran. Die Werte stehen ohnehin in der Ablauf-Liste und in der
 * Beschreibung für TalkBack.
 */
internal fun curveLabelSpots(pattern: FreePattern, selected: Int?, layout: CurveLayout, width: Float, sizes: List<Size>): List<Rect?> {
    val levels = pattern.levels()
    val handles = curveHandles(pattern, layout)
    val margin = layout.hairline * 2
    val blocked = handles.map { h ->
        val r = (if (h.index == selected) layout.handleSelected else layout.handle) / 2f + margin
        Rect(h.center, r)
    }.toMutableList()
    // Die Wörter „voll“ und „leer“ an den Linien bleiben frei, mit Abstand: Ein „2 s“ direkt
    // dahinter läse sich als „leer 2 s“.
    val full = layout.measurer.measure(layout.fullLabel, layout.smallStyle).size
    val empty = layout.measurer.measure(layout.emptyLabel, layout.smallStyle).size
    blocked += Rect(Offset(layout.gapSmall, layout.yFull - layout.hairline - full.height), Size(full.width.toFloat(), full.height.toFloat())).inflate(layout.gapSmall)
    blocked += Rect(Offset(layout.gapSmall, layout.yEmpty + layout.hairline), Size(empty.width.toFloat(), empty.height.toFloat())).inflate(layout.gapSmall)
    // Die Linien selbst: Keine Beschriftung liegt auf „voll“ oder „leer“.
    val lineBand = layout.hairline + margin
    listOf(layout.yFull, layout.yEmpty).forEach { y -> blocked += Rect(0f, y - lineBand, width, y + lineBand) }
    // Innenrand wie Linien und Wörter: nichts reicht bis an die Kartenkante oder in die Zeitleiste.
    val area = Rect(layout.gapSmall, 0f, width - layout.gapSmall, layout.bandTop)
    val spots = arrayOfNulls<Rect>(pattern.phases.size)
    val order = pattern.phases.indices.sortedBy { it != selected }
    val gap = layout.gapSmall
    val clear = layout.handleSelected / 2f + margin
    for (i in order) {
        val (w, h) = sizes[i]
        val start = pattern.startOf(i)
        val from = Offset(layout.xAt(start.toFloat()), layout.yAt(levels[i].from))
        val to = Offset(layout.xAt((start + pattern.phases[i].seconds).toFloat()), layout.yAt(levels[i].to))
        val midX = (from.x + to.x) / 2f
        val midY = (from.y + to.y) / 2f
        val candidates = when (pattern.phases[i].type) {
            // Notfalls über bzw. unter dem Griff am Ende der Schräge.
            FreePhaseType.IN -> listOf(
                Offset(midX - gap - w, midY - h),
                Offset(midX + gap, midY),
                Offset(to.x - w / 2f, to.y - clear - h),
                // Knapp über „voll“: rechts neben dem Griff am Ende.
                Offset(to.x + clear, to.y - lineBand - h),
            )

            FreePhaseType.OUT -> listOf(
                Offset(midX + gap, midY - h),
                Offset(midX - gap - w, midY),
                Offset(to.x - w / 2f, to.y + clear),
                // Unter „leer“ ist nur Platz für den Griff: rechts neben dem Griff am Ende.
                Offset(to.x + clear, to.y + lineBand),
            )

            FreePhaseType.HOLD -> {
                // Obere Seite ([FreePattern.isUpperSide]): Beschriftung über der Linie.
                val upper = pattern.isUpperSide(i)
                // Dicht an der Linie, zwischen den Griffen an den Enden; unter „leer“ ist nur dafür
                // Platz, über „voll“ bei knapper Höhe ebenso.
                val close = if (upper) from.y - lineBand - h else from.y + lineBand
                // Mit Abstand über bzw. unter den Griffen: frei auch bei kurzem Halten.
                val outer = if (upper) from.y - clear - h else from.y + clear
                val inner = if (upper) from.y + gap else from.y - gap - h
                // Seitlich: links neben dem Griff am Ende.
                val beside = to.x - layout.handle / 2f - margin - gap - w
                listOf(
                    Offset(midX - w / 2f, close),
                    Offset(midX - w / 2f, outer),
                    Offset(midX - w / 2f, inner),
                    Offset(beside, close),
                    Offset(beside, outer),
                    Offset(beside, inner),
                )
            }
        }
        val spot = candidates
            .map { Rect(it, Size(w, h)) }
            .firstOrNull { r -> r.left >= area.left && r.right <= area.right && r.top >= area.top && r.bottom <= area.bottom && blocked.none { it.overlaps(r) } }
        if (spot != null) {
            spots[i] = spot
            // Mit Rand: Zwei Beschriftungen stoßen nie Kante an Kante.
            blocked += spot.inflate(margin)
        }
    }
    return spots.toList()
}
