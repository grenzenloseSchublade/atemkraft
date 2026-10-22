package app.atemkraft.ui.patterns

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import app.atemkraft.R
import app.atemkraft.cue.FakePreviewSoundOutput
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SoundMode
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
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

/**
 * Muster-Editor „Frei gestalten“ (MUSTER-10, Nachtrag 4): Startpunkt eingeklappt mit Vorlagen,
 * Ablauf-Liste (Auswahl, Sekunden, Entfernen, Einfügemarke), Hinzufügen hinter der Auswahl bis 12
 * Phasen, „intensiv“ mit Hinweis und Sicherheitsdialog, Ton-Schalter der Vorschau, Einordnung ohne
 * Live-Region, Lesezeichen, Name, Start mit dem Entwurf.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class PatternEditorScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val prefix get() = app.getString(R.string.pattern_editor_title)
    private val saves = mutableListOf<Boolean>()
    private val lore = mutableListOf<String>()
    private val started = mutableListOf<Pair<Exercise, SessionConfig>>()
    private var confirmedSafety = 0

    private fun free(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { (type, seconds) -> FreePhase(type, seconds) })

    private fun template(label: String) = FreePatternTemplates.all.single { it.label == label }

    private fun show(
        state: PatternEditorState,
        cueSettings: CueSettings = CueSettings(soundMode = SoundMode.CUES),
        requireSafetyConfirm: Boolean = true,
    ): PatternEditorState {
        compose.setContent {
            AtemkraftTheme {
                PatternEditorScreen(
                    state = state,
                    onSavedChange = { saves += it },
                    onStart = { exercise, config -> started += exercise to config },
                    onOpenLore = { lore += it },
                    onBack = {},
                    cueSettings = cueSettings,
                    requireSafetyConfirm = requireSafetyConfirm,
                    onConfirmedSafety = { confirmedSafety++ },
                    soundOutput = { FakePreviewSoundOutput() },
                )
            }
        }
        return state
    }

    private fun string(id: Int) = app.getString(id)

    private fun stepButton(spoken: String, action: Int) = compose.onNodeWithContentDescription("$spoken, ${string(action)}")

    /** Zeile [number] der Ablauf-Liste (die auswählbare Fläche, nicht der Stepper). */
    private fun row(number: Int, label: String) = compose.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and hasText(label) and hasText("$number"),
    )

    private fun visibleSegment(label: String) = compose.onNode(
        hasText(label) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and
            SemanticsMatcher("sichtbar") { it.size.width > 0 },
    )

    // ---- Startpunkt ----

    @Test
    fun `Startpunkt ist eingeklappt, nennt die Vorlage und klappt nach der Wahl wieder zu`() {
        val state = show(PatternEditorState.new(prefix))
        val header = compose.onNode(hasText(string(R.string.pattern_editor_start)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_collapsed)))
        compose.onNodeWithContentDescription("4·6").assertExists()
        visibleSegment("Box").assertDoesNotExist()

        header.performClick()
        visibleSegment("4·6").assertIsSelected()
        visibleSegment("Seufzer").assertIsNotSelected().performClick()
        assertEquals(template("Seufzer").pattern, state.pattern)
        visibleSegment("Box").assertDoesNotExist()
        compose.onNodeWithContentDescription("Seufzer").assertExists()

        // Eine Änderung macht aus der Vorlage „angepasst“.
        compose.runOnIdle { state.setSeconds(0, 5) }
        compose.onNodeWithContentDescription(string(R.string.adjust_modified)).assertExists()
    }

    // ---- Dauer ----

    @Test
    fun `die Dauer aendert die Folge nicht`() {
        val state = show(PatternEditorState.new(prefix))
        stepButton(string(R.string.cd_adjust_duration), R.string.adjust_increase).performClick()
        assertEquals(6, state.minutes)
        assertEquals(FreePattern.DEFAULT, state.pattern)
        compose.onNodeWithText(string(R.string.action_start), substring = true).assert(hasText(app.getString(R.string.duration_approx, 6), substring = true))
    }

    // ---- Ablauf ----

    @Test
    fun `Zeile antippen waehlt sie, die gewaehlte traegt Stepper und Papierkorb, nochmal hebt die Auswahl auf`() {
        val state = show(PatternEditorState.new(prefix, template("Box").pattern))
        val spoken = app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")
        stepButton(spoken, R.string.adjust_increase).assertDoesNotExist()

        row(2, "Halten voll").performScrollTo().assertIsNotSelected().performClick()
        assertEquals(1, state.selected)
        row(2, "Halten voll").assertIsSelected()
        stepButton(spoken, R.string.adjust_increase).performScrollTo().performClick()
        assertEquals(5, state.pattern.phases[1].seconds)
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 2)).assertIsEnabled()

        row(2, "Halten voll").performClick()
        assertNull(state.selected)
        stepButton(spoken, R.string.adjust_increase).assertDoesNotExist()
    }

    @Test
    fun `Papierkorb ist aus, wenn die Folge danach ungueltig waere, sonst entfernt er die Phase`() {
        val state = show(PatternEditorState.new(prefix))
        row(1, "Einatmen").performScrollTo().performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 1)).assertIsNotEnabled()

        compose.runOnIdle { state.applyTemplate(template("Box")) }
        row(4, "Halten leer").performScrollTo().performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 4)).performClick()
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.HOLD to 4, FreePhaseType.OUT to 4), state.pattern)
        assertEquals(2, state.selected)
    }

    @Test
    fun `Hinzufuegen kommt hinter die gewaehlte Phase, die Karte nennt die Stelle`() {
        val state = show(PatternEditorState.new(prefix))
        compose.onNodeWithText(string(R.string.pattern_editor_add_end), substring = true).performScrollTo()
        compose.onNodeWithText(string(R.string.pattern_editor_insert_here)).assertExists()

        row(1, "Einatmen").performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.pattern_editor_add_after, 1, "Einatmen"), substring = true).assertExists()
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_add_hold)).performScrollTo().performClick()
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.HOLD to 2, FreePhaseType.OUT to 6), state.pattern)
        assertEquals(1, state.selected)
        compose.onNodeWithText(app.getString(R.string.pattern_editor_add_after, 2, "Halten voll"), substring = true).assertExists()
        // Der Kopf „Ablauf“ nennt einmal, was wohin kam; nach der nächsten Änderung nur die Zahl.
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_added, "Halten voll", 2, 3, 12)).assertExists()
        compose.runOnIdle { state.setSeconds(1, 3) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_sequence_count, 3, 12)).assertExists()
    }

    @Test
    fun `bei 12 Phasen sind die Knoepfe aus`() {
        val state = show(PatternEditorState.new(prefix))
        repeat(9) { compose.runOnIdle { state.add(FreePhaseType.HOLD) } }
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_add_out)).performScrollTo().assertIsEnabled().performClick()
        assertEquals(12, state.pattern.phases.size)
        listOf(R.string.cd_pattern_add_in, R.string.cd_pattern_add_hold, R.string.cd_pattern_add_out).forEach {
            compose.onNodeWithContentDescription(string(it)).assertIsNotEnabled()
        }
    }

    // ---- Intensiv ----

    @Test
    fun `intensives Muster zeigt Kennzeichen und Hinweis und startet erst nach dem Sicherheitsdialog`() {
        val state = show(PatternEditorState.new(prefix, template("4-7-8").pattern))
        compose.onNodeWithText(string(R.string.detail_intense)).assertDoesNotExist()

        compose.runOnIdle { state.setSeconds(1, 12) }
        compose.onNodeWithText(string(R.string.detail_intense)).assertExists()
        compose.onNodeWithText(state.pattern.intensityNote()!!, substring = true).performScrollTo()

        compose.onNodeWithText(string(R.string.action_start), substring = true).performClick()
        assertTrue(started.isEmpty())
        compose.onNodeWithText(string(R.string.safety_confirm)).performClick()
        assertEquals(1, confirmedSafety)
        val (exercise, _) = started.single()
        assertTrue(exercise.caution)
    }

    @Test
    fun `nach bestaetigtem Hinweis startet ein intensives Muster direkt`() {
        show(PatternEditorState.new(prefix, free(FreePhaseType.IN to 1, FreePhaseType.OUT to 1)), requireSafetyConfirm = false)
        compose.onNodeWithText(string(R.string.action_start), substring = true).performClick()
        assertTrue(started.single().first.caution)
        compose.onNodeWithText(string(R.string.safety_confirm)).assertDoesNotExist()
    }

    // ---- Ton der Vorschau ----

    @Test
    fun `Ton-Schalter ist ein Switch, an bei Klangmodus Toene`() {
        show(PatternEditorState.new(prefix))
        val switch = compose.onNodeWithContentDescription(string(R.string.cd_preview_sound))
        switch.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
    }

    @Test
    fun `Ton-Schalter ist aus und abgeschaltet bei Klangmodus Aus`() {
        show(PatternEditorState.new(prefix), cueSettings = CueSettings(soundMode = SoundMode.OFF))
        compose.onNodeWithContentDescription(string(R.string.cd_preview_sound))
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
    }

    // ---- Einordnung ----

    @Test
    fun `Einordnung wechselt von Langhana zu Samana und hat keine Live-Region`() {
        val state = show(PatternEditorState.new(prefix))
        val langhana = state.exercise().lore!!.teaser
        assertTrue(langhana, langhana.contains("Langhana"))
        compose.onNodeWithText(langhana, useUnmergedTree = true)
            .performScrollTo()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))

        // 4·6 → 4·4: gleich langes Ein- und Ausatmen.
        compose.runOnIdle { state.setSeconds(1, 4) }
        val samana = state.exercise().lore!!.teaser
        assertTrue(samana, samana.contains("Samana"))
        compose.onNodeWithText(langhana, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(samana, useUnmergedTree = true)
            .performScrollTo()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `Verweis in die Atemlehre ruft onOpenLore mit der Karte`() {
        show(PatternEditorState.new(prefix))
        compose.onNodeWithText(string(R.string.detail_lore)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.detail_lore_link, "Langhana")).performScrollTo().performClick()
        assertEquals(listOf("langhana"), lore)
    }

    // ---- Lesezeichen ----

    @Test
    fun `Lesezeichen ist eine Checkbox mit Zustand und Live-Region, neu angelegt sofort umkehrbar`() {
        val state = show(PatternEditorState.new(prefix))
        val toggle = compose.onNodeWithContentDescription(string(R.string.cd_pattern_save))
        toggle
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_not_saved)))
            .performClick()
        assertEquals(listOf(true), saves)

        // Der Aufrufer hat gespeichert und meldet die Id zurück; der eigene Tipp meldet sich per Live-Region.
        compose.runOnIdle { state.markSaved(7) }
        toggle
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_saved)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        toggle.performClick()
        assertEquals(listOf(true, false), saves)
    }

    @Test
    fun `Bestehendes Muster entfernt das Lesezeichen nur zweistufig`() {
        show(PatternEditorState.forEdit(prefix, id = 7, pattern = FreePattern.DEFAULT, minutes = 5, name = "Abendruhe"))
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_save))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_saved)))
            .performClick()
        assertEquals(emptyList<Boolean>(), saves)

        val confirm = app.getString(R.string.cd_delete_confirm, "Abendruhe")
        compose.onNodeWithContentDescription(confirm)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .performClick()
        assertEquals(listOf(false), saves)
    }

    @Test
    fun `Geaenderte Werte leeren das Lesezeichen, ein Tipp speichert sie einstufig`() {
        val state = show(PatternEditorState.forEdit(prefix, id = 7, pattern = FreePattern.DEFAULT, minutes = 5, name = "Abendruhe"))
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_add_hold)).performScrollTo().performClick()
        assertFalse(state.saved)
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_save))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_not_saved)))
            .performClick()
        assertEquals(listOf(true), saves)
    }

    // ---- Name ----

    @Test
    fun `Namensfeld folgt den Zahlen, traegt sein Label und nimmt keine Emoji an`() {
        val state = show(PatternEditorState.new(prefix))
        val field = compose.onNode(hasSetTextAction())
        field.performScrollTo()
            .assert(hasText(string(R.string.pattern_editor_name_label), substring = true))
            .assert(hasText("$prefix 4·6", substring = true))

        field.performTextReplacement("Abend😀ruhe\nkurz")
        assertEquals("Abendruhe kurz", state.name)
        assertTrue(state.nameEdited)
    }

    @Test
    fun `Fertig im Namensfeld behaelt den Fokus fuer Tastatur und D-Pad`() {
        show(PatternEditorState.new(prefix))
        val field = compose.onNode(hasSetTextAction())
        field.performScrollTo().performClick()
        field.assertIsFocused()
        field.performImeAction()
        field.assertIsFocused()
    }

    @Test
    fun `Eingabefilter entspricht der Namensbereinigung des Imports`() {
        assertEquals("Ruhig  Abend", nameInput("Ruhig 🌙\tAbend"))
        assertEquals("ab", nameInput("a​b"))
        assertEquals("Ruhe ☀", nameInput("Ruhe ☀"))
    }

    // ---- Start ----

    @Test
    fun `Starten ruft onStart mit dem Entwurf und der Dauer, Reset erst nach einer Aenderung`() {
        val state = show(PatternEditorState.new(prefix, template("Box").pattern))
        val reset = string(R.string.adjust_reset)
        compose.onNodeWithContentDescription(reset).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.action_start), substring = true)
            .assert(hasText(app.getString(R.string.duration_approx, 5), substring = true))
            .performClick()
        val (exercise, config) = started.single()
        assertEquals(RandomPatternGenerator.DRAFT_ID, exercise.id)
        assertEquals(SessionConfig(minutes = 5), config)
        assertFalse(exercise.caution)
        val phases = exercise.segments.single().phases
        assertEquals(listOf(PhaseType.INHALE, PhaseType.HOLD_FULL, PhaseType.EXHALE, PhaseType.HOLD_EMPTY), phases.map { it.type })
        assertEquals(PhaseDuration.Fixed(4_000L), phases.first().duration)

        compose.runOnIdle { state.add(FreePhaseType.OUT) }
        compose.onNodeWithContentDescription(reset).performClick()
        compose.waitForIdle()
        assertEquals(template("Box").pattern, state.pattern)
        compose.onNode(hasContentDescription(reset)).assertDoesNotExist()
    }

    @Test
    fun `ein Atemzug pro Minute steht in der Einzahl`() {
        show(PatternEditorState.new(prefix, free(FreePhaseType.IN to 30, FreePhaseType.OUT to 30)))
        compose.onNodeWithText(string(R.string.pattern_editor_rate_one), substring = true).assertExists()
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_editor_rate_one), substring = true).assertExists()
    }
}
