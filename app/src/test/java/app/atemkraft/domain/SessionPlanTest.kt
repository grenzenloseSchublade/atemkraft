package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests für das Herzstück der Session-Korrektheit: Timeline-Ausmultiplikation + Dauerschätzung. */
class SessionPlanTest {

    private fun exercise(
        rounds: Int = 1,
        segments: List<Segment>,
        perRound: (Phase, Int) -> Phase = { p, _ -> p },
    ) = Exercise(
        id = "test",
        name = "Test",
        family = BreathingFamily.DOWNREGULATE,
        shortDescription = "",
        effect = "",
        segments = segments,
        rounds = rounds,
        perRound = perRound,
    )

    private fun fixed(type: PhaseType, seconds: Double) = Phase(type, PhaseDuration.Fixed((seconds * 1000).toLong()))

    @Test
    fun `timeline multipliziert Runden mal Segmente mal Wiederholungen mal Phasen aus`() {
        val ex = exercise(
            rounds = 3,
            segments = listOf(
                Segment(
                    phases = listOf(fixed(PhaseType.INHALE, 4.0), fixed(PhaseType.EXHALE, 6.0)),
                    repeat = 5,
                ),
            ),
        )
        val timeline = ex.buildTimeline()
        assertEquals(3 * 5 * 2, timeline.size)
        // Runden-Metadaten stimmen
        assertEquals(0, timeline.first().roundIndex)
        assertEquals(2, timeline.last().roundIndex)
        assertTrue(timeline.all { it.roundCount == 3 })
    }

    @Test
    fun `perRound passt Phasen der jeweiligen Runde an (Wim-Hof-Muster)`() {
        val ex = exercise(
            rounds = 2,
            segments = listOf(Segment(listOf(fixed(PhaseType.HOLD_EMPTY, 10.0)))),
            perRound = { phase, round ->
                if (round == 1) phase.copy(duration = PhaseDuration.Fixed(20_000)) else phase
            },
        )
        val timeline = ex.buildTimeline()
        assertEquals(10_000L, (timeline[0].duration as PhaseDuration.Fixed).millis)
        assertEquals(20_000L, (timeline[1].duration as PhaseDuration.Fixed).millis)
    }

    @Test
    fun `estimatedTotalSeconds summiert nur feste Phasen`() {
        val ex = exercise(
            rounds = 2,
            segments = listOf(
                Segment(
                    listOf(
                        fixed(PhaseType.INHALE, 4.0),
                        Phase(PhaseType.HOLD_EMPTY, PhaseDuration.UntilUrge), // offen → zählt nicht
                        fixed(PhaseType.EXHALE, 6.0),
                    ),
                ),
            ),
        )
        assertEquals(2 * (4 + 6), ex.estimatedTotalSeconds())
        assertTrue(ex.hasOpenPhases)
    }

    @Test
    fun `isRoundBased nur bei mehr als einer Runde`() {
        val one = exercise(rounds = 1, segments = listOf(Segment(listOf(fixed(PhaseType.INHALE, 4.0)))))
        val many = exercise(rounds = 4, segments = listOf(Segment(listOf(fixed(PhaseType.INHALE, 4.0)))))
        assertFalse(one.isRoundBased)
        assertTrue(many.isRoundBased)
    }

    @Test
    fun `cycleSeconds und defaultMinutes aus Segment-Wiederholung`() {
        // 10-s-Zyklus, 30 Wiederholungen → 300 s → 5 min Vorgabe
        val ex = exercise(
            segments = listOf(
                Segment(
                    phases = listOf(fixed(PhaseType.INHALE, 4.0), fixed(PhaseType.EXHALE, 6.0)),
                    repeat = 30,
                ),
            ),
        )
        assertEquals(10, ex.cycleSeconds())
        assertEquals(5, ex.defaultMinutes())
    }
}
