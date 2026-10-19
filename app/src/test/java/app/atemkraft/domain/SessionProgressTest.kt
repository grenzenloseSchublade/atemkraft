package app.atemkraft.domain

import app.atemkraft.domain.PhaseType.EXHALE
import app.atemkraft.domain.PhaseType.HOLD_EMPTY
import app.atemkraft.domain.PhaseType.HOLD_FULL
import app.atemkraft.domain.PhaseType.INHALE
import app.atemkraft.domain.PhaseType.INHALE_TOP_UP
import app.atemkraft.domain.PhaseType.REST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Fortschrittsbogen und Bilanz: reine Rechnung auf der ausmultiplizierten Timeline. */
class SessionProgressTest {

    private fun phase(type: PhaseType, seconds: Double?, round: Int = 0, rounds: Int = 1) = RuntimePhase(
        type = type,
        duration = seconds?.let { PhaseDuration.Fixed((it * 1000).toLong()) } ?: PhaseDuration.UntilUrge,
        label = null,
        note = null,
        side = BreathSide.NONE,
        roundIndex = round,
        roundCount = rounds,
    )

    /** [cycles] × (Einatmen 4 s, Ausatmen 6 s) in einer Runde – wie Resonanz-Atmung. */
    private fun continuous(cycles: Int) = List(cycles) { listOf(phase(INHALE, 4.0), phase(EXHALE, 6.0)) }.flatten()

    @Test
    fun `ohne Runden ist der Fortschritt verstrichene durch gesamte Zeit`() {
        val progress = SessionProgress(continuous(3))
        assertEquals(0f, progress.at(0, 0L), DELTA)
        assertEquals(7f / 30f, progress.at(1, 3_000L), DELTA)
        assertEquals(1f, progress.at(5, 6_000L), DELTA)
    }

    @Test
    fun `verstrichene Zeit wird auf die Phasendauer begrenzt`() {
        val progress = SessionProgress(continuous(3))
        assertEquals(progress.at(0, 4_000L), progress.at(0, 99_000L), DELTA)
        assertEquals(0f, progress.at(0, -500L), DELTA)
    }

    @Test
    fun `rundenbasiert zaehlt jede Runde gleich viel samt Anteil der laufenden Runde`() {
        // Runde 1 dauert 10 s, Runde 2 doppelt so lang (perRound) – der Bogen folgt „Runde x von y“.
        val timeline = listOf(
            phase(INHALE, 4.0, round = 0, rounds = 2),
            phase(EXHALE, 6.0, round = 0, rounds = 2),
            phase(INHALE, 8.0, round = 1, rounds = 2),
            phase(EXHALE, 12.0, round = 1, rounds = 2),
        )
        val progress = SessionProgress(timeline)
        assertEquals(0.5f, progress.at(2, 0L), DELTA)
        assertEquals((1f + 10f / 20f) / 2f, progress.at(3, 2_000L), DELTA)
    }

    @Test
    fun `in offenen Phasen steht der Fortschritt still`() {
        val timeline = listOf(
            phase(EXHALE, 2.0, round = 0, rounds = 2),
            phase(HOLD_EMPTY, null, round = 0, rounds = 2),
            phase(INHALE, 2.0, round = 0, rounds = 2),
            phase(EXHALE, 2.0, round = 1, rounds = 2),
            phase(HOLD_EMPTY, null, round = 1, rounds = 2),
            phase(INHALE, 2.0, round = 1, rounds = 2),
        )
        val progress = SessionProgress(timeline)
        assertEquals(0.25f, progress.at(1, 0L), DELTA)
        assertEquals(0.25f, progress.at(1, 60_000L), DELTA)
    }

    @Test
    fun `Runde ohne feste Dauer zaehlt nach Phasen`() {
        val timeline = listOf(phase(HOLD_EMPTY, null), phase(HOLD_FULL, null))
        val progress = SessionProgress(timeline)
        assertEquals(0f, progress.at(0, 0L), DELTA)
        assertEquals(0.5f, progress.at(1, 0L), DELTA)
    }

    @Test
    fun `Index ausserhalb des Ablaufs liefert Anfang bzw Ende`() {
        val progress = SessionProgress(continuous(1))
        assertEquals(0f, progress.at(-1, 0L), DELTA)
        assertEquals(1f, progress.at(2, 0L), DELTA)
        assertEquals(0f, SessionProgress(emptyList()).at(-1, 0L), DELTA)
    }

    @Test
    fun `ein Atemzug je Einatmen nach einem Ausatmen`() {
        assertEquals(3, continuous(3).breathCount())
        // Physiologischer Seufzer: Nachschub gehört zum selben Atemzug.
        assertEquals(1, listOf(phase(INHALE, 2.0), phase(INHALE_TOP_UP, 1.0), phase(EXHALE, 5.0)).breathCount())
        // Viloma I: gestuftes Einatmen mit Halten dazwischen bleibt ein Atemzug.
        val viloma = listOf(
            phase(INHALE, 2.0),
            phase(HOLD_FULL, 2.0),
            phase(INHALE, 2.0),
            phase(HOLD_FULL, 2.0),
            phase(INHALE, 2.0),
            phase(EXHALE, 8.0),
        )
        assertEquals(2, (viloma + viloma).breathCount())
        // Feueratmung: Stoß zuerst, passives Einatmen zählt; die freie Pause nicht.
        val pumps = List(30) { listOf(phase(EXHALE, 0.5), phase(INHALE, 0.5)) }.flatten() + phase(REST, 45.0)
        assertEquals(30, pumps.breathCount())
    }

    @Test
    fun `Bilanz nennt Runden nur bei Rundenuebungen und Atemzuege nur wenn gezaehlt`() {
        val single = sessionSummary(continuous(60), durationMs = 600_000L)
        assertEquals(SessionSummary(durationMs = 600_000L, breaths = 60, rounds = null), single)

        val box = List(8) { r ->
            listOf(INHALE, HOLD_FULL, EXHALE, HOLD_EMPTY).map { phase(it, 4.0, round = r, rounds = 8) }
        }.flatten()
        assertEquals(SessionSummary(durationMs = 128_000L, breaths = 8, rounds = 8), sessionSummary(box, 128_000L))

        val holdsOnly = sessionSummary(listOf(phase(HOLD_EMPTY, null)), durationMs = -5L)
        assertNull(holdsOnly.breaths)
        assertEquals(0L, holdsOnly.durationMs)
    }

    private companion object {
        const val DELTA = 1e-4f
    }
}
