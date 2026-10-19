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
import androidx.compose.ui.test.performSemanticsAction
import app.atemkraft.R
import app.atemkraft.domain.PhaseType
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MeditationStatus
import app.atemkraft.ui.meditation.MeditationUiState
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
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
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
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
            SessionOverlay(
                state = SessionUiState(
                    exerciseName = "Box-Atmung",
                    status = status,
                    phaseType = PhaseType.INHALE,
                    phaseTotalMs = 4_000L,
                    remainingMs = 2_000L,
                    muted = muted,
                ),
                showNextPhase = false,
                onMinimize = { events += "minimieren" },
                onToggleMute = { events += "ton" },
                onTogglePause = {},
                onTapHaptic = {},
                onContinue = {},
                onRestart = {},
                onStop = { events += "ende" },
            )
        }
    }

    private fun meditation(status: MeditationStatus) {
        show {
            MeditationOverlay(
                state = MeditationUiState(status = status, remainingMs = 60_000L, elapsedMs = 10 * 60_000L),
                onMinimize = { events += "minimieren" },
                onToggleMute = {},
                onTogglePause = {},
                onTapHaptic = {},
                onRestart = {},
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

    @Test
    fun `Pause wird am Phasenwort angesagt, ohne zweite Live-Region`() {
        session(SessionStatus.PAUSED)
        val live = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals(1, live.size)
        val said = live.single().config[SemanticsProperties.ContentDescription].single()
        assertTrue("Ansage „$said“", said.startsWith(string(R.string.session_paused) + ", "))
    }

    @Test
    fun `laufend sagt das Phasenwort nur die Phase`() {
        session(SessionStatus.RUNNING)
        val said = compose
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertTrue("Ansage „$said“", !said.contains(string(R.string.session_paused)))
    }

    @Test
    fun `Atemkreis sagt, was der Doppeltipp tut, und ist im Countdown kein Halt`() {
        session(SessionStatus.RUNNING)
        assertEquals(string(R.string.circle_action_pause), clickLabel(R.string.cd_breathing_circle))
        session(SessionStatus.PAUSED)
        assertEquals(string(R.string.circle_action_resume), clickLabel(R.string.cd_breathing_circle))
        session(SessionStatus.WAITING_FOR_USER)
        assertEquals(string(R.string.circle_action_continue), clickLabel(R.string.cd_breathing_circle))
        session(SessionStatus.PREPARING)
        compose.onNodeWithContentDescription(string(R.string.cd_breathing_circle)).assertDoesNotExist()
    }

    @Test
    fun `Ton ist ein Schalter mit festem Namen und Zustand`() {
        session(SessionStatus.RUNNING)
        compose.onNodeWithContentDescription(string(R.string.action_sound))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_switch_on)))
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("ton"), events)
        session(SessionStatus.RUNNING, muted = true)
        compose.onNodeWithContentDescription(string(R.string.action_sound))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.state_switch_off)))
    }

    @Test
    fun `Overlay und Ende-Screen sind eigene Bereiche`() {
        session(SessionStatus.RUNNING)
        assertEquals(listOf("Box-Atmung"), paneTitles())
        session(SessionStatus.FINISHED)
        assertEquals(listOf("Box-Atmung", string(R.string.session_done_title)), paneTitles())
        meditation(MeditationStatus.FINISHED)
        assertEquals(listOf(string(R.string.meditation_title), string(R.string.session_done_title)), paneTitles())
    }

    @Test
    fun `Zurück minimiert die laufende Session und beendet sie am Ende-Screen`() {
        session(SessionStatus.RUNNING)
        back()
        session(SessionStatus.FINISHED)
        back()
        assertEquals(listOf("minimieren", "ende"), events)
    }

    @Test
    fun `Zurück minimiert die laufende Meditation und beendet sie am Ende-Screen`() {
        meditation(MeditationStatus.RUNNING)
        back()
        meditation(MeditationStatus.FINISHED)
        back()
        assertEquals(listOf("minimieren", "ende"), events)
    }

    @Test
    fun `Meditation sagt die Pause als Zustand des sichtbaren Titels an`() {
        meditation(MeditationStatus.PAUSED)
        val title = compose.onNodeWithText(string(R.string.meditation_title), useUnmergedTree = true).fetchSemanticsNode().config
        assertEquals(string(R.string.session_paused), title.getOrNull(SemanticsProperties.StateDescription))
        assertNull("sichtbarer Name bleibt der gesprochene", title.getOrNull(SemanticsProperties.ContentDescription))
        assertEquals(string(R.string.circle_action_resume), clickLabel(R.string.cd_meditation_ring))
        meditation(MeditationStatus.RUNNING)
        val running = compose.onNodeWithText(string(R.string.meditation_title), useUnmergedTree = true).fetchSemanticsNode().config
        assertNull(running.getOrNull(SemanticsProperties.StateDescription))
        assertEquals(string(R.string.circle_action_pause), clickLabel(R.string.cd_meditation_ring))
    }

    @Test
    fun `Meditation fragt am Ende wie die Atem-Session nach dem Befinden`() {
        meditation(MeditationStatus.FINISHED)
        compose.onNodeWithText(string(R.string.session_done_question)).assertExists()
        compose.onNodeWithText("10:00", substring = true).assertExists()
    }
}
