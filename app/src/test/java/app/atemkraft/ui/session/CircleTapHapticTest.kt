package app.atemkraft.ui.session

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MeditationStatus
import app.atemkraft.ui.meditation.MeditationUiState
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
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class CircleTapHapticTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val events = mutableListOf<String>()

    @Test
    fun `Tippen auf den Atemkreis meldet den Tipp-Tick und pausiert`() {
        compose.setContent {
            AtemkraftTheme {
                SessionOverlay(
                    state = SessionUiState(exerciseName = "Test", status = SessionStatus.RUNNING),
                    showNextPhase = false,
                    onMinimize = {},
                    onToggleMute = {},
                    onTogglePause = { events += "pause" },
                    onTapHaptic = { events += "tick" },
                    onContinue = {},
                    onRestart = {},
                    onStop = {},
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
                MeditationOverlay(
                    state = MeditationUiState(status = MeditationStatus.RUNNING),
                    onMinimize = {},
                    onToggleMute = {},
                    onTogglePause = { events += "pause" },
                    onTapHaptic = { events += "tick" },
                    onRestart = {},
                    onEnd = {},
                )
            }
        }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_meditation_ring)).performClick()
        assertEquals(listOf("tick", "pause"), events)
    }
}
