package app.atemkraft.ui.session

import app.atemkraft.data.BuiltInExercises
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SessionTiming
import app.atemkraft.playback.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Zeitablauf der Atem-Session in virtueller Zeit (kotlinx-coroutines-test, ohne Robolectric): Die
 * Uhr ist der Test-Scheduler, deshalb stimmen Dauern auf die Millisekunde.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTimingTest {

    private val box = BuiltInExercises.all.first { it.id == "box-4-4-4-4" } // 8 Runden à 16 s

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Drift und Pause über dieselbe Box-Session: Ende auf die Millisekunde, die Pause zählt nicht. */
    @Test
    fun `Countdown, Pause und Ende genau nach der Sollzeit`() = runTest {
        val vm = SessionViewModel(now = { testScheduler.currentTime })
        vm.start(box)
        runCurrent()
        assertEquals(SessionStatus.PREPARING, vm.state.value.status)
        assertEquals(SessionTiming.COUNTDOWN_SECONDS, vm.state.value.countdown)

        advanceTimeBy(COUNTDOWN_MS + 40_000L)
        runCurrent()
        assertEquals(SessionStatus.RUNNING, vm.state.value.status)
        vm.pause()
        val paused = vm.state.value.progress
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(SessionStatus.PAUSED, vm.state.value.status)
        assertEquals("Bogen steht in der Pause", paused, vm.state.value.progress, 0f)

        vm.resume()
        advanceTimeBy(BOX_MS - 40_000L - 1)
        runCurrent()
        assertEquals(SessionStatus.RUNNING, vm.state.value.status)
        advanceTimeBy(1)
        runCurrent()
        val end = vm.state.value
        assertEquals(SessionStatus.FINISHED, end.status)
        assertEquals(BOX_MS, requireNotNull(end.summary).durationMs)
    }

    /** Von vorne nimmt dasselbe Muster samt Anpassung, ohne es per Id neu nachzuschlagen. */
    @Test
    fun `Von vorne setzt Countdown und aktive Zeit zurück`() = runTest {
        val vm = SessionViewModel(now = { testScheduler.currentTime })
        // Ein Muster, das das Repository nicht kennt (wie ein Tagesmuster von gestern), mit 2 Runden.
        vm.start(box.copy(id = "nicht-im-repository", name = "Eigenes Muster"), SessionConfig(rounds = 2))
        advanceTimeBy(COUNTDOWN_MS + 20_000L)
        runCurrent()
        vm.restart()
        runCurrent()
        val restarted = vm.state.value
        assertEquals(SessionStatus.PREPARING, restarted.status)
        assertEquals("Eigenes Muster", restarted.exerciseName)
        assertEquals(2, restarted.roundCount)
        assertEquals(0f, restarted.progress, 0f)

        advanceTimeBy(COUNTDOWN_MS + 2 * 16_000L)
        runCurrent()
        assertEquals(SessionStatus.FINISHED, vm.state.value.status)
        assertEquals(2 * 16_000L, requireNotNull(vm.state.value.summary).durationMs)
    }

    private companion object {
        const val COUNTDOWN_MS = SessionTiming.COUNTDOWN_SECONDS * SessionTiming.COUNTDOWN_STEP_MS
        const val BOX_MS = 8 * 16_000L
    }
}
