package app.atemkraft.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Einstieg in den Muster-Editor auf der Detailseite: Der Stift sitzt nur bei generierten Mustern
 * im Kopf, eingebaute Übungen behalten ihren Slot. Gespeicherte und
 * eigene Muster bieten nur die Dauer an, ihre Phasen ändert man im Editor (Nutzer 2026-10-22);
 * das Muster des Tages behält seine Phasen-Stepper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class ExerciseDetailEditActionTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val daily = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 3)).exercise
    private val saved = RandomPatternGenerator.exerciseFrom(PatternSpec(inhale = 4.0, holdFull = 1.0, exhale = 6.0, holdEmpty = null, activating = false), "muster-7", "Eigenes Muster 4·1·6")
    private val builtIn = BuiltInExercises.all.first { it.id == "box-4-4-4-4" }
    private val events = mutableListOf<String>()

    @Composable
    private fun Detail(
        exercise: Exercise,
        savedIntervals: IntervalOverrides? = null,
        onStart: (SessionConfig) -> Unit = {},
        onMarkedChange: ((Boolean) -> Unit)? = null,
        onEditPattern: (() -> Unit)? = { events += "bearbeiten" },
    ) = ExerciseDetailScreen(
        exercise = exercise,
        requireSafetyConfirm = false,
        savedIntervals = savedIntervals,
        onIntervalsChange = { _, _, _, _ -> },
        onIntervalsReset = {},
        onConfirmedSafety = {},
        onStart = onStart,
        onBack = {},
        onMarkedChange = onMarkedChange,
        onEditPattern = onEditPattern,
    )

    private fun show(content: @Composable () -> Unit) = compose.setContent { AtemkraftTheme { content() } }

    /**
     * Angebot je Herkunft (Nutzer 2026-10-22): Stift nur bei generierten Mustern; Phasen-Stepper bei
     * eingebauten Übungen (Markierung bleibt im Slot) und beim Muster des Tages wie in v1.6;
     * gespeicherte und eigene Muster nur die Dauer – ihre Phasen ändert man im Editor.
     */
    @Test
    fun `Angebot je Herkunft - Stift nur bei generierten Mustern, Phasen-Stepper nur bei eingebauten und Tagesmuster`() {
        var exercise by mutableStateOf(builtIn)
        show { Detail(exercise, onMarkedChange = if (exercise == builtIn) ({}) else null) }

        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_edit)).assertDoesNotExist()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily)).assertDoesNotExist()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_personal_mark)).assertExists()
        compose.onNodeWithText(app.getString(R.string.adjust_intervals)).performClick()
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertExists()

        exercise = daily
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily)).performClick()
        assertEquals(listOf("bearbeiten"), events)
        compose.onNodeWithText(app.getString(R.string.adjust_intervals)).performClick()
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertExists()

        exercise = saved
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_edit)).assertExists()
        compose.onNodeWithText(app.getString(R.string.adjust_minutes)).assertExists()
        compose.onNodeWithText(app.getString(R.string.adjust_intervals)).assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertDoesNotExist()
    }

    /** Persistierte Overrides aus v1.5 dürfen nach einem Update keine Phasen mehr erzwingen. */
    @Test
    fun `Alte Phasen-Anpassung eines generierten Musters gilt nicht mehr, die Dauer schon`() {
        var config: SessionConfig? = null
        show {
            Detail(
                saved,
                savedIntervals = IntervalOverrides(duration = 7, inhale = 10, hold = 15, exhale = 12),
                onStart = { config = it },
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText(app.getString(R.string.action_start), substring = true).performClick()
        assertEquals(SessionConfig(minutes = 7), config)
    }
}
