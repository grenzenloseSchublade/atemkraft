package app.atemkraft.ui.meditation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.atemkraft.R
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMeditationOverlay
import app.atemkraft.ui.components.AUTO_CALM_DELAY_MS
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Ruhe-Ansicht der Meditation ([MeditationOverlay]) wie in der Atem-Session: Tipp neben den Kreis
 * blendet aus und ein, automatisch nach [AUTO_CALM_DELAY_MS], nicht in der Pause; ohne Zeit steht
 * nichts im Kreis.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class MeditationCalmViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    private val events = mutableListOf<String>()
    private var status by mutableStateOf(MeditationStatus.RUNNING)
    private var display by mutableStateOf(MeditationDisplay(autoCalm = true))

    private val ring get() = compose.onNodeWithContentDescription(app.getString(R.string.cd_meditation_ring))
    private val stop get() = compose.onNodeWithContentDescription(app.getString(R.string.action_stop))

    /** Ecke des Quadrats um den Kreis: außerhalb der Kreisform, also „daneben“. */
    private fun tapBeside() = ring.performTouchInput { click(Offset(1f, 1f)) }

    /**
     * Die Ruhe-Logik steckt im gemeinsamen CalmHost (geprüft in `CalmViewTest`); hier nur die
     * Verdrahtung der Meditation: Ausblenden, Ring-Tipp mit Tick und Pause, nie in der Pause, Zeit-Schalter.
     */
    @Test
    fun `Meditation nutzt die Ruhe-Ansicht - daneben blendet aus, Ring-Tipp meldet Tick und pausiert, Pause blendet nie aus`() {
        compose.setContent {
            AtemkraftTheme {
                TestMeditationOverlay(
                    state = MeditationUiState(status = status, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
                    display = display,
                    onTogglePause = { events += "pause" },
                    onTapHaptic = { events += "tick" },
                )
            }
        }
        tapBeside()
        stop.assertDoesNotExist()
        tapBeside()
        stop.assertExists()

        ring.performClick()
        assertEquals(listOf("tick", "pause"), events)

        status = MeditationStatus.PAUSED
        tapBeside()
        compose.mainClock.advanceTimeBy(AUTO_CALM_DELAY_MS * 2)
        stop.assertExists()

        // Ohne Zeit steht nichts im Kreis.
        compose.onNodeWithText("7:12").assertExists()
        display = MeditationDisplay(autoCalm = false, showTime = false)
        compose.onNodeWithText("7:12").assertDoesNotExist()
    }
}
