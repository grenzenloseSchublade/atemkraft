package app.atemkraft.ui.session

import app.atemkraft.data.BuiltInExercises
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

    @Test
    fun `Countdown, dann 32 feste Phasen ohne Drift - Ende genau nach der Sollzeit`() = runTest {
        val vm = SessionViewModel(now = { testScheduler.currentTime })
        vm.start(box)
        runCurrent()
        assertEquals(SessionStatus.PREPARING, vm.state.value.status)
        assertEquals(SessionTiming.COUNTDOWN_SECONDS, vm.state.value.countdown)

        advanceTimeBy(COUNTDOWN_MS)
        runCurrent()
        assertEquals(SessionStatus.RUNNING, vm.state.value.status)

        advanceTimeBy(BOX_MS - 1)
        runCurrent()
        assertEquals(SessionStatus.RUNNING, vm.state.value.status)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(SessionStatus.FINISHED, vm.state.value.status)
        assertEquals(BOX_MS, requireNotNull(vm.state.value.summary).durationMs)
    }

    @Test
    fun `eine Pause verlängert die Sitzung, zählt aber nicht zur aktiven Zeit`() = runTest {
        val vm = SessionViewModel(now = { testScheduler.currentTime })
        vm.start(box)
        advanceTimeBy(COUNTDOWN_MS + 40_000L)
        runCurrent()
        vm.pause()
        assertEquals(SessionStatus.PAUSED, vm.state.value.status)
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(SessionStatus.PAUSED, vm.state.value.status)

        vm.resume()
        advanceTimeBy(BOX_MS)
        runCurrent()
        val end = vm.state.value
        assertEquals(SessionStatus.FINISHED, end.status)
        assertEquals(BOX_MS, requireNotNull(end.summary).durationMs)
    }

    @Test
    fun `Von vorne setzt Countdown und aktive Zeit zurück`() = runTest {
        val vm = SessionViewModel(now = { testScheduler.currentTime })
        vm.start(box)
        advanceTimeBy(COUNTDOWN_MS + 20_000L)
        runCurrent()
        vm.restart()
        runCurrent()
        assertEquals(SessionStatus.PREPARING, vm.state.value.status)
        assertEquals(0f, vm.state.value.progress, 0f)

        advanceTimeBy(COUNTDOWN_MS + BOX_MS)
        runCurrent()
        assertEquals(SessionStatus.FINISHED, vm.state.value.status)
        assertEquals(BOX_MS, requireNotNull(vm.state.value.summary).durationMs)
    }

    private companion object {
        const val COUNTDOWN_MS = SessionTiming.COUNTDOWN_SECONDS * SessionTiming.COUNTDOWN_STEP_MS
        const val BOX_MS = 8 * 16_000L
    }
}
