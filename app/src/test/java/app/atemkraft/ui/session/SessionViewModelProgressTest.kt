package app.atemkraft.ui.session

import android.os.Looper
import app.atemkraft.data.ExerciseRepository
import app.atemkraft.data.FakeSavedPatternDao
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.domain.SessionConfig
import app.atemkraft.playback.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Fortschritt und Bilanz im echten Ablauf: Der Bogen steht in der Pause und in offenen Phasen still,
 * die Bilanz zählt nur aktive Atemzeit. Die Uhr läuft über den pausierten Main-Looper von Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class SessionViewModelProgressTest {

    private val repository = ExerciseRepository(SavedPatternsRepository(FakeSavedPatternDao()))

    private fun exercise(id: String) = requireNotNull(repository.byId(id))

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun `Box-Atmung - Rundenfortschritt, Stillstand in der Pause, Bilanz am Ende`() {
        val vm = SessionViewModel()
        vm.start(exercise("box-4-4-4-4")) // 8 Runden à 16 s
        assertEquals(0f, vm.state.value.progress, 0f)
        idle(COUNTDOWN_MS + 2 * 16_000L)
        assertEquals(2f / 8f, vm.state.value.progress, 0.01f)
        assertNull(vm.state.value.summary)

        vm.pause()
        val paused = vm.state.value.progress
        idle(10_000L)
        assertEquals(paused, vm.state.value.progress, 0f)

        vm.resume()
        idle(7 * 16_000L)
        val end = vm.state.value
        assertEquals(SessionStatus.FINISHED, end.status)
        assertEquals(1f, end.progress, 0f)
        val summary = requireNotNull(end.summary)
        assertEquals(8, summary.rounds)
        assertEquals(8, summary.breaths)
        // Aktive Zeit ohne Countdown und Pause (Toleranz: ein Tick pro Pause/Phasenwechsel).
        assertEquals(128_000.0, summary.durationMs.toDouble(), 250.0)
    }

    @Test
    fun `Wim Hof - im Halten bis zum Atemreiz steht der Bogen still`() {
        val vm = SessionViewModel()
        vm.start(exercise("wim-hof")) // Runde: 30 × 3 s, Ausatmen 2 s, Halten offen, Einatmen 2 s, Halten 15 s
        idle(COUNTDOWN_MS + 30 * 3_000L + 2_000L + 500L)
        assertEquals(SessionStatus.WAITING_FOR_USER, vm.state.value.status)
        val waiting = vm.state.value.progress
        assertEquals((92f / 109f) / 3f, waiting, 0.005f)
        idle(20_000L)
        assertEquals(waiting, vm.state.value.progress, 0f)

        vm.continueFromUserPaced()
        idle(1_000L)
        assertEquals((93f / 109f) / 3f, vm.state.value.progress, 0.005f)
        vm.stop()
    }

    @Test
    fun `Von vorne startet dasselbe Muster samt Anpassung, ohne es per Id neu nachzuschlagen`() {
        val vm = SessionViewModel()
        // Ein Muster, das das Repository nicht kennt (wie ein Tagesmuster von gestern).
        val pattern = exercise("box-4-4-4-4").copy(id = "nicht-im-repository", name = "Eigenes Muster")
        vm.start(pattern, SessionConfig(rounds = 2))
        idle(COUNTDOWN_MS + 5_000L)

        vm.restart()
        val restarted = vm.state.value
        assertEquals("Eigenes Muster", restarted.exerciseName)
        assertEquals(SessionStatus.PREPARING, restarted.status)
        assertEquals(2, restarted.roundCount)
        vm.stop()
    }

    private companion object {
        const val COUNTDOWN_MS = 3_000L
    }
}
