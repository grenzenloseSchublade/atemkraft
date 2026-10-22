package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Erzeugungsregeln über das ganze Gitter (Einatmen 2..8, Ausatmen 2..8, Halten voll 0..4,
 * Halten leer 0..2, beide Charaktere): Teilmenge der Import-Leitplanken, nachgezählte Mengen.
 */
class PatternRulesTest {

    private val grid: List<PatternSpec> = buildList {
        for (activating in listOf(false, true)) {
            for (inhale in 2..8) {
                for (exhale in 2..8) {
                    for (holdFull in 0..4) {
                        for (holdEmpty in 0..2) {
                            add(spec(inhale, holdFull, exhale, holdEmpty, activating))
                        }
                    }
                }
            }
        }
    }

    private val valid: List<PatternSpec> = grid.filter { it.withinCreationRules() }
    private val calm: List<PatternSpec> = valid.filter { !it.activating }
    private val active: List<PatternSpec> = valid.filter { it.activating }

    private fun spec(inhale: Int, holdFull: Int, exhale: Int, holdEmpty: Int, activating: Boolean) = PatternSpec(
        inhale = inhale.toDouble(),
        holdFull = holdFull.takeIf { it > 0 }?.toDouble(),
        exhale = exhale.toDouble(),
        holdEmpty = holdEmpty.takeIf { it > 0 }?.toDouble(),
        activating = activating,
    )

    private fun PatternSpec.hasHold() = holdFull != null || holdEmpty != null

    /** Jedes erzeugte Muster besteht den Import: kein Verwerfen, keine Sicherheitsabfrage. */
    @Test
    fun `Erzeugungsregeln sind eine Teilmenge der Leitplanken`() {
        valid.forEach { assertTrue("$it", it.withinGuardrails()) }
    }

    @Test
    fun `Zaehlung ruhig 191 davon 37 Samana und 15 ohne Halten`() {
        assertEquals(191, calm.size)
        assertEquals(37, calm.count { it.exhale == it.inhale })
        assertEquals(15, calm.count { !it.hasHold() })
        assertEquals(13, calm.count { !it.hasHold() && it.exhale > it.inhale })
        assertEquals(141, calm.count { it.hasHold() && it.exhale > it.inhale })
    }

    @Test
    fun `Zaehlung aktivierend 18 davon 8 ohne und 10 mit Halten`() {
        assertEquals(18, active.size)
        assertEquals(8, active.count { !it.hasHold() })
        assertEquals(10, active.count { it.hasHold() })
        assertEquals(209, valid.size)
    }

    @Test
    fun `erreichbare Grenzen je Charakter`() {
        fun List<PatternSpec>.span(field: PatternField) = minOf { it.secondsOf(field) }..maxOf { it.secondsOf(field) }
        assertEquals(2..6, calm.span(PatternField.INHALE))
        assertEquals(2..8, calm.span(PatternField.EXHALE))
        assertEquals(0..4, calm.span(PatternField.HOLD_FULL))
        assertEquals(0..2, calm.span(PatternField.HOLD_EMPTY))
        assertEquals(3..7, active.span(PatternField.INHALE))
        assertEquals(2..4, active.span(PatternField.EXHALE))
        assertEquals(0..2, active.span(PatternField.HOLD_FULL))
        assertTrue(active.all { it.holdEmpty == null })
    }

    /** 4·4 = 8 s liegt unter 9 s: Ruhig ohne Halten mit Ein = Aus geht nur als 5·5 oder 6·6. */
    @Test
    fun `ruhig ohne Halten und gleich lang nur 5-5 und 6-6`() {
        val samana = calm.filter { !it.hasHold() && it.exhale == it.inhale }
        assertEquals(listOf(5, 6), samana.map { it.secondsOf(PatternField.INHALE) }.sorted())
    }

    @Test
    fun `halbe Sekunden Halten 0 und aktivierend gleich lang sind keine Erzeugung`() {
        assertTrue(PatternSpec(4.5, null, 6.0, null, false).withinGuardrails())
        assertFalse(PatternSpec(4.5, null, 6.0, null, false).withinCreationRules())
        assertFalse(PatternSpec(4.0, 0.0, 6.0, null, false).withinCreationRules())
        assertTrue(PatternSpec(4.0, null, 4.0, null, true).withinGuardrails())
        assertFalse(PatternSpec(4.0, null, 4.0, null, true).withinCreationRules())
        // 8,5 s: Leitplanke ja, Erzeugung nein.
        assertTrue(PatternSpec(4.0, null, 4.5, null, false).withinGuardrails())
        assertFalse(PatternSpec(4.0, null, 4.5, null, false).withinCreationRules())
        assertEquals(5, PatternSpec(4.5, null, 6.0, null, false).secondsOf(PatternField.INHALE))
    }

    /** Das Muster des Tages folgt den Erzeugungsregeln. */
    @Test
    fun `jedes Tagesmuster erfuellt die Erzeugungsregeln`() {
        (0L until 5000L).forEach { seed ->
            val spec = RandomPatternGenerator.forSeed(seed).spec
            assertTrue("$seed: $spec", spec.withinCreationRules())
        }
    }

    @Test
    fun `Konstanten passen zum Plan`() {
        assertEquals(9, PatternRules.CALM_CYCLE_MIN)
        assertEquals(13, PatternRules.CALM_CYCLE_MAX)
        assertEquals(8, PatternRules.EXHALE_MAX)
        assertEquals(6, PatternRules.ACTIVE_CYCLE_MIN)
        assertEquals(9, PatternRules.ACTIVE_CYCLE_MAX)
    }
}
