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
import app.atemkraft.data.SessionDisplay
import app.atemkraft.domain.PhaseType
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestSessionOverlay
import app.atemkraft.ui.components.AUTO_CALM_DELAY_MS
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
 * im Countdown auf dem Kreis und mit TalkBack (Touch-Erkundung) bleibt alles sichtbar. Mit
 * automatischer Ruhe blendet die laufende Übung nach [AUTO_CALM_DELAY_MS] ohne Berührung aus.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class CalmViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val events = mutableListOf<String>()

    private fun show(status: SessionStatus, display: SessionDisplay = SessionDisplay(autoCalm = false)) {
        compose.setContent {
            AtemkraftTheme {
                TestSessionOverlay(
                    state = SessionUiState(
                        exerciseName = "Test",
                        status = status,
                        roundIndex = 2,
                        roundCount = 8,
                        phaseType = PhaseType.INHALE,
                        phaseTotalMs = 4_000L,
                        remainingMs = 2_400L,
                    ),
                    display = display,
                    onTogglePause = { events += "pause" },
                    onContinue = { events += "weiter" },
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
        // auf „Gleich geht’s los“, das über dem Kreis liegt und den Tipp durchlässt.
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

    @Test
    fun `Automatische Ruhe blendet nach der Wartezeit aus, ein Tipp holt alles zurück`() {
        show(SessionStatus.RUNNING, SessionDisplay(autoCalm = true))
        compose.mainClock.advanceTimeBy(AUTO_CALM_DELAY_MS - 1_000L)
        stop.assertExists()
        compose.mainClock.advanceTimeBy(2_000L)
        stop.assertDoesNotExist()
        tapBeside()
        stop.assertExists()
        // Der Tipp startet die Wartezeit neu.
        compose.mainClock.advanceTimeBy(AUTO_CALM_DELAY_MS - 1_000L)
        stop.assertExists()
        compose.mainClock.advanceTimeBy(2_000L)
        stop.assertDoesNotExist()
    }

    @Test
    fun `Automatische Ruhe greift nicht im Countdown`() {
        show(SessionStatus.PREPARING, SessionDisplay(autoCalm = true))
        compose.mainClock.advanceTimeBy(AUTO_CALM_DELAY_MS * 2)
        stop.assertExists()
    }

    @Test
    fun `Mit Sekunden zählt der Kreis die Phase mit`() {
        show(SessionStatus.RUNNING)
        compose.onNodeWithText("3").assertExists()
    }

    @Test
    fun `Ohne Sekunden steht nur das Phasenwort im Kreis`() {
        show(SessionStatus.RUNNING, SessionDisplay(showSeconds = false, autoCalm = false))
        compose.onNodeWithText("3").assertDoesNotExist()
    }
}
