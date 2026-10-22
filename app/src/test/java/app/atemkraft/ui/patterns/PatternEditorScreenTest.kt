package app.atemkraft.ui.patterns

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.compose.ui.test.performSemanticsAction
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Muster-Editor „Frei gestalten“ (MUSTER-10, Nachtrag 4 und 5): Startpunkt als Pille mit Vorlagen,
 * Dauer daneben, Werkzeugleiste (gewählte Phase mit Sekunden und Papierkorb, Hinzufügen hinter
 * der Auswahl bis 12 Phasen), Ablauf-Liste eingeklappt und synchron, Vorschau im Kopf „Atemzug“,
 * „intensiv“ mit Hinweis und Sicherheitsdialog, Ton-Schalter, Einordnung ohne Live-Region,
 * Lesezeichen, Name, Start mit dem Entwurf.
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

    /** „Ablauf“ aufklappen (beim Öffnen eingeklappt). */
    private fun openSequence() {
        compose.onNode(hasText(string(R.string.pattern_editor_sequence)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_collapsed)))
            .performClick()
    }

    // ---- Startpunkt ----

    @Test
    fun `Startpunkt ist eingeklappt, nennt die Vorlage und klappt nach der Wahl wieder zu`() {
        val state = show(PatternEditorState.new(prefix))
        val pill = compose.onNode(hasText(string(R.string.pattern_editor_start)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        pill.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_collapsed)))
            .assert(hasText("4·6"))
        visibleSegment("Box").assertDoesNotExist()

        pill.performClick()
        visibleSegment("4·6").assertIsSelected()
        visibleSegment("Seufzer").assertIsNotSelected().performClick()
        assertEquals(template("Seufzer").pattern, state.pattern)
        // Nach einer Vorlage ist ihre letzte Phase gewählt.
        assertEquals(2, state.selected)
        visibleSegment("Box").assertDoesNotExist()
        pill.assert(hasText("Seufzer"))

        // Eine Änderung macht aus der Vorlage „angepasst“.
        compose.runOnIdle { state.setSeconds(0, 5) }
        pill.assert(hasText(string(R.string.adjust_modified)))
    }

    // ---- Dauer ----

    @Test
    fun `die Dauer aendert die Folge nicht`() {
        val state = show(PatternEditorState.new(prefix))
        compose.onNodeWithText("5\u00A0min").assertExists()
        stepButton(string(R.string.cd_adjust_duration), R.string.adjust_increase).performClick()
        assertEquals(6, state.minutes)
        assertEquals(FreePattern.DEFAULT, state.pattern)
        compose.onNodeWithText("6\u00A0min").assertExists()
        compose.onNodeWithText(string(R.string.action_start), substring = true).assert(hasText(app.getString(R.string.duration_approx, 6), substring = true))
    }

    // ---- Werkzeugleiste und Ablauf ----

    @Test
    fun `Werkzeugleiste zeigt die gewaehlte Phase, die Ablauf-Liste waehlt synchron, ein zweiter Tipp hebt nichts auf`() {
        val state = show(PatternEditorState.new(prefix, template("Box").pattern))
        // Beim Öffnen ist die letzte Phase gewählt: Stepper und Papierkorb gehören zu ihr.
        val last = app.getString(R.string.pattern_editor_phase_spoken, 4, "Halten leer")
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_selected, last)).assertExists()
        stepButton(last, R.string.adjust_increase).assertExists()

        openSequence()
        row(4, "Halten leer").assertIsSelected()
        row(2, "Halten voll").performScrollTo().assertIsNotSelected().performClick()
        assertEquals(1, state.selected)
        row(2, "Halten voll").assertIsSelected()
        val spoken = app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")
        stepButton(last, R.string.adjust_increase).assertDoesNotExist()
        stepButton(spoken, R.string.adjust_increase).performScrollTo().performClick()
        assertEquals(5, state.pattern.phases[1].seconds)
        compose.onNodeWithText("5\u00A0s").assertExists()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 2)).assertIsEnabled()

        row(2, "Halten voll").performClick()
        assertEquals(1, state.selected)
        stepButton(spoken, R.string.adjust_increase).assertExists()
    }

    @Test
    fun `Papierkorb ist aus, wenn die Folge danach ungueltig waere, sonst entfernt er die Phase`() {
        val state = show(PatternEditorState.new(prefix))
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 2)).assertIsNotEnabled()

        compose.runOnIdle { state.applyTemplate(template("Box")) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 4)).performClick()
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.HOLD to 4, FreePhaseType.OUT to 4), state.pattern)
        assertEquals(2, state.selected)
        // Die Zeile der nun gewählten Phase sagt das Entfernen einmal an.
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_removed, 4, 3, 12)).assertExists()
    }

    @Test
    fun `Hinzufuegen kommt hinter die gewaehlte Phase, die Leiste nennt die Stelle`() {
        val state = show(PatternEditorState.new(prefix))
        compose.onNodeWithText(app.getString(R.string.pattern_editor_add_after_short, 2)).assertExists()
            .assert(hasContentDescription(app.getString(R.string.cd_pattern_add_after, 2, "Ausatmen")))

        openSequence()
        row(1, "Einatmen").performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.pattern_editor_add_after_short, 1)).assertExists()
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_add_hold)).performScrollTo().performClick()
        assertEquals(free(FreePhaseType.IN to 4, FreePhaseType.HOLD to 2, FreePhaseType.OUT to 6), state.pattern)
        assertEquals(1, state.selected)
        compose.onNodeWithText(app.getString(R.string.pattern_editor_add_after_short, 2)).assertExists()
        // Die gewählte Phase nennt einmal, was wohin kam; nach der nächsten Änderung nur sich selbst.
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_added, "Halten voll", 2, 3, 12))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.runOnIdle { state.setSeconds(1, 3) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_selected, app.getString(R.string.pattern_editor_phase_spoken, 2, "Halten voll")))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
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

    // ---- Vorschau ----

    @Test
    fun `laufende Vorschau zeigt die Phase am Kopf Atemzug statt des Status`() {
        show(PatternEditorState.new(prefix, free(FreePhaseType.IN to 4, FreePhaseType.HOLD to 1, FreePhaseType.OUT to 6)))
        val status = app.getString(R.string.pattern_editor_cycle, 11, app.getString(R.string.pattern_editor_rate_short, "5,5"))
        compose.onNodeWithText(status).assertExists()

        // Uhr erst anhalten, dann den Klick direkt auslösen: Die Vorschau läuft endlos.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription(string(R.string.cd_preview_play)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(PREVIEW_IN_HOLD_MS)
        compose.onNodeWithText(status).assertDoesNotExist()
        compose.onNodeWithText("· Halten voll").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))

        compose.onNodeWithContentDescription(string(R.string.cd_preview_pause)).performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.advanceTimeBy(PREVIEW_STEP_MS)
        compose.onNodeWithText(status).assertExists()
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
    fun `ein Atemzug pro Minute steht gesprochen in der Einzahl`() {
        show(PatternEditorState.new(prefix, free(FreePhaseType.IN to 30, FreePhaseType.OUT to 30)))
        compose.onNodeWithText(app.getString(R.string.pattern_editor_rate_short, "1"), substring = true).assertExists()
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_editor_rate_one), substring = true).assertExists()
    }

    private companion object {
        /** 4,5 s nach Play: mitten in „Halten voll“ von 4·1·6. */
        const val PREVIEW_IN_HOLD_MS = 4_500L
        const val PREVIEW_STEP_MS = 100L
    }
}
