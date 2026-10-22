package app.atemkraft.ui.patterns

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.atemkraft.R
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.PatternWish
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * TalkBack im Muster-Editor (MUSTER-10, A11Y-04): Die Lesereihenfolge folgt der Seite – Wünsche,
 * Atemzug, „Genau einstellen“, Name, „Aus der Yoga-Lehre“, Start –, und jeder Stepper-Tipp löst
 * genau eine Ansage aus: den neuen Wert. Weder die Einordnung noch der Atemzug im Kopf noch das
 * Lesezeichen (beim Bearbeiten wird es durch die Änderung leer) melden sich dazu.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class PatternEditorA11yTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val prefix get() = app.getString(R.string.pattern_editor_title)

    private fun string(id: Int) = app.getString(id)

    private fun show(state: PatternEditorState): PatternEditorState {
        compose.setContent {
            AtemkraftTheme {
                PatternEditorScreen(state = state, onSavedChange = {}, onStart = { _, _ -> }, onOpenLore = {}, onBack = {})
            }
        }
        return state
    }

    /** Was TalkBack an einem Knoten liest: Beschreibung, Text, Zustand. */
    private fun SemanticsNode.spoken(): String = listOfNotNull(
        config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
        config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text },
        config.getOrNull(SemanticsProperties.EditableText)?.text,
        config.getOrNull(SemanticsProperties.StateDescription),
    ).joinToString(" | ")

    /**
     * Knoten in der Reihenfolge, in der TalkBack sie erreicht: Baum in Tiefe zuerst, Geschwister
     * nach `traversalIndex` (stabil, sonst Lage im Layout) – so wie Compose sie an Android meldet.
     */
    private fun traversal(): List<String> {
        fun visit(node: SemanticsNode): List<String> {
            val children = node.children.sortedBy { it.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f }
            return listOf(node.spoken()) + children.flatMap(::visit)
        }
        return visit(compose.onRoot().fetchSemanticsNode())
    }

    /** Inhalt jeder Live-Region, nach Knoten-Id: daran erkennt TalkBack eine Änderung. */
    private fun liveRegions(): Map<Int, String> = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .associate { it.id to it.spoken() }

    /** Tippt [button] an und liefert die Live-Regionen, deren Inhalt sich dadurch geändert hat (oder neu sind). */
    private fun announcementsAfterTapOn(button: String): List<String> {
        val node = compose.onNodeWithContentDescription(button)
        node.performScrollTo()
        compose.waitForIdle()
        val before = liveRegions()
        node.performClick()
        compose.waitForIdle()
        return liveRegions().filter { (id, spoken) -> before[id] != spoken }.values.toList()
    }

    @Test
    fun `Lesereihenfolge folgt der Seite – Wuensche, Atemzug, Genau einstellen, Name, Lehre, Start`() {
        show(PatternEditorState.new(prefix, PatternWish(activating = false, withHold = true)))
        val order = traversal()
        val milestones = listOf(
            string(R.string.pattern_editor_wishes),
            string(R.string.pattern_editor_character),
            string(R.string.pattern_editor_hold),
            "${string(R.string.cd_adjust_duration)}, ${string(R.string.adjust_increase)}",
            string(R.string.pattern_editor_breath),
            string(R.string.pattern_editor_fine),
            "${string(R.string.phase_inhale)}, ${string(R.string.adjust_increase)}",
            "${string(R.string.phase_exhale)}, ${string(R.string.adjust_increase)}",
            string(R.string.pattern_editor_name_label),
            string(R.string.detail_lore),
            string(R.string.action_start),
        )
        val positions = milestones.map { m -> order.indexOfFirst { it.contains(m) } }
        milestones.zip(positions).forEach { (m, at) -> assertTrue("„$m“ fehlt in:\n${order.joinToString("\n")}", at >= 0) }
        assertEquals("Reihenfolge:\n${order.joinToString("\n")}", positions.sorted(), positions)
    }

    @Test
    fun `je Stepper-Tipp genau eine Ansage – neues Muster`() {
        val state = show(PatternEditorState.new(prefix, PatternWish(activating = false, withHold = true)))
        tapEveryStepper(state)
    }

    @Test
    fun `je Stepper-Tipp genau eine Ansage – gespeichertes Muster bearbeiten`() {
        val spec = PatternSpec(4.0, 1.0, 6.0, null, activating = false)
        val state = show(PatternEditorState.forEdit(prefix, id = 7, spec = spec, minutes = 5, name = "Abendruhe"))
        assertTrue(state.saved)
        // „+“ leert das Lesezeichen, „−“ füllt es wieder (gleiche Werte wie gespeichert) – beides
        // ohne eigene Ansage.
        tapEveryStepper(state)
    }

    /** Jeden Knopf jedes Steppers einmal, sofern er nicht am Anschlag steht; jeweils genau eine Ansage. */
    private fun tapEveryStepper(state: PatternEditorState) {
        val steppers = listOf(R.string.cd_adjust_duration) + PatternField.entries.filter { state.rangeFor(it) != null }.map {
            when (it) {
                PatternField.INHALE -> R.string.phase_inhale
                PatternField.HOLD_FULL -> R.string.cd_hold_full
                PatternField.EXHALE -> R.string.phase_exhale
                PatternField.HOLD_EMPTY -> R.string.cd_hold_empty
            }
        }
        var tapped = 0
        steppers.forEach { spoken ->
            listOf(R.string.adjust_increase, R.string.adjust_decrease).forEach { action ->
                val button = "${string(spoken)}, ${string(action)}"
                val enabled = compose.onNodeWithContentDescription(button).fetchSemanticsNode()
                    .config.getOrNull(SemanticsProperties.Disabled) == null
                if (enabled) {
                    val said = announcementsAfterTapOn(button)
                    assertEquals("Ansagen nach „$button“: $said", 1, said.size)
                    assertTrue("„$button“ sagt den Wert an: $said", said.single().startsWith(string(spoken) + ":"))
                    tapped++
                }
            }
        }
        assertTrue("mindestens ein Knopf je Stepper", tapped >= steppers.size)
    }
}
