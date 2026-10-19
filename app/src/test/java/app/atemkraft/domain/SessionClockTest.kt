package app.atemkraft.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Gemeinsame Uhr von Atem-Session und Meditation: Countdown in virtueller Zeit, Pausen zählen nicht. */
class SessionClockTest {

    @Test
    fun `Countdown meldet 3, 2, 1 je Sekunde und endet nach drei Sekunden`() = runTest {
        val clock = SessionClock { testScheduler.currentTime }
        val seen = mutableListOf<Pair<Int, Long>>()
        clock.countdown { n -> seen += n to testScheduler.currentTime }
        assertEquals(listOf(3 to 0L, 2 to 1_000L, 1 to 2_000L), seen)
        assertEquals(SessionTiming.COUNTDOWN_SECONDS * SessionTiming.COUNTDOWN_STEP_MS, testScheduler.currentTime)
    }

    @Test
    fun `aktive Zeit zählt nur zwischen resume und accrue, eine Pause nicht`() {
        var t = 1_000L
        val clock = SessionClock { t }
        clock.reset()
        t += 5_000L
        clock.accrue()
        assertEquals(5_000L, clock.activeMs)

        t += 60_000L // Pause: kein accrue, beim Fortsetzen neuer Bezugspunkt
        clock.resume()
        t += 2_500L
        clock.accrue()
        assertEquals(7_500L, clock.activeMs)

        clock.reset()
        assertEquals(0L, clock.activeMs)
    }
}
