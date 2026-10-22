package app.atemkraft.ui.patterns

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternRules
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.PatternWish
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.withinCreationRules
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Muster-Editor (MUSTER-10): Wunsch-Reihen als Radiogruppen, Stepper mit den Grenzen der
 * Erzeugungsregeln (Anschlag abgeschaltet, Fokus beim Gegenknopf), kein „Halten leer“ bei
 * „sanft aktivierend“, Einordnung ohne Live-Region, Reset nur bei Abweichung, Verweis in die
 * Atemlehre, Lesezeichen (sofort umkehrbar bzw. zweistufig beim Bearbeiten), Start mit dem Entwurf.
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
    private lateinit var inputMode: InputModeManager

    private fun spec(inhale: Int, holdFull: Int?, exhale: Int, holdEmpty: Int?, activating: Boolean = false) = PatternSpec(inhale.toDouble(), holdFull?.toDouble(), exhale.toDouble(), holdEmpty?.toDouble(), activating)

    private fun show(state: PatternEditorState, onStart: (Exercise, SessionConfig) -> Unit = { _, _ -> }): PatternEditorState {
        compose.setContent {
            inputMode = LocalInputModeManager.current
            AtemkraftTheme {
                PatternEditorScreen(
                    state = state,
                    onSavedChange = { saves += it },
                    onStart = onStart,
                    onOpenLore = { lore += it },
                    onBack = {},
                )
            }
        }
        return state
    }

    private fun string(id: Int) = app.getString(id)

    private fun stepButton(spoken: Int, action: Int) = compose.onNodeWithContentDescription("${string(spoken)}, ${string(action)}")

    /** Das sichtbare Segment (die Auswahlreihe misst ihre Beschriftungen vorab in unsichtbaren Proben). */
    private fun segment(label: Int) = compose.onNode(
        hasText(string(label)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and
            SemanticsMatcher("sichtbar") { it.size.width > 0 },
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private fun keyboardMode() {
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.waitForIdle()
    }

    // ---- Wünsche ----

    @Test
    fun `Charakter und Halten sind Radiogruppen mit Gruppenlabel und setzen den Vorschlag`() {
        val state = show(PatternEditorState.new(prefix))
        compose.onNode(hasContentDescription(string(R.string.pattern_editor_character))).assertExists()
        compose.onNode(hasContentDescription(string(R.string.pattern_editor_hold))).assertExists()
        segment(R.string.detail_calm).assertIsSelected()
        segment(R.string.pattern_editor_hold_without).assertIsSelected()

        segment(R.string.daily_pattern_gentle_up).assertIsNotSelected().performClick()
        segment(R.string.daily_pattern_gentle_up).assertIsSelected()
        assertTrue(state.spec.activating)
        assertEquals(state.suggestion, state.spec)

        segment(R.string.pattern_editor_hold_with).performClick()
        assertEquals(PatternWish(activating = true, withHold = true), state.wish)
    }

    @Test
    fun `Anderer Vorschlag blaettert weiter, die Dauer aendert die Phasen nicht`() {
        val state = show(PatternEditorState.new(prefix))
        val first = state.spec
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_suggest)).performClick()
        assertNotEquals(first, state.spec)
        assertEquals(state.suggestion, state.spec)

        val before = state.spec
        stepButton(R.string.cd_adjust_duration, R.string.adjust_increase).performClick()
        assertEquals(6, state.minutes)
        assertEquals(before, state.spec)
        compose.onNodeWithText(string(R.string.action_start), substring = true).assert(hasText("ca. 6 min", substring = true))
    }

    // ---- Genau einstellen ----

    @Test
    fun `Stepper am Anschlag ist abgeschaltet und gibt den Fokus an den Gegenknopf`() {
        // Ruhig 4·6: Einatmen geht bis 6 (Ausatmen mindestens so lang).
        val state = show(PatternEditorState.new(prefix))
        assertEquals(3..6, state.rangeFor(PatternField.INHALE))
        val plus = stepButton(R.string.phase_inhale, R.string.adjust_increase)
        val minus = stepButton(R.string.phase_inhale, R.string.adjust_decrease)
        plus.performScrollTo()
        keyboardMode()
        plus.performSemanticsAction(SemanticsActions.RequestFocus)
        plus.performSemanticsAction(SemanticsActions.OnClick)
        plus.assertIsEnabled().assertIsFocused()
        plus.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(6, state.secondsOf(PatternField.INHALE))
        plus.assertIsNotEnabled()
        minus.assertIsFocused()
        assertTrue(state.spec.withinCreationRules())
    }

    @Test
    fun `Halten-Stepper sprechen 0 als aus, Halten leer fehlt bei sanft aktivierend`() {
        show(PatternEditorState.new(prefix))
        compose.onNodeWithContentDescription("${string(R.string.cd_hold_full)}: ${string(R.string.stepper_value_off)}").assertExists()
        compose.onNodeWithText(string(R.string.adjust_hold_empty)).assertExists()

        segment(R.string.daily_pattern_gentle_up).performClick()
        compose.onNodeWithText(string(R.string.adjust_hold_empty)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.adjust_hold_full)).assertExists()
        compose.onNodeWithText(string(R.string.pattern_editor_rule_active)).assertExists()
    }

    @Test
    fun `Reset erscheint nur bei Abweichung vom Vorschlag und setzt zurueck`() {
        val state = show(PatternEditorState.new(prefix))
        val reset = string(R.string.adjust_reset)
        compose.onNodeWithContentDescription(reset).assertDoesNotExist()

        stepButton(R.string.phase_exhale, R.string.adjust_increase).performScrollTo().performClick()
        assertTrue(state.modified)
        compose.onNodeWithContentDescription(reset).performClick()
        compose.waitForIdle()
        assertFalse(state.modified)
        assertEquals(state.suggestion, state.spec)
        compose.onNodeWithContentDescription(reset).assertDoesNotExist()
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

        // 4·6 → 5·5: gleich langes Ein- und Ausatmen.
        stepButton(R.string.phase_inhale, R.string.adjust_increase).performScrollTo().performClick()
        stepButton(R.string.phase_exhale, R.string.adjust_decrease).performClick()
        assertEquals(spec(5, null, 5, null), state.spec)
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
        show(PatternEditorState.forEdit(prefix, id = 7, spec = spec(4, 1, 6, null), minutes = 5, name = "Abendruhe"))
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
        val state = show(PatternEditorState.forEdit(prefix, id = 7, spec = spec(4, 1, 6, null), minutes = 5, name = "Abendruhe"))
        stepButton(R.string.cd_adjust_duration, R.string.adjust_increase).performClick()
        assertFalse(state.saved)
        compose.onNodeWithContentDescription(string(R.string.cd_pattern_save))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_not_saved)))
            .performClick()
        assertEquals(listOf(true), saves)
    }

    @Test
    fun `Regeltexte nennen die Grenzen aus PatternRules, samt Ausatmen hoechstens 8 s`() {
        val calm = string(R.string.pattern_editor_rule_calm)
        listOf(
            "höchstens ${PatternRules.EXHALE_MAX}\u00A0s",
            "${PatternRules.CALM_CYCLE_MIN} bis ${PatternRules.CALM_CYCLE_MAX}\u00A0s",
            "voll bis ${PatternRules.CALM_HOLD_FULL_MAX}\u00A0s",
            "leer bis ${PatternRules.CALM_HOLD_EMPTY_MAX}\u00A0s",
        ).forEach { assertTrue("„$it“ fehlt in „$calm“", it in calm) }
        val active = string(R.string.pattern_editor_rule_active)
        listOf(
            "${PatternRules.ACTIVE_CYCLE_MIN} bis ${PatternRules.ACTIVE_CYCLE_MAX}\u00A0s",
            "voll bis ${PatternRules.ACTIVE_HOLD_FULL_MAX}\u00A0s",
        ).forEach { assertTrue("„$it“ fehlt in „$active“", it in active) }
    }

    @Test
    fun `angepasst steht leise am Kopf, eine eigene Musterzeile gibt es nicht`() {
        val state = show(PatternEditorState.new(prefix))
        val modified = string(R.string.adjust_modified)
        compose.onNodeWithContentDescription(modified).assertDoesNotExist()
        stepButton(R.string.phase_exhale, R.string.adjust_increase).performScrollTo().performClick()
        assertTrue(state.modified)
        // A54 bei Schrift 1,0: passt hinter den Titel, deshalb mit Trennpunkt.
        compose.onNodeWithContentDescription(modified).assert(hasText("· $modified"))
        // Die Werte stehen in den Steppern, die Dauer im Start-Knopf – keine Zeile „Einatmen 4 s · …“.
        compose.onNodeWithText(RandomPatternGenerator.hintFor(state.spec), substring = true).assertDoesNotExist()
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
    fun `Starten ruft onStart mit dem gueltigen Entwurf und der Dauer`() {
        var started: Pair<Exercise, SessionConfig>? = null
        val state = show(PatternEditorState.new(prefix, PatternWish(activating = false, withHold = true))) { exercise, config -> started = exercise to config }
        compose.onNodeWithText(string(R.string.action_start), substring = true)
            .assert(hasText("ca. 5 min", substring = true))
            .performClick()
        val (exercise, config) = started!!
        assertEquals(RandomPatternGenerator.DRAFT_ID, exercise.id)
        assertEquals(SessionConfig(minutes = 5), config)
        assertFalse(exercise.caution)
        assertTrue(state.spec.withinCreationRules())
        val inhale = exercise.segments.flatMap { it.phases }.first { it.type == PhaseType.INHALE }
        assertEquals(PhaseDuration.Fixed(state.secondsOf(PatternField.INHALE) * 1000L), inhale.duration)
    }
}
