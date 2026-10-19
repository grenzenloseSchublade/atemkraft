package app.atemkraft.ui.session

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMeditationOverlay
import app.atemkraft.ui.TestSessionOverlay
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * AUDIO-04, -05: Der Kreis-Tap vibriert nicht selbst, sondern ruft `onTapHaptic` – dort
 * entscheidet der Aufrufer anhand des App-Schalters „Vibration“. Ein eigener Player in der
 * Composition würde die Einstellung umgehen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class CircleTapHapticTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val events = mutableListOf<String>()

    @Test
    fun `Tippen auf den Atemkreis meldet den Tipp-Tick und pausiert`() {
        compose.setContent {
            AtemkraftTheme {
                TestSessionOverlay(
                    state = SessionUiState(exerciseName = "Test", status = SessionStatus.RUNNING),
                    onTogglePause = { events += "pause" },
                    onTapHaptic = { events += "tick" },
                )
            }
        }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_breathing_circle)).performClick()
        assertEquals(listOf("tick", "pause"), events)
    }

    @Test
    fun `Tippen auf den Meditations-Ring meldet den Tipp-Tick und pausiert`() {
        compose.setContent {
            AtemkraftTheme {
                TestMeditationOverlay(
                    state = MeditationUiState(status = MeditationStatus.RUNNING),
                    onTogglePause = { events += "pause" },
                    onTapHaptic = { events += "tick" },
                )
            }
        }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_meditation_ring)).performClick()
        assertEquals(listOf("tick", "pause"), events)
    }
}
