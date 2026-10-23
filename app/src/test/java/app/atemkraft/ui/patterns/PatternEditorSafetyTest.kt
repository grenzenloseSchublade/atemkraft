package app.atemkraft.ui.patterns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
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

/** MUSTER-06 im Muster-Editor: Eine intensive freie Folge startet erst nach dem Sicherheitsdialog. */
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
                    onSavedChange = {},
                    onStart = { exercise, _ -> started += exercise },
                    onOpenLore = {},
                    onBack = {},
                    safety = safety,
                    onConfirmedSafety = { id -> safety = SafetySettings(showWarning = false, acknowledgedIds = setOf(id)) },
                    soundOutput = { SilentPreviewSound },
                )
            }
        }
        val play = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_play).substringBefore(","), substring = true)

        // Halten voll 12 s (über 10 s) macht das Muster intensiv.
        compose.runOnIdle { state.setSeconds(1, 12) }
        compose.onNodeWithText(app.getString(R.string.detail_intense)).assertExists()
        play.performClick()
        assertTrue(started.isEmpty())
        compose.onNodeWithText(app.getString(R.string.safety_confirm)).performClick()
        assertTrue(started.single().caution)

        // Einmal bestätigt, startet es direkt.
        play.performClick()
        assertEquals(2, started.size)
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
}
