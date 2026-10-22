package app.atemkraft.ui.patterns

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
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
 * TalkBack im Muster-Editor (MUSTER-10, A11Y-04, Nachtrag 4 bis 6): Die Lesereihenfolge folgt der
 * Seite – Atemzug mit Vorschau-Knöpfen, Kurve, Werkzeugleiste (gewählte Phase, Sekunden, Papierkorb,
 * „Neu nach …“, Hinzufügen), Ablauf, Name, „Aus der Yoga-Lehre“, dann die feste Leiste (Startpunkt,
 * Dauer, Start) –, jeder Stepper-Tipp löst genau eine Ansage aus (den neuen Wert; weder Einordnung,
 * Status am Kopf, Kurve noch Lesezeichen melden sich dazu), eine Vorlage nur eine (das
 * Startpunkt-Symbol), und alles ist ohne
 * Ziehen bedienbar: die Kurve ist ein Knoten ohne Live-Region, Auswahl über die Ablauf-Liste,
 * Sekunden, Entfernen und Hinzufügen über die Werkzeugleiste.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class PatternEditorA11yTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val prefix get() = app.getString(R.string.pattern_editor_title)

    private fun string(id: Int) = app.getString(id)

    private lateinit var inputMode: InputModeManager

    private fun show(state: PatternEditorState): PatternEditorState {
        compose.setContent {
            inputMode = LocalInputModeManager.current
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
        // Die Dauer steht in der festen Leiste unten, nicht in der Scroll-Fläche.
        runCatching { node.performScrollTo() }
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
            string(R.string.pattern_editor_breath),
            string(R.string.cd_preview_play),
            string(R.string.cd_preview_sound),
            app.getString(R.string.cd_breath_curve, box.spokenDescription()),
            app.getString(R.string.cd_pattern_selected, app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")),
            "${app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")}, ${string(R.string.adjust_increase)}",
            app.getString(R.string.cd_pattern_phase_remove, 2),
            app.getString(R.string.cd_pattern_add_after, 2, "Halten voll"),
            string(R.string.cd_pattern_add_in),
            string(R.string.cd_pattern_add_out),
            string(R.string.pattern_editor_sequence),
            string(R.string.pattern_editor_name_label),
            string(R.string.detail_lore),
            // Feste Leiste unten (Nachtrag 6): Startpunkt, Dauer, Start.
            app.getString(R.string.cd_pattern_editor_start, "Box"),
            "${string(R.string.cd_adjust_duration)}, ${string(R.string.adjust_increase)}",
            app.getString(R.string.cd_pattern_editor_play, ""),
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
        val selected = state.selected
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
        fun row(label: String) = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and hasText(label))
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)

        // „Ablauf“ aufklappen, dann wie TalkBack wählen: Doppeltipp = Klick-Aktion der Zeile.
        compose.onNode(hasText(string(R.string.pattern_editor_sequence)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        row("Einatmen")
        assertEquals(0, state.selected)
        row("Ausatmen")
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

    @Test
    fun `Einfuegen und Entfernen sagt die gewaehlte Phase genau einmal an`() {
        val state = show(PatternEditorState.new(prefix, box).apply { select(1) })
        // Gesprochen wird die Beschreibung, der sichtbare Text („3 Halten voll“) nicht.
        val added = announcementsAfterTapOn(string(R.string.cd_pattern_add_hold)).map { it.substringBefore(" | ") }
        assertEquals(listOf(app.getString(R.string.cd_pattern_phase_added, "Halten voll", 3, 5, 12)), added)

        // Mittlere Phase entfernen: Der Stepper der nachrückenden Phase schweigt, die Zeile sagt es.
        state.select(1)
        compose.waitForIdle()
        val removed = announcementsAfterTapOn(app.getString(R.string.cd_pattern_phase_remove, 2)).map { it.substringBefore(" | ") }
        assertEquals(listOf(app.getString(R.string.cd_pattern_phase_removed, 2, 4, 12)), removed)

        // Der nächste Stepper-Tipp sagt wieder nur seinen Wert an.
        val spoken = app.getString(R.string.pattern_editor_phase_spoken, 2, state.pattern.labelAt(1))
        val said = announcementsAfterTapOn("$spoken, ${string(R.string.adjust_increase)}")
        assertEquals(1, said.size)
        assertTrue(said.single(), said.single().startsWith("$spoken:"))
    }

    @Test
    fun `nach Einfuegen und Wechsel der Auswahl liest die Zeile wieder die gewaehlte Phase`() {
        val state = show(PatternEditorState.new(prefix, box).apply { select(1) })
        announcementsAfterTapOn(string(R.string.cd_pattern_add_hold))
        assertEquals(2, state.selected)
        val notice = app.getString(R.string.cd_pattern_phase_added, "Halten voll", 3, 5, 12)
        compose.onNodeWithContentDescription(notice).assertExists()

        // Andere Phase wählen (wie über Ablauf-Liste oder Kurve): Die Ansage ist verbraucht.
        compose.runOnIdle { state.select(4) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(notice).assertDoesNotExist()
        val selected = app.getString(R.string.cd_pattern_selected, app.getString(R.string.pattern_editor_phase_spoken, 5, state.pattern.labelAt(4)))
        compose.onNodeWithContentDescription(selected).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))

        // Auch nach dem Entfernen und einem Wechsel zurück gilt wieder „Gewählte Phase: …“.
        announcementsAfterTapOn(app.getString(R.string.cd_pattern_phase_remove, 5))
        val removed = app.getString(R.string.cd_pattern_phase_removed, 5, 4, 12)
        compose.onNodeWithContentDescription(removed).assertExists()
        compose.runOnIdle { state.select(0) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription(removed).assertDoesNotExist()
        // Der Stepper der nun gewählten Phase sagt seinen nächsten Wert wieder an.
        val spoken = app.getString(R.string.pattern_editor_phase_spoken, 1, "Einatmen")
        val said = announcementsAfterTapOn("$spoken, ${string(R.string.adjust_increase)}")
        assertEquals(said.toString(), 1, said.size)
        assertTrue(said.single(), said.single().startsWith("$spoken:"))
    }

    @Test
    fun `Zurueckwaehlen einer Phase sagt ihren Wert nicht noch einmal an`() {
        val state = show(PatternEditorState.new(prefix, box).apply { select(1) })
        val spoken = app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")
        assertEquals(1, announcementsAfterTapOn("$spoken, ${string(R.string.adjust_increase)}").size)
        compose.runOnIdle { state.select(2) }
        compose.waitForIdle()
        val before = liveRegions()
        compose.runOnIdle { state.select(1) }
        compose.waitForIdle()
        val said = liveRegions().filter { (id, text) -> before[id] != text }.values.toList()
        assertEquals("Ansagen nach dem Zurückwählen: $said", emptyList<String>(), said)
    }

    @Test
    fun `Kippt Entfernen oder Einfuegen die Intensitaet, sagt die Zeile es mit`() {
        // Ein 1 · Halten 2 · Aus 1: 15 pro Minute; ohne das Halten 30 pro Minute, also intensiv.
        val state = show(PatternEditorState.new(prefix, free(FreePhaseType.IN to 1, FreePhaseType.HOLD to 2, FreePhaseType.OUT to 1)).apply { select(1) })
        val removed = announcementsAfterTapOn(app.getString(R.string.cd_pattern_phase_remove, 2)).map { it.substringBefore(" | ") }
        assertTrue(state.pattern.isIntense)
        assertEquals(listOf(app.getString(R.string.cd_pattern_phase_removed, 2, 2, 12) + ", ${string(R.string.detail_intense)}"), removed)

        // Halten wieder einfügen: 15 pro Minute, nicht mehr intensiv.
        compose.runOnIdle { state.select(0) }
        compose.waitForIdle()
        val added = announcementsAfterTapOn(string(R.string.cd_pattern_add_hold)).map { it.substringBefore(" | ") }
        assertTrue(!state.pattern.isIntense)
        assertEquals(listOf(app.getString(R.string.cd_pattern_phase_added, "Halten voll", 2, 3, 12) + ", ${string(R.string.cd_pattern_not_intense)}"), added)
    }

    @Test
    fun `Kippt ein Stepper-Tipp ins Intensive, sagt der Wert es mit`() {
        val state = show(PatternEditorState.new(prefix, box).apply { select(1) })
        val spoken = app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")
        val plus = "$spoken, ${string(R.string.adjust_increase)}"
        repeat(6) { compose.onNodeWithContentDescription(plus).performScrollTo().performSemanticsAction(SemanticsActions.OnClick) }
        assertEquals(10, state.pattern.phases[1].seconds)
        val said = announcementsAfterTapOn(plus)
        assertEquals(1, said.size)
        assertTrue(said.single(), said.single().substringBefore(" | ").endsWith(", ${string(R.string.detail_intense)}"))
    }

    @Test
    fun `Vorlage gewaehlt - das Startpunkt-Symbol sagt einmal an und bekommt den Tastatur-Fokus`() {
        show(PatternEditorState.new(prefix))
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "4·6")).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val before = liveRegions()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and hasText("Viloma I")).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val said = liveRegions().filter { (id, spoken) -> before[id] != spoken }.values.toList()
        // Eine Ansage: das Symbol selbst („Startpunkt wählen, aktuell: Viloma I“).
        assertEquals(said.toString(), 1, said.size)
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "Viloma I")).assertIsFocused()
    }

    @Test
    fun `Tastatur - Zuruecksetzen in der Leiste gibt den Fokus an den Play-Knopf`() {
        val state = show(PatternEditorState.new(prefix, box))
        compose.runOnIdle { state.add(FreePhaseType.IN) }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val reset = compose.onNodeWithContentDescription(string(R.string.adjust_reset))
        reset.performSemanticsAction(SemanticsActions.RequestFocus)
        reset.assertIsFocused()
        reset.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(box, state.pattern)
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_editor_play).substringBefore(","), substring = true).assertIsFocused()
    }

    @Test
    fun `Tastatur - Papierkorb oder Hinzufuegen abgeschaltet, der Fokus geht auf den Gegenpart`() {
        val state = show(PatternEditorState.new(prefix, box).apply { select(3) })
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val trash = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 4))
        trash.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        trash.assertIsFocused()
        trash.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        // Nun ist „Ausatmen“ gewählt, das einzige Aus: Sein Papierkorb ist aus, der Fokus auf „+ Ein“.
        assertEquals(2, state.selected)
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_add_in)).assertIsFocused()

        repeat(8) { compose.runOnIdle { state.add(FreePhaseType.HOLD) } }
        val addOut = compose.onNodeWithContentDescription(string(R.string.cd_pattern_add_out))
        addOut.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        addOut.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(12, state.pattern.phases.size)
        // Die zwölfte Phase schaltet die Knöpfe ab: Fokus auf den Papierkorb der neuen Phase.
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, state.selected + 1)).assertIsFocused()
    }
}
