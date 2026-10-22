package app.atemkraft.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.rangeFor
import app.atemkraft.domain.secondsOf
import app.atemkraft.domain.with
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Atemkurve des Muster-Editors (MUSTER-10): Ziehen eines Griffs rastet auf ganze Sekunden, meldet
 * den Wert über onChange und klemmt an `rangeFor`; je Raste ein Tick. „Sanft aktivierend“ hat keinen
 * Halten-leer-Griff. Für TalkBack ist die Kurve ein Knoten mit Beschreibung, ohne liveRegion.
 */
@RunWith(RobolectricTestRunner::class)
class BreathCurveEditorTest {

    @get:Rule
    val compose = createComposeRule()

    private val calm = PatternSpec(inhale = 4.0, holdFull = 1.0, exhale = 6.0, holdEmpty = null, activating = false)
    private val active = PatternSpec(inhale = 4.0, holdFull = 1.0, exhale = 3.0, holdEmpty = null, activating = true)

    private val calls = mutableListOf<Pair<PatternField, Int>>()
    private var ticks = 0
    private var spec by mutableStateOf(calm)
    private lateinit var metrics: CurveMetrics

    private val haptics = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            if (hapticFeedbackType == HapticFeedbackType.SegmentTick) ticks++
        }
    }

    private fun show(initial: PatternSpec) {
        spec = initial
        compose.setContent {
            AtemkraftTheme {
                metrics = rememberCurveMetrics()
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    BreathCurveEditor(
                        spec = spec,
                        onChange = { field, value ->
                            calls += field to value
                            spec = spec.with(field, value)
                        },
                    )
                }
            }
        }
    }

    private fun curve(text: String) = compose.onNodeWithContentDescription(text)

    /** Zieht den Griff von [field] in kleinen Schritten um [seconds] Sekunden (negativ = nach links). */
    private fun dragHandle(description: String, field: PatternField, seconds: Int) {
        val node = curve(description)
        val width = node.fetchSemanticsNode().size.width.toFloat()
        val layout = metrics.layout(width)
        val handle = curveHandles(spec, layout).single { it.field == field }
        node.performTouchInput {
            down(handle.center)
            val step = layout.pxPerSecond / 2f * if (seconds < 0) -1 else 1
            repeat(kotlin.math.abs(seconds) * 2) { moveBy(Offset(step, 0f)) }
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun `Ziehen meldet ganze Sekunden und klemmt an rangeFor`() {
        show(calm)
        val range = calm.rangeFor(PatternField.INHALE)!!
        assertEquals(2..6, range)

        dragHandle(CALM_DESCRIPTION, PatternField.INHALE, seconds = 5)
        assertEquals(listOf(PatternField.INHALE to 5, PatternField.INHALE to 6), calls)
        assertEquals(6, spec.secondsOf(PatternField.INHALE))
        assertEquals("ein Tick je Raste", 2, ticks)

        calls.clear()
        val description = "Atemkurve: Einatmen 6 Sekunden, Halten voll 1 Sekunde, Ausatmen 6 Sekunden"
        dragHandle(description, PatternField.INHALE, seconds = -6)
        assertEquals(spec.rangeFor(PatternField.INHALE)!!.first, spec.secondsOf(PatternField.INHALE))
        assertTrue("nie außerhalb von rangeFor", calls.all { (_, v) -> v in range })
        assertEquals(2, spec.secondsOf(PatternField.INHALE))
    }

    @Test
    fun `Halten bei 0 s bleibt greifbar und wird nie ungueltig`() {
        show(calm.with(PatternField.HOLD_FULL, 0))
        dragHandle("Atemkurve: Einatmen 4 Sekunden, Ausatmen 6 Sekunden", PatternField.HOLD_FULL, seconds = 9)
        val max = calm.with(PatternField.HOLD_FULL, 0).rangeFor(PatternField.HOLD_FULL)!!.last
        assertEquals(max, spec.secondsOf(PatternField.HOLD_FULL))
        assertTrue(calls.all { (field, _) -> field == PatternField.HOLD_FULL })
    }

    @Test
    fun `sanft aktivierend hat keinen Griff fuer Halten leer`() {
        show(active)
        val node = curve("Atemkurve: Einatmen 4 Sekunden, Halten voll 1 Sekunde, Ausatmen 3 Sekunden")
        val layout = metrics.layout(node.fetchSemanticsNode().size.width.toFloat())
        val fields = curveHandles(active, layout).map { it.field }
        assertEquals(listOf(PatternField.INHALE, PatternField.HOLD_FULL, PatternField.EXHALE), fields)
        assertEquals(4, curveHandles(calm, layout).size)

        // Auch ein Griff dorthin, wo er bei „ruhig“ säße, ändert Halten leer nie.
        val where = Offset(layout.xAt(active.cycleSeconds().toFloat()) + layout.zeroShift, layout.yEmpty + layout.holdOffset)
        node.performTouchInput {
            down(where)
            repeat(4) { moveBy(Offset(layout.pxPerSecond / 2f, 0f)) }
            up()
        }
        compose.waitForIdle()
        assertFalse(calls.any { it.first == PatternField.HOLD_EMPTY })
    }

    @Test
    fun `ein Knoten mit Beschreibung, ohne liveRegion`() {
        show(calm)
        curve(CALM_DESCRIPTION)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .assertCountEquals(0)
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(CALM_DESCRIPTION)), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun `Vorschau-Knopf vorhanden, Griffe mit 48-dp-Tippflaeche`() {
        show(calm)
        compose.onNodeWithContentDescription("Vorschau abspielen").assertExists()
        assertTrue(Sizes.CurveHandle <= Dimens.MinTouchTarget)
        val layout = metrics.layout(curve(CALM_DESCRIPTION).fetchSemanticsNode().size.width.toFloat())
        val handle = curveHandles(calm, layout).first()
        val edge = handle.center - Offset(layout.touchRadius - 1f, 0f)
        assertEquals(handle, nearestHandle(curveHandles(calm, layout), edge, layout.touchRadius))
        assertEquals(compose.density.run { Dimens.MinTouchTarget.toPx() } / 2f, layout.touchRadius, 0.01f)
    }

    private companion object {
        const val CALM_DESCRIPTION = "Atemkurve: Einatmen 4 Sekunden, Halten voll 1 Sekunde, Ausatmen 6 Sekunden"
    }

    /**
     * Zeitleiste: Zahlen alle 2 s, solange sie nebeneinander passen; auf 360 dp bei Schrift 2,0
     * alle 4 s statt übereinander. Zwischen zwei gezeigten Zahlen bleibt immer 2 × GapSmall frei.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // echte Textmaße
    fun `Zeitleiste zeigt bei grosser Schrift weniger Zahlen statt sie zu ueberlagern`() {
        // Wie im ScreenshotTest über die echte Konfiguration (nichtlineare Skalierung).
        RuntimeEnvironment.setFontScale(TestMatrix.MAX_FONT_SCALE)
        show(calm)
        // Im Editor: Gerätebreite ohne den Seitenrand links und rechts.
        val width = with(compose.density) { (360.dp - Dimens.ScreenPadding * 2).toPx() }
        val layout = metrics.layout(width)
        assertEquals(4, layout.labelStep)
        assertLabelsApart(layout)
    }

    @Test
    @Config(qualifiers = TestMatrix.A54_QUALIFIERS)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `Zeitleiste zeigt auf dem A54 alle 2 s eine Zahl`() {
        show(calm)
        val width = curve(CALM_DESCRIPTION).fetchSemanticsNode().size.width.toFloat()
        val layout = metrics.layout(width)
        assertEquals(2, layout.labelStep)
        assertLabelsApart(layout)
    }

    private fun assertLabelsApart(layout: CurveLayout) {
        val shown = (0..CURVE_SECONDS step layout.labelStep).map { second ->
            val w = layout.tickLabels[second].size.width
            val x = layout.xAt(second.toFloat())
            x - w / 2f to x + w / 2f
        }
        val gap = with(compose.density) { Dimens.GapSmall.toPx() } * 2
        shown.zipWithNext().forEach { (a, b) -> assertTrue("Zahlen zu dicht: $a / $b", a.second + gap <= b.first) }
    }
}
