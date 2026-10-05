package app.atemkraft.ui.screenshots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SelectChip
import app.atemkraft.ui.components.SessionPrimaryButton
import app.atemkraft.ui.components.SessionSecondaryButton
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.Stepper
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.ControlSize
import app.atemkraft.ui.theme.controlScale
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Echte Tap-Injektion um jedes zentrale Bedienelement (A11Y-05): Die sichtbare Fläche folgt der
 * Schrift (Sizes), die Tippfläche bleibt 48 dp – ±23 dp vom Mittelpunkt trifft, ±25 dp nicht.
 * Sichert ab, was nur über Material-Interna (Hit-Test-Erweiterung, minimumInteractive) gilt.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h800dp-xhdpi")
class ControlTouchTest(private val fontScale: Float) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "fs{0}")
        fun params(): List<Array<Any>> = listOf(1.0f, 1.1f, 2.0f).map { arrayOf<Any>(it) }

        private const val INSIDE_DP = 23f
        private const val OUTSIDE_DP = 25f
    }

    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val compose = createComposeRule(effectContext = Frozen)

    private object Frozen : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R = awaitCancellation()
    }

    private var current by mutableStateOf<(@Composable () -> Unit)?>(null)
    private val hits = mutableListOf<String>()
    private val density get() = compose.density.density

    @Before
    fun setUp() {
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            AtemkraftTheme {
                Box(Modifier.requiredSize(360.dp, 400.dp), contentAlignment = Alignment.Center) { current?.invoke() }
            }
        }
    }

    private fun show(content: @Composable () -> Unit) {
        current = content
        compose.waitForIdle()
    }

    private fun clickables(): List<SemanticsNode> = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { it.layoutInfo.isPlaced }

    /** Tippt an [at] (Root-px) und liefert, wer reagiert hat. */
    private fun tap(at: Offset): List<String> {
        hits.clear()
        compose.onRoot().performTouchInput { click(at) }
        compose.waitForIdle()
        return hits.toList()
    }

    /**
     * Platz des Knotens im Layout (mit 48-dp-Tippfläche). `layoutInfo.coordinates` ist die innerste
     * Ebene (nach Polster und Größe); die Bedienelemente sind darin zentriert, deshalb liefert sie
     * den Mittelpunkt, die äußere Größe kommt aus `layoutInfo.width/height`.
     */
    private fun layoutBounds(node: SemanticsNode): Rect {
        val c = node.layoutInfo.coordinates.boundsInRoot().center
        val w = node.layoutInfo.width / 2f
        val h = node.layoutInfo.height / 2f
        return Rect(c.x - w, c.y - h, c.x + w, c.y + h)
    }

    /**
     * Prüft ein Element: ±23 dp trifft entlang beider Achsen; entlang der [tightAxes] (dort ist die
     * Optik kleiner als 48 dp) verfehlt ±25 dp.
     */
    private fun probe(name: String, node: SemanticsNode, expected: String, tightAxes: Set<Char>) {
        val c = layoutBounds(node).center
        for (axis in listOf('x', 'y')) {
            for (sign in listOf(-1f, 1f)) {
                fun at(d: Float) = if (axis == 'x') Offset(c.x + sign * d * density, c.y) else Offset(c.x, c.y + sign * d * density)
                assertEquals("$name fs$fontScale: ${sign * INSIDE_DP} dp auf $axis muss treffen", listOf(expected), tap(at(INSIDE_DP)))
                if (axis in tightAxes) {
                    assertTrue("$name fs$fontScale: ${sign * OUTSIDE_DP} dp auf $axis darf nicht treffen", expected !in tap(at(OUTSIDE_DP)))
                }
            }
        }
    }

    @Test
    fun `AppIconButton trifft genau in der 48-dp-Tippflaeche`() {
        show { AppIconButton(onClick = { hits += "icon" }) { Icon(Icons.Filled.Delete, contentDescription = "x") } }
        probe("AppIconButton", clickables().single(), "icon", setOf('x', 'y'))
    }

    @Test
    fun `Stepper-Knoepfe treffen genau in der 48-dp-Tippflaeche`() {
        show {
            Box(Modifier.width(340.dp)) {
                Stepper(label = "Dauer", value = 5, range = 1..10, onChange = { hits += if (it > 5) "plus" else "minus" })
            }
        }
        val (minus, plus) = clickables().sortedBy { layoutBounds(it).left }
        probe("Stepper −", minus, "minus", setOf('x', 'y'))
        probe("Stepper +", plus, "plus", setOf('x', 'y'))
    }

    @Test
    fun `Chip, Text-Button, Segment und Reset sind 48 dp hoch antippbar`() {
        show { SelectChip(label = "10 min", selected = false, onClick = { hits += "chip" }) }
        probe("SelectChip", clickables().single(), "chip", setOf('y'))

        show { AppTextButton(onClick = { hits += "text" }) { Text("Laden") } }
        probe("AppTextButton", clickables().single(), "text", setOf('y'))

        show { Box(Modifier.width(340.dp)) { SegmentedChoiceRow(listOf(1, 2, 3), 1, { hits += "seg$it" }) { "Stufe $it" } } }
        val middle = clickables().sortedBy { layoutBounds(it).left }[1]
        probe("Segment", middle, "seg2", setOf('y'))

        show { Box(Modifier.width(340.dp)) { StartSplitButton("Starten", resetVisible = true, onStart = { hits += "start" }, onReset = { hits += "reset" }) } }
        val reset = clickables().maxBy { layoutBounds(it).left }
        probe("Reset", reset, "reset", setOf('y'))
    }

    @Test
    fun `Schalterzeile schaltet ueber die ganze Zeile`() {
        show {
            var on by remember { mutableStateOf(false) }
            Box(Modifier.width(340.dp)) {
                ToggleRow(label = "Vibration", checked = on, onCheckedChange = {
                    on = it
                    hits += "row"
                })
            }
        }
        val row = compose
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .single()
        assertTrue("Zeile ≥ 48 dp hoch", layoutBounds(row).height >= 48 * density - 0.5f)
        probe("ToggleRow", row, "row", emptySet())
        // Ganz links auf dem Label schaltet die Zeile ebenso.
        val b = layoutBounds(row)
        assertEquals(listOf("row"), tap(Offset(b.left + 4 * density, b.center.y)))
    }

    @Test
    fun `Stepper und Schalterzeile melden Name, Rolle, Zustand und Wert`() {
        // Was TalkBack liest (zusammengeführter Baum): Knöpfe mit Name, Rolle, Klick und Disabled
        // an der Grenze; der Wert als Live-Region, die nach dem Klick den neuen Wert trägt.
        val app = RuntimeEnvironment.getApplication()
        val minusName = "Dauer, ${app.getString(R.string.adjust_decrease)}"
        val plusName = "Dauer, ${app.getString(R.string.adjust_increase)}"
        var value by mutableIntStateOf(1)
        show { Box(Modifier.width(340.dp)) { Stepper(label = "Dauer", value = value, range = 1..10, onChange = { value = it }) } }
        compose.onNodeWithContentDescription(minusName)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
        compose.onNodeWithContentDescription(plusName)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription("Dauer: 2")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithContentDescription(minusName).assertIsEnabled()

        var on by mutableStateOf(false)
        show { Box(Modifier.width(340.dp)) { ToggleRow(label = "Vibration", checked = on, onCheckedChange = { on = it }, hint = "Hinweis") } }
        val row = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        row.assertIsOff().assert(hasText("Vibration")).assert(hasText("Hinweis"))
        row.performSemanticsAction(SemanticsActions.OnClick)
        row.assertIsOn()

        show { Box(Modifier.width(340.dp)) { ToggleRow(label = "Sprachanleitung", checked = false, onCheckedChange = {}, enabled = false) } }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsNotEnabled()
    }

    @Test
    fun `benachbarte Icon-Buttons ohne Abstand trennen an der Naht`() {
        show {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                AppIconButton(onClick = { hits += "links" }) { Icon(Icons.Filled.PlayArrow, contentDescription = "l") }
                AppIconButton(onClick = { hits += "rechts" }) { Icon(Icons.Filled.Delete, contentDescription = "r") }
            }
        }
        val (left, right) = clickables().sortedBy { layoutBounds(it).left }
        val seam = layoutBounds(left).right
        val y = layoutBounds(left).center.y
        assertEquals(listOf("links"), tap(Offset(seam - 2 * density, y)))
        assertEquals(listOf("rechts"), tap(Offset(seam + 2 * density, y)))
    }

    @Test
    fun `Start und Reset sind gleich hoch und nicht auf 48 dp aufgeblaeht`() {
        show { Box(Modifier.width(340.dp)) { StartSplitButton("Starten", resetVisible = true, onStart = {}, onReset = {}) } }
        val (start, reset) = clickables().sortedBy { layoutBounds(it).left }
        val hStart = visibleHeight(start)
        val hReset = visibleHeight(reset)
        assertTrue("Start $hStart px ≠ Reset $hReset px (fs$fontScale)", abs(hStart - hReset) <= 1f)
        assertMinHeight("Start", hStart)
    }

    @Test
    fun `Session-Buttons in einer Reihe sind sichtbar nur so hoch wie Buttons`() {
        show {
            Box(Modifier.width(340.dp)) {
                AdaptiveButtonRow {
                    SessionPrimaryButton(text = "Pause", onClick = {})
                    SessionSecondaryButton(text = "Von vorne", onClick = {})
                }
            }
        }
        val (a, b) = clickables().sortedBy { layoutBounds(it).left }
        assertTrue(abs(visibleHeight(a) - visibleHeight(b)) <= 1f)
        assertMinHeight("Session-Button", visibleHeight(a))
    }

    @Test
    fun `Pruefung erkennt klickbare Flaechen ohne Klick-Semantik und zu kleine Tippflaechen`() {
        // Selbsttest für ControlFindings: bricht ein Compose-Update die Erkennung über die
        // Modifier-Klassen, wird dieser Test rot statt die Screenshot-Prüfung stumm.
        show {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(48.dp).clearAndSetSemantics { contentDescription = "stumm" }.clickable(role = Role.Button) {})
                Box(Modifier.size(40.dp).clickable(role = Role.Button) {})
            }
        }
        val nodes = compose.onAllNodes(SemanticsMatcher("alle") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        val frame = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val found = ControlFindings.findings(nodes, frame, density, "selbsttest").map { it.substringBefore('\t') }
        assertTrue("OHNE_KLICK nicht erkannt: $found", "OHNE_KLICK" in found)
        assertTrue("TIPPFLAECHE nicht erkannt: $found", "TIPPFLAECHE" in found)
    }

    /** Höhe der sichtbaren Fläche: der Klick-Knoten, kleiner als seine 48-dp-Reservierung. */
    private fun visibleHeight(node: SemanticsNode): Float {
        // Bei M3-Buttons sitzt die Klick-Semantik innen (nach minimumInteractive): size = Optik.
        val text = node.config.getOrNull(SemanticsProperties.Text)
        return node.size.height.toFloat().also { check(it > 0f) { "Knoten ohne Größe: $text" } }
    }

    /** Sichtbar genau die skalierte Mindesthöhe, solange die Beschriftung hineinpasst (fs ≤ 1,3). */
    private fun assertMinHeight(name: String, heightPx: Float) {
        val expected = ControlSize.BUTTON_HEIGHT.at(controlScale(fontScale)).value * density
        if (fontScale <= 1.3f) {
            assertEquals("$name fs$fontScale sichtbar", expected, heightPx, 1f)
        } else {
            assertTrue("$name fs$fontScale wächst mit der Schrift: $heightPx < $expected", heightPx >= expected - 1f)
        }
    }
}
