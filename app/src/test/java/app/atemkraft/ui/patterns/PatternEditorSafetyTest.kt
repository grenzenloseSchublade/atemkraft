package app.atemkraft.ui.patterns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.SafetySettings
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.ui.SilentPreviewSound
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.detail.DetailActions
import app.atemkraft.ui.detail.ExerciseDetailScreen
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
 * MUSTER-06 an beiden Startpfaden mit Sicherheitsdialog: Eine intensive freie Folge im
 * Muster-Editor und eine intensive eingebaute Übung (Wim Hof) auf der Detailseite starten erst
 * nach dem Dialog. Die Bestätigung wirkt wie in der App (`acknowledgeSafety` merkt sich nur die
 * Übung): Mit „Sicherheitshinweis immer zeigen“ (Standard an) kommt der Dialog jedes Mal, erst
 * ausgeschaltet startet die bestätigte Übung direkt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class PatternEditorSafetyTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `intensives Muster ist caution und startet erst nach dem Sicherheitsdialog`() {
        val app = RuntimeEnvironment.getApplication()
        val started = mutableListOf<Exercise>()
        var safety by mutableStateOf(SafetySettings())
        val state = PatternEditorState.new(app.getString(R.string.pattern_editor_title), FreePatternTemplates.all.single { it.label == "4-7-8" }.pattern)
        compose.setContent {
            AtemkraftTheme {
                PatternEditorScreen(
                    state = state,
                    actions = PatternEditorActions(
                        onSavedChange = {},
                        onStart = { exercise, _ -> started += exercise },
                        onOpenLore = {},
                        onBack = {},
                        onConfirmedSafety = { id -> safety = safety.copy(acknowledgedIds = safety.acknowledgedIds + id) },
                    ),
                    safety = safety,
                    soundOutput = { SilentPreviewSound },
                )
            }
        }
        val play = compose.onNodeWithText(app.getString(R.string.action_start), substring = true)

        // Halten voll 12 s (über 10 s) macht das Muster intensiv.
        compose.runOnIdle { state.setSeconds(1, 12) }
        compose.onNodeWithText(app.getString(R.string.detail_intense)).assertExists()
        play.performClick()
        assertTrue(started.isEmpty())
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).performClick()
        assertTrue(started.single().caution)

        // Mit „Sicherheitshinweis immer zeigen“ (Standard an) kommt der Dialog wieder.
        play.performClick()
        assertEquals(1, started.size)
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).performClick()
        assertEquals(2, started.size)

        // Erst ausgeschaltet startet das bestätigte Muster direkt.
        compose.runOnIdle { safety = safety.copy(showWarning = false) }
        play.performClick()
        assertEquals(3, started.size)
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).assertDoesNotExist()
    }

    /** MUSTER-06 auf der Detailseite: Wim Hof startet erst nach dem Dialog, ohne „immer zeigen“ danach direkt. */
    @Test
    fun `Wim Hof auf der Detailseite startet erst nach dem Sicherheitsdialog`() {
        val app = RuntimeEnvironment.getApplication()
        val wimHof = BuiltInExercises.all.single { it.id == "wim-hof" }
        var starts = 0
        var safety by mutableStateOf(SafetySettings())
        compose.setContent {
            AtemkraftTheme {
                ExerciseDetailScreen(
                    exercise = wimHof,
                    requireSafetyConfirm = safety.requiresConfirm(wimHof),
                    savedIntervals = null,
                    actions = DetailActions(
                        onIntervalsChange = { _, _, _, _ -> },
                        onIntervalsReset = {},
                        onConfirmedSafety = { safety = safety.copy(acknowledgedIds = safety.acknowledgedIds + wimHof.id) },
                        onStart = { starts++ },
                        onBack = {},
                    ),
                )
            }
        }
        val start = compose.onNode(hasText(app.getString(R.string.action_start), substring = true) and hasClickAction())

        start.performClick()
        assertEquals(0, starts)
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).performClick()
        assertEquals(1, starts)

        // Mit „Sicherheitshinweis immer zeigen“ (Standard an) kommt der Dialog wieder.
        start.performClick()
        assertEquals(1, starts)
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).performClick()
        assertEquals(2, starts)

        // Erst ausgeschaltet startet die bestätigte Übung direkt.
        compose.runOnIdle { safety = safety.copy(showWarning = false) }
        start.performClick()
        assertEquals(3, starts)
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).assertDoesNotExist()
    }

    @Test
    fun `Bestätigung gilt nur für die bestätigte Übung`() {
        val wimHof = BuiltInExercises.all.single { it.id == "wim-hof" }
        val fire = BuiltInExercises.all.single { it.id == "feueratmung" }
        val safety = SafetySettings(showWarning = false, acknowledgedIds = setOf(wimHof.id))
        assertFalse(safety.requiresConfirm(wimHof))
        assertTrue(safety.requiresConfirm(fire))
    }

    @Test
    fun `Vorlage verliert keine geformte Kurve - Zurücksetzen führt erst davor, dann zum Öffnungsstand`() {
        val opened = FreePatternTemplates.all.single { it.label == "4-7-8" }.pattern
        val state = PatternEditorState.new("Eigenes Muster", opened)
        state.setSeconds(0, 6)
        val shaped = state.pattern
        state.applyTemplate(FreePatternTemplates.all.single { it.label == "Box" })
        state.applyTemplate(FreePatternTemplates.all.single { it.label == "Viloma I" })
        state.reset()
        assertEquals(shaped, state.pattern)
        assertTrue(state.modified)
        state.reset()
        assertEquals(opened, state.pattern)
        assertFalse(state.modified)
    }
}
