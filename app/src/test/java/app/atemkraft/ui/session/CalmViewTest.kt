package app.atemkraft.ui.session

import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Ruhe-Ansicht der Atem-Session ([SessionOverlay]): Ein Tipp neben den Kreis blendet die
 * Bedienelemente aus und wieder ein; der Tipp auf den Kreis bleibt Pause/Weiter. In der Pause,
 * im Countdown auf dem Kreis und mit TalkBack (Touch-Erkundung) bleibt alles sichtbar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class CalmViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val events = mutableListOf<String>()

    private fun show(status: SessionStatus) {
        compose.setContent {
            AtemkraftTheme {
                SessionOverlay(
                    state = SessionUiState(exerciseName = "Test", status = status, roundIndex = 2, roundCount = 8),
                    showNextPhase = true,
                    onMinimize = {},
                    onToggleMute = {},
                    onTogglePause = { events += "pause" },
                    onTapHaptic = {},
                    onContinue = { events += "weiter" },
                    onRestart = {},
                    onStop = {},
                )
            }
        }
    }

    private val circle get() = compose.onNodeWithContentDescription(app.getString(R.string.cd_breathing_circle))
    private val stop get() = compose.onNodeWithContentDescription(app.getString(R.string.action_stop))

    /** Ecke des Quadrats um den Kreis: außerhalb der Kreisform, also „daneben“. */
    private fun tapBeside() = circle.performTouchInput { click(Offset(1f, 1f)) }

    @Test
    fun `Tipp neben den Kreis blendet aus, ein zweiter wieder ein`() {
        show(SessionStatus.RUNNING)
        tapBeside()
        stop.assertDoesNotExist()
        tapBeside()
        stop.assertExists()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `Tipp auf den Kreis pausiert und blendet nichts aus`() {
        show(SessionStatus.RUNNING)
        circle.performTouchInput { click(center) }
        stop.assertExists()
        assertEquals(listOf("pause"), events)
    }

    @Test
    fun `In der Pause bleibt alles sichtbar`() {
        show(SessionStatus.PAUSED)
        tapBeside()
        stop.assertExists()
    }

    @Test
    fun `Im Countdown bewirkt der Tipp auf den Kreis nichts`() {
        show(SessionStatus.PREPARING)
        // Im Countdown hat der Kreis keinen Namen (kein leerer TalkBack-Halt): getippt wird mitten
        // auf „Gleich geht's los“, das über dem Kreis liegt und den Tipp durchlässt.
        circle.assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.session_get_ready)).performTouchInput { click(center) }
        stop.assertExists()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `Mit Touch-Erkundung wird nie ausgeblendet`() {
        val manager = app.getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(true)
        show(SessionStatus.RUNNING)
        tapBeside()
        stop.assertExists()
    }

    @Test
    fun `Offener Hold – Kreis-Tipp atmet weiter`() {
        show(SessionStatus.WAITING_FOR_USER)
        circle.performClick()
        assertEquals(listOf("weiter"), events)
    }
}
