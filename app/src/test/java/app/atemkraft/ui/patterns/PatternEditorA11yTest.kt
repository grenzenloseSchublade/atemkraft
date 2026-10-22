package app.atemkraft.ui.patterns

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import app.atemkraft.R
import app.atemkraft.cue.FakePreviewSoundOutput
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
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
 * TalkBack im Muster-Editor (MUSTER-10, A11Y-04, Nachtrag 4): Die Lesereihenfolge folgt der Seite –
 * Startpunkt, Dauer, Atemzug mit Kurve und Vorschau, Ablauf, „Phase hinzufügen“, Name, „Aus der
 * Yoga-Lehre“, Start –, jeder Stepper-Tipp löst genau eine Ansage aus (den neuen Wert; weder
 * Einordnung, Status am Kopf, Kurve noch Lesezeichen melden sich dazu), und alles ist ohne Ziehen
 * bedienbar: die Kurve ist ein Knoten ohne Live-Region, Auswahl, Sekunden, Entfernen und
 * Hinzufügen gehen über die Ablauf-Liste.
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
                PatternEditorScreen(
                    state = state,
                    onSavedChange = {},
                    onStart = { _, _ -> },
                    onOpenLore = {},
                    onBack = {},
                    soundOutput = { FakePreviewSoundOutput() },
                )
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

    private fun free(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { (type, seconds) -> FreePhase(type, seconds) })

    private val box get() = FreePatternTemplates.all.single { it.label == "Box" }.pattern

    @Test
    fun `Lesereihenfolge folgt der Seite`() {
        show(PatternEditorState.new(prefix, box).apply { select(1) })
        val order = traversal()
        val milestones = listOf(
            string(R.string.pattern_editor_start),
            "${string(R.string.cd_adjust_duration)}, ${string(R.string.adjust_increase)}",
            string(R.string.pattern_editor_breath),
            app.getString(R.string.cd_breath_curve, box.spokenDescription()),
            string(R.string.cd_preview_play),
            string(R.string.cd_preview_sound),
            string(R.string.pattern_editor_sequence),
            // Die Zeilen lesen sich als „Nummer, Name, Sekunden“.
            "1, Einatmen",
            "2, Halten voll",
            app.getString(R.string.cd_pattern_phase_remove, 2),
            "${app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")}, ${string(R.string.adjust_increase)}",
            "4, Halten leer",
            string(R.string.pattern_editor_add),
            string(R.string.cd_pattern_add_in),
            string(R.string.cd_pattern_add_out),
            string(R.string.pattern_editor_name_label),
            string(R.string.detail_lore),
            string(R.string.action_start),
        )
        val positions = milestones.map { m -> order.indexOfFirst { it.contains(m) } }
        milestones.zip(positions).forEach { (m, at) -> assertTrue("„$m“ fehlt in:\n${order.joinToString("\n")}", at >= 0) }
        assertEquals("Reihenfolge:\n${order.joinToString("\n")}", positions.sorted(), positions)
    }

    @Test
    fun `die Kurve ist ein Knoten mit Beschreibung und ohne Live-Region`() {
        show(PatternEditorState.new(prefix, box))
        compose.onNodeWithContentDescription(app.getString(R.string.cd_breath_curve, box.spokenDescription()))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `je Stepper-Tipp genau eine Ansage – neues Muster`() {
        val state = show(PatternEditorState.new(prefix, box).apply { select(1) })
        tapEveryStepper(state)
    }

    @Test
    fun `je Stepper-Tipp genau eine Ansage – gespeichertes Muster bearbeiten`() {
        val state = show(PatternEditorState.forEdit(prefix, id = 7, pattern = box, minutes = 5, name = "Abendruhe").apply { select(1) })
        assertTrue(state.saved)
        // „+“ leert das Lesezeichen, „−“ füllt es wieder (gleiche Werte wie gespeichert) – beides
        // ohne eigene Ansage.
        tapEveryStepper(state)
    }

    /** Dauer und die Sekunden der gewählten Phase: jeder Knopf einmal, jeweils genau eine Ansage. */
    private fun tapEveryStepper(state: PatternEditorState) {
        val selected = requireNotNull(state.selected)
        val steppers = listOf(
            string(R.string.cd_adjust_duration),
            app.getString(R.string.pattern_editor_phase_spoken, selected + 1, state.pattern.labelAt(selected)),
        )
        var tapped = 0
        steppers.forEach { spoken ->
            listOf(R.string.adjust_increase, R.string.adjust_decrease).forEach { action ->
                val button = "$spoken, ${string(action)}"
                val enabled = compose.onNodeWithContentDescription(button).fetchSemanticsNode()
                    .config.getOrNull(SemanticsProperties.Disabled) == null
                if (enabled) {
                    val said = announcementsAfterTapOn(button)
                    assertEquals("Ansagen nach „$button“: $said", 1, said.size)
                    assertTrue("„$button“ sagt den Wert an: $said", said.single().startsWith("$spoken:"))
                    tapped++
                }
            }
        }
        assertEquals(4, tapped)
    }

    @Test
    fun `ohne Ziehen bedienbar – waehlen, Sekunden, entfernen, hinzufuegen`() {
        val state = show(PatternEditorState.new(prefix))
        fun click(description: String) = compose.onNodeWithContentDescription(description).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)

        // Phase 2 (Ausatmen) wählen, wie TalkBack es tut: Doppeltipp = Klick-Aktion der Zeile.
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and hasText("Ausatmen"))
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, state.selected)
        click("${app.getString(R.string.pattern_editor_phase_spoken, 2, "Ausatmen")}, ${string(R.string.adjust_increase)}")
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.OUT to 7), state.pattern)

        click(string(R.string.cd_pattern_add_hold))
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.OUT to 7, FreePhaseType.HOLD to 2), state.pattern)
        assertEquals(2, state.selected)
        click(app.getString(R.string.cd_pattern_phase_remove, 3))
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.OUT to 7), state.pattern)
        click(string(R.string.cd_pattern_add_in))
        assertEquals(3, state.pattern.phases.size)
        assertTrue(state.pattern.isValid())
    }
}
