package app.atemkraft.domain

import org.junit.Assert.assertEquals
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

        // perRound passt Phasen der jeweiligen Runde an (Wim-Hof-Muster).
        val wimHof = exercise(
            rounds = 2,
            segments = listOf(Segment(listOf(fixed(PhaseType.HOLD_EMPTY, 10.0)))),
            perRound = { phase, round ->
                if (round == 1) phase.copy(duration = PhaseDuration.Fixed(20_000)) else phase
            },
        )
        val perRound = wimHof.buildTimeline()
        assertEquals(10_000L, (perRound[0].duration as PhaseDuration.Fixed).millis)
        assertEquals(20_000L, (perRound[1].duration as PhaseDuration.Fixed).millis)
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
    fun `Minuten rechnen mit der exakten Zykluslaenge`() {
        // 10-s-Zyklus, 30 Wiederholungen → 300 s → 5 min Vorgabe.
        val even = exercise(segments = listOf(Segment(listOf(fixed(PhaseType.INHALE, 4.0), fixed(PhaseType.EXHALE, 6.0)), repeat = 30)))
        assertEquals(10, even.cycleSeconds())
        assertEquals(5, even.defaultMinutes())
        // 11,5-s-Zyklus (gespeichertes Muster mit 0,5-s-Phasen): 5 min → 26 Zyklen = 299 s, nicht 27 (310,5 s).
        val ex = exercise(
            segments = listOf(
                Segment(phases = listOf(fixed(PhaseType.INHALE, 5.0), fixed(PhaseType.EXHALE, 6.5)), repeat = 1),
            ),
        )
        val repeat = ex.adjusted(SessionConfig(minutes = 5)).segments.single().repeat
        assertEquals(26, repeat)
        assertEquals(11_500L, ex.cycleMillis())
    }

    @Test
    fun `Halten leer folgt dem Stepper nur, wenn es so lang ist wie Halten voll`() {
        fun box(full: Double, empty: Double) = exercise(
            segments = listOf(
                Segment(
                    phases = listOf(
                        fixed(PhaseType.INHALE, 4.0),
                        fixed(PhaseType.HOLD_FULL, full),
                        fixed(PhaseType.EXHALE, 5.0),
                        fixed(PhaseType.HOLD_EMPTY, empty),
                    ),
                    repeat = 1,
                ),
            ),
        )
        fun millisOf(ex: Exercise, type: PhaseType) = (ex.segments.single().phases.single { it.type == type }.duration as PhaseDuration.Fixed).millis

        // Box (gleich lang): beide Halten folgen.
        val equal = box(4.0, 4.0).adjusted(SessionConfig(minutes = 5, holdSeconds = 6.0))
        assertEquals(6_000L, millisOf(equal, PhaseType.HOLD_FULL))
        assertEquals(6_000L, millisOf(equal, PhaseType.HOLD_EMPTY))

        // Tagesmuster (voll 3 s, leer 1 s): nur Halten voll folgt, Halten leer bleibt ≤ 2 s.
        val soft = box(3.0, 1.0).adjusted(SessionConfig(minutes = 5, holdSeconds = 4.0))
        assertEquals(4_000L, millisOf(soft, PhaseType.HOLD_FULL))
        assertEquals(1_000L, millisOf(soft, PhaseType.HOLD_EMPTY))
    }
}
