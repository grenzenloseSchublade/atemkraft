package app.atemkraft.ui.patterns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import app.atemkraft.R
import app.atemkraft.cue.FakePreviewSoundOutput
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SoundMode
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
 * Muster-Editor „Frei gestalten“ (MUSTER-10, Nachtrag 4 bis 6) auf Screen-Ebene: Startpunkt-Symbol
 * mit Vorlagen-Sheet, Grenzen der Werkzeugleiste, Vorschau im Kopf „Atemzug“, „intensiv“ mit
 * Sicherheitsdialog, Verweis in die Atemlehre, Lesezeichen für TalkBack. Zustand, Bedienung ohne
 * Ziehen und Fluss bis in die Datenbank prüfen `PatternEditorStateTest`, `PatternEditorA11yTest`
 * und `PatternEditorFlowTest`.
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
    private var requireSafetyConfirm by mutableStateOf(true)

    private fun free(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { (type, seconds) -> FreePhase(type, seconds) })

    private fun template(label: String) = FreePatternTemplates.all.single { it.label == label }

    private fun show(state: PatternEditorState): PatternEditorState {
        compose.setContent {
            AtemkraftTheme {
                PatternEditorScreen(
                    state = state,
                    onSavedChange = { saves += it },
                    onStart = { exercise, config -> started += exercise to config },
                    onOpenLore = { lore += it },
                    onBack = {},
                    cueSettings = CueSettings(soundMode = SoundMode.CUES),
                    requireSafetyConfirm = requireSafetyConfirm,
                    onConfirmedSafety = {
                        confirmedSafety++
                        requireSafetyConfirm = false
                    },
                    soundOutput = { FakePreviewSoundOutput() },
                )
            }
        }
        return state
    }

    private fun string(id: Int) = app.getString(id)

    /** Großer Play-Knopf der festen Leiste. */
    private fun play() = compose.onNodeWithContentDescription(string(R.string.cd_pattern_editor_play).substringBefore(","), substring = true)

    private fun visibleSegment(label: String) = compose.onNode(
        hasText(label) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and
            SemanticsMatcher("sichtbar") { it.size.width > 0 },
    )

    // ---- Startpunkt ----

    @Test
    fun `Startpunkt-Symbol nennt die Vorlage, das Sheet waehlt eine und schliesst sich`() {
        val state = show(PatternEditorState.new(prefix))
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "4·6")).assertExists()
        visibleSegment("Box").assertDoesNotExist()

        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "4·6")).performClick()
        visibleSegment("4·6").assertIsSelected()
        visibleSegment("Seufzer").assertIsNotSelected().performClick()
        compose.waitForIdle()
        assertEquals(template("Seufzer").pattern, state.pattern)
        visibleSegment("Box").assertDoesNotExist()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "Seufzer")).assertExists()

        // Eine Änderung macht aus der Vorlage „angepasst“.
        compose.runOnIdle { state.setSeconds(0, 5) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, string(R.string.adjust_modified))).assertExists()
    }

    /** Die Werkzeugleiste zeigt die Grenzen der freien Folge: nichts, was die Folge ungültig machte. */
    @Test
    fun `Werkzeugleiste schaltet an den Grenzen ab`() {
        val state = show(PatternEditorState.new(prefix))
        // 4·6: einziges Ein und einziges Aus – der Papierkorb ist aus.
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_phase_remove, 2)).assertIsNotEnabled()
        // Bei 12 Phasen ist jedes Hinzufügen aus.
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

    /** MUSTER-06: intensiv → Sicherheitsdialog; ist der Hinweis einmal bestätigt, startet es direkt. */
    @Test
    fun `intensives Muster zeigt Kennzeichen und Hinweis und startet erst nach dem Sicherheitsdialog`() {
        val state = show(PatternEditorState.new(prefix, template("4-7-8").pattern))
        compose.onNodeWithText(string(R.string.detail_intense)).assertDoesNotExist()

        compose.runOnIdle { state.setSeconds(1, 12) }
        compose.onNodeWithText(string(R.string.detail_intense)).assertExists()
        compose.onNodeWithText(state.pattern.intensityNote()!!, substring = true).performScrollTo()

        play().performClick()
        assertTrue(started.isEmpty())
        compose.onNodeWithText(string(R.string.safety_confirm)).performClick()
        assertEquals(1, confirmedSafety)
        val (exercise, _) = started.single()
        assertTrue(exercise.caution)

        play().performClick()
        assertEquals(2, started.size)
        compose.onNodeWithText(string(R.string.safety_confirm)).assertDoesNotExist()
    }

    // ---- Atemlehre ----

    @Test
    fun `Verweis in die Atemlehre ruft onOpenLore mit der Karte`() {
        show(PatternEditorState.new(prefix))
        compose.onNodeWithText(string(R.string.detail_lore)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.detail_lore_link, "Langhana")).performScrollTo().performClick()
        assertEquals(listOf("langhana"), lore)
    }

    // ---- Lesezeichen ----

    @Test
    fun `Lesezeichen ist eine Checkbox mit Zustand und Live-Region`() {
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
    }

    private companion object {
        /** 4,5 s nach Play: mitten in „Halten voll“ von 4·1·6. */
        const val PREVIEW_IN_HOLD_MS = 4_500L
        const val PREVIEW_STEP_MS = 100L
    }
}
