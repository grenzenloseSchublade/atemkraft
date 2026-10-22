package app.atemkraft.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Einstieg in den Muster-Editor auf der Detailseite: Der Stift sitzt nur bei generierten Mustern
 * im Kopf (Wortlaut nach Herkunft), eingebaute Übungen behalten ihren Slot. Generierte Muster
 * bieten nur die Dauer an, ihre Phasen ändert man im Editor (Leitplanken, Nutzer 2026-10-22).
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

    @Test
    fun `Stift beim Muster des Tages ist ein Button mit voller Tippflaeche und ruft den Editor`() {
        show { Detail(daily) }
        val pen = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily))
        pen.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        // Klick-Semantik im sichtbaren Kreis, die 48 dp trägt der Layout-Knoten drumherum.
        val node = pen.fetchSemanticsNode()
        val size = Offset(node.layoutInfo.width.toFloat(), node.layoutInfo.height.toFloat())
        val px48 = 48 * compose.density.density
        assertTrue("Tippfläche ${size.x} × ${size.y} px", size.x >= px48 - 0.5f && size.y >= px48 - 0.5f)

        pen.performClick()
        assertEquals(listOf("bearbeiten"), events)
    }

    @Test
    fun `Gespeichertes Muster heisst Muster bearbeiten, eigener Wortlaut geht vor`() {
        var description by mutableStateOf<String?>(null)
        show {
            ExerciseDetailScreen(
                exercise = saved,
                requireSafetyConfirm = false,
                savedIntervals = null,
                onIntervalsChange = { _, _, _, _ -> },
                onIntervalsReset = {},
                onConfirmedSafety = {},
                onStart = {},
                onBack = {},
                onEditPattern = { events += "bearbeiten" },
                editDescription = description,
            )
        }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_edit)).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily)).assertDoesNotExist()

        description = app.getString(R.string.cd_pattern_adapt_daily)
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily)).assertExists()
        assertEquals(listOf("bearbeiten"), events)
    }

    @Test
    fun `Eingebaute Uebung zeigt keinen Stift, die Markierung bleibt im Slot`() {
        show { Detail(builtIn, onMarkedChange = {}) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_edit)).assertDoesNotExist()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily)).assertDoesNotExist()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_personal_mark)).assertExists()
    }

    @Test
    fun `Ohne Callback kein Stift, auch nicht bei generierten Mustern`() {
        show { Detail(daily, onEditPattern = null) }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_adapt_daily)).assertDoesNotExist()
    }

    @Test
    fun `Generierte Muster bieten nur die Dauer an, keine Phasen-Stepper`() {
        show { Detail(saved) }
        compose.onNodeWithText(app.getString(R.string.adjust_minutes)).assertExists()
        compose.onNodeWithText(app.getString(R.string.adjust_intervals)).assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertDoesNotExist()
    }

    @Test
    fun `Eingebaute Uebungen behalten Phasen anpassen`() {
        show { Detail(builtIn) }
        compose.onNodeWithText(app.getString(R.string.adjust_intervals)).performClick()
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertExists()
        compose.onNodeWithText(app.getString(R.string.adjust_exhale)).assertExists()
    }

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
