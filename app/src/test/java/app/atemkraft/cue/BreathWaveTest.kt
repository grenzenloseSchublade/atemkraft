package app.atemkraft.cue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die „Welle“ folgt dem Füllstand des Kreises: Einatmen schwillt an, Ausatmen klingt ab, Halten ist
 * still, und nach der Pause geht sie dort weiter, wo der Kreis steht. Bricht das, führt die
 * Vibration mit geschlossenen Augen falsch.
 */
class BreathWaveTest {

    @Test
    fun waveFollowsTheCircle() {
        val inhale = BreathWave.of(from = 0f, to = 1f, durationMs = 4_000)!!
        assertEquals(4_000L, inhale.timings.sum())
        assertTrue("Einatmen schwillt an", inhale.amplitudes.toList().zipWithNext().all { (a, b) -> b >= a })
        assertTrue(inhale.amplitudes.first() < inhale.amplitudes.last())

        val exhale = BreathWave.of(from = 1f, to = 0f, durationMs = 6_000)!!
        assertTrue("Ausatmen klingt ab", exhale.amplitudes.toList().zipWithNext().all { (a, b) -> b <= a })

        assertNull("Halten ist still", BreathWave.of(from = 1f, to = 1f, durationMs = 7_000))
        assertNull("zu kurz für eine Welle", BreathWave.of(from = 0f, to = 1f, durationMs = 60))

        // Fortsetzen nach der Hälfte: Die Welle beginnt in der Mitte, nicht wieder unten.
        val resumed = BreathWave.of(from = 0f, to = 1f, durationMs = 2_000, elapsedMs = 2_000)!!
        assertTrue(resumed.amplitudes.first() >= BreathWave.amplitudeAt(0.5f))
        assertEquals(BreathWave.amplitudeAt(1f).toDouble(), resumed.amplitudes.last().toDouble(), 3.0)
    }
}
