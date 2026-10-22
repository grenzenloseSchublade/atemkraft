package app.atemkraft.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Atemkurve des Muster-Editors (MUSTER-10, „Frei gestalten“): Ziehen eines Griffs ändert genau
 * die Phase links vom Griff, rastet auf ganze Sekunden und klemmt auf 1–30 s, je Raste ein Tick;
 * während des Ziehens steht das gelbe Band, die Achse rechnet erst nach dem Loslassen neu. Die
 * Zeitleiste wählt ihren Schritt nach der Länge. Für TalkBack ist die Kurve ein Knoten mit
 * Beschreibung der Folge, ohne liveRegion.
 */
@RunWith(RobolectricTestRunner::class)
class BreathCurveEditorTest {

    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<Pair<Int, Int>>()
    private val selections = mutableListOf<Int?>()
    private var ticks = 0
    private var pattern by mutableStateOf(BOX)
    private var selected by mutableStateOf<Int?>(null)
    private lateinit var metrics: CurveMetrics

    private val haptics = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            if (hapticFeedbackType == HapticFeedbackType.SegmentTick) ticks++
        }
    }

    private fun show(initial: FreePattern, modifier: Modifier = Modifier) {
        pattern = initial
        compose.setContent {
            AtemkraftTheme {
                metrics = rememberCurveMetrics()
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    BreathCurveEditor(
                        pattern = pattern,
                        selected = selected,
                        onSelect = {
                            selections += it
                            selected = it
                        },
                        onChangeSeconds = { index, value ->
                            calls += index to value
                            pattern = pattern.withSeconds(index, value)
                        },
                        modifier = modifier,
                    )
                }
            }
        }
    }

    private fun curve(): SemanticsNodeInteraction = compose.onNode(SemanticsMatcher.keyIsDefined(CurveAxisSeconds))

    private fun layoutNow(): CurveLayout {
        val node = curve().fetchSemanticsNode()
        val axis = node.config[CurveAxisSeconds]
        return metrics.layout(node.size.width.toFloat(), axis.toFloat(), axis)
    }

    /** Zieht den Griff von Phase [index] in halben Sekunden um [seconds] (negativ = nach links); [release] = loslassen. */
    private fun dragHandle(index: Int, seconds: Int, release: Boolean = true) {
        val layout = layoutNow()
        val handle = curveHandles(pattern, layout).single { it.index == index }
        curve().performTouchInput {
            down(handle.center)
            val step = layout.pxPerSecond / 2f * if (seconds < 0) -1 else 1
            repeat(kotlin.math.abs(seconds) * 2) { moveBy(Offset(step, 0f)) }
            if (release) up()
        }
        compose.waitForIdle()
    }

    private fun releaseDrag() {
        curve().performTouchInput { up() }
        compose.waitForIdle()
    }

    @Test
    fun `Ziehen aendert genau die Phase am Griff, waehlt sie aus, ein Tick je Raste`() {
        show(BOX)
        dragHandle(index = 2, seconds = 3)
        assertEquals(listOf(2 to 5, 2 to 6, 2 to 7), calls)
        assertEquals(listOf(4, 4, 7, 4), pattern.phases.map { it.seconds })
        assertEquals("ein Tick je Raste", 3, ticks)
        assertEquals(listOf<Int?>(2), selections)
    }

    @Test
    fun `Ziehen klemmt auf 1 bis 30 s`() {
        show(BOX)
        dragHandle(index = 0, seconds = -8)
        assertEquals(FreePattern.MIN_SECONDS, pattern.phases[0].seconds)
        assertTrue(calls.all { (i, v) -> i == 0 && v in FreePattern.SECONDS_RANGE })

        // Nach rechts: Die Achse steht während des Zugs, der Finger kommt bis zum Rand – auch
        // mehrfach gezogen nie über 30 s.
        calls.clear()
        repeat(4) { dragHandle(index = 1, seconds = 40) }
        assertEquals(FreePattern.MAX_SECONDS, pattern.phases[1].seconds)
        assertTrue(calls.all { (i, v) -> i == 1 && v in FreePattern.SECONDS_RANGE })
    }

    @Test
    fun `Halten-Griffe ueber bzw unter der Linie, Ein und Aus auf der Kurve`() {
        show(BOX)
        val layout = layoutNow()
        val handles = curveHandles(BOX, layout)
        assertEquals(layout.yFull, handles[0].center.y, 0.01f)
        assertEquals(layout.yFull - layout.holdOffset, handles[1].center.y, 0.01f)
        assertEquals(layout.yEmpty, handles[2].center.y, 0.01f)
        assertEquals(layout.yEmpty + layout.holdOffset, handles[3].center.y, 0.01f)
        assertEquals(layout.xAt(8f), handles[1].center.x, 0.01f)
    }

    @Test
    fun `Band nur waehrend des Ziehens, Achse erst nach dem Loslassen neu`() {
        show(BOX)
        curve().assert(SemanticsMatcher.keyNotDefined(CurveBandLabel))
        curve().assert(SemanticsMatcher.keyNotDefined(CurveDragPhase))
        assertEquals(22, BOX.axisSeconds())
        curve().assert(SemanticsMatcher.expectValue(CurveAxisSeconds, 22))

        dragHandle(index = 1, seconds = 3, release = false)
        assertEquals(7, pattern.phases[1].seconds)
        curve().assert(SemanticsMatcher.expectValue(CurveDragPhase, 1))
        curve().assert(SemanticsMatcher.expectValue(CurveBandLabel, "1 bis 30 s, Ende rechts außerhalb"))
        // Gesamt 19 s → Achse wäre 26 s, steht aber noch auf 22 s.
        curve().assert(SemanticsMatcher.expectValue(CurveAxisSeconds, 22))

        releaseDrag()
        curve().assert(SemanticsMatcher.keyNotDefined(CurveBandLabel))
        curve().assert(SemanticsMatcher.keyNotDefined(CurveDragPhase))
        curve().assert(SemanticsMatcher.expectValue(CurveAxisSeconds, 26))
    }

    @Test
    fun `Senkrechtes Wischen auf einem Griff aendert nichts, ein Tipp waehlt ihn`() {
        show(BOX)
        val layout = layoutNow()
        val handle = curveHandles(BOX, layout).single { it.index == 2 }
        curve().performTouchInput {
            down(handle.center)
            // Leicht schräg nach oben, wie beim Scrollen.
            repeat(6) { moveBy(Offset(layout.pxPerSecond / 16f, -layout.touchRadius / 2f)) }
            up()
        }
        compose.waitForIdle()
        assertTrue(calls.isEmpty())
        assertTrue(selections.isEmpty())
        assertEquals(0, ticks)

        curve().performTouchInput { click(handle.center) }
        compose.waitForIdle()
        assertTrue(calls.isEmpty())
        assertEquals(listOf<Int?>(2), selections)
    }

    @Test
    fun `Tipp neben die Griffe waehlt nichts und aendert nichts`() {
        show(BOX)
        val layout = layoutNow()
        curve().performTouchInput {
            down(Offset(layout.xAt(2f), layout.yAt(0.5f) + layout.touchRadius * 2))
            moveBy(Offset(layout.pxPerSecond * 3, 0f))
            up()
        }
        compose.waitForIdle()
        assertTrue(calls.isEmpty())
        assertTrue(selections.isEmpty())
    }

    @Test
    fun `Griffe mit 48-dp-Tippflaeche, bei Ueberlappung gewinnt der naechste`() {
        show(BOX)
        assertTrue(Sizes.CurveHandleSelected <= Dimens.MinTouchTarget)
        val layout = layoutNow()
        assertEquals(compose.density.run { Dimens.MinTouchTarget.toPx() } / 2f, layout.touchRadius, 0.01f)
        val handles = curveHandles(BOX, layout)
        val edge = handles.first().center - Offset(layout.touchRadius - 1f, 0f)
        assertEquals(handles.first(), nearestHandle(handles, edge, layout.touchRadius))
        // Zwei Griffe dicht beieinander: Wer näher liegt, gewinnt.
        val a = CurveHandle(0, Offset(100f, 100f))
        val b = CurveHandle(1, Offset(110f, 100f))
        assertEquals(b, nearestHandle(listOf(a, b), Offset(106f, 100f), layout.touchRadius))
        assertEquals(a, nearestHandle(listOf(a, b), Offset(104f, 100f), layout.touchRadius))
        assertNull(nearestHandle(listOf(a, b), Offset(100f, 100f + layout.touchRadius * 2), layout.touchRadius))
    }

    @Test
    fun `Stufen - Viloma steigt in drei Stufen, Halten dazwischen auf halber Hoehe darunter`() {
        val viloma = FreePattern(
            listOf(
                FreePhase(FreePhaseType.IN, 2),
                FreePhase(FreePhaseType.HOLD, 2),
                FreePhase(FreePhaseType.IN, 2),
                FreePhase(FreePhaseType.HOLD, 2),
                FreePhase(FreePhaseType.IN, 2),
                FreePhase(FreePhaseType.HOLD, 2),
                FreePhase(FreePhaseType.OUT, 6),
            ),
        )
        show(viloma)
        val layout = layoutNow()
        val handles = curveHandles(viloma, layout)
        assertEquals(layout.yAt(1f / 3f), handles[0].center.y, 0.5f)
        // Halten bei einem Drittel: unter der Linie.
        assertEquals(layout.yAt(1f / 3f) + layout.holdOffset, handles[1].center.y, 0.5f)
        // Halten bei zwei Dritteln: über der Linie.
        assertEquals(layout.yAt(2f / 3f) - layout.holdOffset, handles[3].center.y, 0.5f)
        assertEquals(layout.yFull - layout.holdOffset, handles[5].center.y, 0.5f)
    }

    @Test
    fun `Skalenschritt je Laenge - Zahlen mindestens 30 dp auseinander, feine Striche ab 6 dp`() {
        val dp = compose.density.density
        val spacing = 30 * dp
        val fine = 6 * dp
        // Feste Breite 320 dp Zeichenfläche: 12 s → 1 s je ~26 dp → Schritt 2.
        assertEquals(1, curveLabelStep(pxPerSecond = 30 * dp, minSpacing = spacing))
        assertEquals(2, curveLabelStep(pxPerSecond = 26 * dp, minSpacing = spacing))
        assertEquals(5, curveLabelStep(pxPerSecond = 8 * dp, minSpacing = spacing))
        assertEquals(10, curveLabelStep(pxPerSecond = 4 * dp, minSpacing = spacing))
        assertEquals(15, curveLabelStep(pxPerSecond = 2.5f * dp, minSpacing = spacing))
        assertEquals(30, curveLabelStep(pxPerSecond = 1.5f * dp, minSpacing = spacing))
        assertEquals(60, curveLabelStep(pxPerSecond = 0.6f * dp, minSpacing = spacing))
        assertEquals(60, curveLabelStep(pxPerSecond = 0.1f * dp, minSpacing = spacing))

        assertEquals(1, curveMinorStep(labelStep = 5, pxPerSecond = 6 * dp, fineMin = fine))
        assertEquals(5, curveMinorStep(labelStep = 5, pxPerSecond = 5 * dp, fineMin = fine))
        assertEquals(2, curveMinorStep(labelStep = 10, pxPerSecond = 4 * dp, fineMin = fine))
        assertEquals(6, curveMinorStep(labelStep = 30, pxPerSecond = 1.5f * dp, fineMin = fine))
    }

    @Test
    @Config(qualifiers = TestMatrix.A54_QUALIFIERS)
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // echte Textmaße
    fun `Zeitleiste auf dem A54 - kurze Folge alle 2 s, lange in groesseren Schritten, nie zu dicht`() {
        show(BOX)
        val width = curve().fetchSemanticsNode().size.width.toFloat()
        val short = metrics.layout(width, 12f)
        assertEquals(2, short.labelStep)
        assertEquals(1, short.minorStep)
        assertLabelsApart(short)
        // 4·1·6 (11 s) → Achse 18 s: noch alle 2 s.
        assertEquals(2, metrics.layout(width, 18f).labelStep)
        assertLabelsApart(metrics.layout(width, 18f))

        val long = metrics.layout(width, 366f)
        assertTrue("Schritt ${long.labelStep}", long.labelStep >= 30)
        assertTrue(long.minorStep > 1)
        assertLabelsApart(long)
    }

    /** 360 dp bei Schrift 2,0: Die Zahlen stehen weiter auseinander statt übereinander. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `Zeitleiste zeigt bei grosser Schrift weniger Zahlen statt sie zu ueberlagern`() {
        RuntimeEnvironment.setFontScale(TestMatrix.MAX_FONT_SCALE)
        show(BOX)
        val width = with(compose.density) { (360.dp - Dimens.ScreenPadding * 2).toPx() }
        listOf(12f, 22f, 60f, 180f).forEach { assertLabelsApart(metrics.layout(width, it)) }
    }

    @Test
    fun `ein Knoten mit Beschreibung der Folge, ohne liveRegion`() {
        show(BOX)
        val description = "Atemkurve: Einatmen 4 Sekunden, Halten voll 4 Sekunden, Ausatmen 4 Sekunden, Halten leer 4 Sekunden"
        compose.onNodeWithContentDescription(description)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .assertCountEquals(0)
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)), useUnmergedTree = true)
            .assertCountEquals(1)

        pattern = pattern.withSeconds(1, 1)
        compose.onNodeWithContentDescription(
            "Atemkurve: Einatmen 4 Sekunden, Halten voll 1 Sekunde, Ausatmen 4 Sekunden, Halten leer 4 Sekunden",
        ).assertExists()
    }

    @Test
    fun `Tipp auf eine Linie waehlt ihre Phase, daneben und beim Wischen nichts`() {
        show(BOX)
        val layout = layoutNow()
        // Mitte der Ausatmen-Linie (8 bis 12 s, voll → leer), fern aller Griffe.
        val middle = Offset(layout.xAt(10f), layout.yAt(0.5f))
        assertNull(nearestHandle(curveHandles(BOX, layout), middle, layout.touchRadius))
        curve().performTouchInput { click(middle) }
        compose.waitForIdle()
        assertEquals(listOf<Int?>(2), selections)
        assertTrue(calls.isEmpty())

        // Mehr als eine halbe Tippfläche über der Linie: keine Auswahl.
        selections.clear()
        curve().performTouchInput { click(middle - Offset(0f, layout.touchRadius * 1.5f)) }
        // Auf der Linie, aber gewischt (Scrollen): keine Auswahl, keine Änderung.
        curve().performTouchInput {
            down(middle)
            repeat(6) { moveBy(Offset(0f, -layout.touchRadius / 2f)) }
            up()
        }
        compose.waitForIdle()
        assertTrue(selections.isEmpty())
        assertTrue(calls.isEmpty())
        assertEquals(0, ticks)
    }

    @Test
    fun `Vorschau-Knoepfe stehen ausserhalb der Kurve und schalten die Vorschau`() {
        val preview = CurvePreviewState()
        compose.setContent { AtemkraftTheme { CurvePreviewControls(preview) } }
        compose.onNodeWithContentDescription("Vorschau abspielen").performClick()
        assertTrue(preview.playing)
        compose.onNodeWithContentDescription("Vorschau anhalten").performClick()
        assertFalse(preview.playing)
    }

    @Test
    fun `Vorschau-Zustand - Phase und Restdauer`() {
        val preview = CurvePreviewState()
        preview.play()
        preview.time = 5.5f
        assertEquals(1, preview.phaseIndexIn(BOX))
        assertEquals(2_500L, preview.remainingMillisIn(BOX))
        preview.time = 15.9f
        assertEquals(3, preview.phaseIndexIn(BOX))
        preview.pause()
        assertFalse(preview.playing)
        preview.toggle()
        assertTrue(preview.playing)
        assertEquals("Start am Anfang", 0f, preview.time, 0f)
    }

    @Test
    fun `Phasen-Beschriftungen ueberlappen weder Griffe noch einander, kurze Folgen tragen alle`() {
        show(BOX, Modifier.width(TestMatrix.COMPACT.widthDp.dp))
        listOf(
            BOX to 1,
            FreePatternTemplates.all.single { it.label == "Viloma I" }.pattern to null,
            // Zwölf kurze Phasen: Was keinen Platz hat, entfällt.
            FreePattern(List(6) { listOf(FreePhase(FreePhaseType.IN, 1), FreePhase(FreePhaseType.OUT, 1)) }.flatten()) to 3,
        ).forEach { (p, sel) ->
            pattern = p
            selected = sel
            compose.waitForIdle()
            val layout = layoutNow()
            val width = curve().fetchSemanticsNode().size.width.toFloat()
            val sizes = p.phases.map { phase ->
                val size = layout.measurer.measure("${phase.seconds}${layout.secondsSuffix}", layout.phaseStyle).size
                Size(size.width.toFloat(), size.height.toFloat())
            }
            val spots = curveLabelSpots(p, sel, layout, width, sizes)
            val placed = spots.filterNotNull()
            placed.forEachIndexed { i, a ->
                placed.drop(i + 1).forEach { b -> assertFalse("Beschriftungen überlappen: $a / $b", a.overlaps(b)) }
                curveHandles(p, layout).forEach { h ->
                    assertFalse("Beschriftung $a über Griff ${h.index}", a.overlaps(Rect(h.center, layout.handle / 2f)))
                }
                assertTrue("in der Fläche über dem Band, mit Innenrand", a.left >= layout.gapSmall && a.right <= width - layout.gapSmall && a.top >= 0f && a.bottom <= layout.bandTop)
                // Nicht auf den Linien „voll“/„leer“ und mit Abstand zu ihren Wörtern.
                listOf(layout.yFull, layout.yEmpty).forEach { y -> assertFalse("Beschriftung $a auf einer Linie", a.top <= y + layout.hairline && a.bottom >= y - layout.hairline) }
                val fullWord = layout.measurer.measure(layout.fullLabel, layout.smallStyle).size
                val emptyWord = layout.measurer.measure(layout.emptyLabel, layout.smallStyle).size
                val words = listOf(
                    Rect(Offset(layout.gapSmall, layout.yFull - layout.hairline - fullWord.height), Size(fullWord.width.toFloat(), fullWord.height.toFloat())),
                    Rect(Offset(layout.gapSmall, layout.yEmpty + layout.hairline), Size(emptyWord.width.toFloat(), emptyWord.height.toFloat())),
                )
                words.forEach { word -> assertFalse("Beschriftung $a klebt an $word", a.overlaps(word.inflate(layout.gapTiny))) }
            }
            if (p == BOX) assertEquals("Box: alle vier", 4, placed.size)
            sel?.let { assertTrue("die gewählte Phase ist beschriftet", spots[it] != null) }
        }
    }

    private fun assertLabelsApart(layout: CurveLayout) {
        val seconds = layout.labelSeconds()
        val shown = seconds.map { s ->
            val text = if (s == seconds.last()) "$s${layout.secondsSuffix}" else s.toString()
            val w = layout.measurer.measure(text, layout.smallStyle).size.width
            val x = layout.xAt(s.toFloat())
            x - w / 2f to x + w / 2f
        }
        val gap = with(compose.density) { Dimens.GapSmall.toPx() }
        val spacing = with(compose.density) { Sizes.CurveLabelMinSpacing.toPx() }
        shown.zipWithNext().forEach { (a, b) -> assertTrue("Zahlen zu dicht: $a / $b", a.second + gap <= b.first) }
        assertTrue(layout.labelStep * layout.pxPerSecond >= spacing)
    }

    private companion object {
        val BOX = FreePattern(
            listOf(
                FreePhase(FreePhaseType.IN, 4),
                FreePhase(FreePhaseType.HOLD, 4),
                FreePhase(FreePhaseType.OUT, 4),
                FreePhase(FreePhaseType.HOLD, 4),
            ),
        )
    }
}
