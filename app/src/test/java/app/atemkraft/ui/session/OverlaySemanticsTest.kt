package app.atemkraft.ui.session

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import app.atemkraft.R
import app.atemkraft.domain.PhaseType
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMeditationOverlay
import app.atemkraft.ui.TestSessionOverlay
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Was TalkBack und Tastatur in den Vollbild-Sessions vorfinden (A11Y-01, -04, -07, MUSTER-07):
 * Overlay und Ende-Screen als eigene Bereiche, Pause angesagt, Kreis mit Aktion, Ton als
 * Schalter, System-Zurück am Ende = „Ende“.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class OverlaySemanticsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()
    private var content by mutableStateOf<(@Composable () -> Unit)?>(null)

    @Before
    fun setUp() {
        compose.setContent {
            AtemkraftTheme { content?.invoke() }
        }
    }

    private fun show(next: @Composable () -> Unit) {
        // Erst leeren: Ein neuer Zustand derselben Ansicht beginnt so wie frisch geöffnet.
        content = null
        compose.waitForIdle()
        content = next
        compose.waitForIdle()
    }

    private fun string(id: Int) = compose.activity.getString(id)

    private fun session(status: SessionStatus, muted: Boolean = false) {
        show {
            TestSessionOverlay(
                state = SessionUiState(
                    exerciseName = "Box-Atmung",
                    status = status,
                    phaseType = PhaseType.INHALE,
                    phaseTotalMs = 4_000L,
                    remainingMs = 2_000L,
                    muted = muted,
                ),
                onMinimize = { events += "minimieren" },
                onToggleMute = { events += "ton" },
                onStop = { events += "ende" },
            )
        }
    }

    private fun meditation(status: MeditationStatus) {
        show {
            TestMeditationOverlay(
                state = MeditationUiState(status = status, remainingMs = 60_000L, elapsedMs = 10 * 60_000L),
                onMinimize = { events += "minimieren" },
                onEnd = { events += "ende" },
            )
        }
    }

    private fun back() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun paneTitles(): List<String> = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { it.config[SemanticsProperties.PaneTitle] }

    private fun clickLabel(description: Int): String? = compose
        .onNodeWithContentDescription(string(description))
        .fetchSemanticsNode()
        .config.getOrNull(SemanticsActions.OnClick)?.label

    /**
     * Atem-Session: eigener Bereich (auch der Ende-Screen), genau eine Live-Region am Phasenwort –
     * pausiert mit „Pausiert“ davor –, der Kreis mit Klick-Aktion (im Countdown kein Halt) und der
     * Ton als Schalter mit Zustand.
     */
    @Test
    fun `Atem-Session fuer TalkBack`() {
        session(SessionStatus.RUNNING)
        assertEquals(listOf("Box-Atmung"), paneTitles())
        val running = compose
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertTrue("Ansage „$running“", !running.contains(string(R.string.session_paused)))
        assertNotNull(clickLabel(R.string.cd_breathing_circle))
        compose.onNodeWithContentDescription(string(R.string.action_sound))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        session(SessionStatus.RUNNING, muted = true)
        compose.onNodeWithContentDescription(string(R.string.action_sound))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))

        session(SessionStatus.PAUSED)
        val live = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals(1, live.size)
        val said = live.single().config[SemanticsProperties.ContentDescription].single()
        assertTrue("Ansage „$said“", said.startsWith(string(R.string.session_paused) + ", "))

        session(SessionStatus.PREPARING)
        compose.onNodeWithContentDescription(string(R.string.cd_breathing_circle)).assertDoesNotExist()

        session(SessionStatus.FINISHED)
        assertEquals(listOf("Box-Atmung", string(R.string.session_done_title)), paneTitles())
    }

    /** Meditation: eigene Bereiche, Pause als Zustand des sichtbaren Titels, Ring mit Klick-Aktion. */
    @Test
    fun `Meditation fuer TalkBack`() {
        meditation(MeditationStatus.PAUSED)
        val title = compose.onNodeWithText(string(R.string.meditation_title), useUnmergedTree = true).fetchSemanticsNode().config
        assertEquals(string(R.string.session_paused), title.getOrNull(SemanticsProperties.StateDescription))
        assertNull("sichtbarer Name bleibt der gesprochene", title.getOrNull(SemanticsProperties.ContentDescription))
        assertEquals(string(R.string.circle_action_resume), clickLabel(R.string.cd_meditation_ring))
        meditation(MeditationStatus.RUNNING)
        val running = compose.onNodeWithText(string(R.string.meditation_title), useUnmergedTree = true).fetchSemanticsNode().config
        assertNull(running.getOrNull(SemanticsProperties.StateDescription))

        meditation(MeditationStatus.FINISHED)
        assertEquals(listOf(string(R.string.meditation_title), string(R.string.session_done_title)), paneTitles())
    }

    /** Navigations-Grundregel (MUSTER-07): Zurück minimiert laufend und beendet am Ende-Screen. */
    @Test
    fun `Zurueck minimiert laufend und beendet am Ende-Screen - Session und Meditation`() {
        session(SessionStatus.RUNNING)
        back()
        session(SessionStatus.FINISHED)
        back()
        meditation(MeditationStatus.RUNNING)
        back()
        meditation(MeditationStatus.FINISHED)
        back()
        assertEquals(listOf("minimieren", "ende", "minimieren", "ende"), events)
    }
}
