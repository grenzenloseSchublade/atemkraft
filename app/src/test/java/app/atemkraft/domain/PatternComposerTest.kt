package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wunsch-Generator des Muster-Editors: vollständig, regelkonform, ohne Zufall, feste Reihenfolge. */
class PatternComposerTest {

    private val calmWithout = PatternWish(activating = false, withHold = false)
    private val calmWith = PatternWish(activating = false, withHold = true)
    private val activeWithout = PatternWish(activating = true, withHold = false)
    private val activeWith = PatternWish(activating = true, withHold = true)
    private val wishes = listOf(calmWithout, calmWith, activeWithout, activeWith)

    private fun spec(inhale: Int, holdFull: Int, exhale: Int, holdEmpty: Int, activating: Boolean) = PatternSpec(
        inhale = inhale.toDouble(),
        holdFull = holdFull.takeIf { it > 0 }?.toDouble(),
        exhale = exhale.toDouble(),
        holdEmpty = holdEmpty.takeIf { it > 0 }?.toDouble(),
        activating = activating,
    )

    @Test
    fun `kein Wunsch hat eine leere Liste 13 141 8 10`() {
        assertEquals(13, PatternComposer.candidates(calmWithout).size)
        assertEquals(141, PatternComposer.candidates(calmWith).size)
        assertEquals(8, PatternComposer.candidates(activeWithout).size)
        assertEquals(10, PatternComposer.candidates(activeWith).size)
    }

    @Test
    fun `alle Kandidaten sind gueltig und passen zum Wunsch`() {
        wishes.forEach { wish ->
            PatternComposer.candidates(wish).forEach { spec ->
                assertTrue("$wish $spec", spec.withinCreationRules())
                assertTrue("$wish $spec", spec.withinGuardrails())
                assertEquals("$wish $spec", wish, PatternComposer.wishOf(spec))
                assertEquals(wish.activating, spec.activating)
                if (wish.activating) {
                    assertTrue("$spec", spec.exhale < spec.inhale)
                    assertEquals(null, spec.holdEmpty)
                } else {
                    // Vorschlag immer Langhana; Samana nur durch Feinjustieren.
                    assertTrue("$spec", spec.exhale > spec.inhale)
                }
                if (!wish.withHold) {
                    assertEquals(null, spec.holdFull)
                    assertEquals(null, spec.holdEmpty)
                }
            }
        }
    }

    @Test
    fun `Kandidaten decken alle gueltigen Muster ausser Samana ab`() {
        val all = wishes.flatMap { PatternComposer.candidates(it) }
        assertEquals(all.size, all.toSet().size)
        assertEquals(154, all.count { !it.activating })
        assertEquals(18, all.count { it.activating })
    }

    @Test
    fun `typischer Vorschlag je Wunsch`() {
        assertEquals(spec(4, 0, 6, 0, false), PatternComposer.propose(calmWithout, 0))
        assertEquals(spec(4, 1, 6, 0, false), PatternComposer.propose(calmWith, 0))
        assertEquals(spec(4, 0, 3, 0, true), PatternComposer.propose(activeWithout, 0))
        assertEquals(spec(4, 1, 3, 0, true), PatternComposer.propose(activeWith, 0))
    }

    /** Friert die Typikalitäts-Reihenfolge ein: „Anderer Vorschlag“ blättert immer gleich. */
    @Test
    fun `Reihenfolge der ersten Vorschlaege`() {
        assertEquals(
            listOf(spec(4, 0, 6, 0, false), spec(4, 0, 5, 0, false), spec(4, 0, 7, 0, false), spec(4, 0, 8, 0, false)),
            PatternComposer.candidates(calmWithout).take(4),
        )
        assertEquals(
            listOf(spec(4, 1, 6, 0, false), spec(4, 0, 6, 1, false), spec(4, 2, 6, 0, false), spec(4, 1, 6, 1, false)),
            PatternComposer.candidates(calmWith).take(4),
        )
        assertEquals(
            listOf(spec(4, 0, 3, 0, true), spec(4, 0, 2, 0, true), spec(5, 0, 3, 0, true), spec(5, 0, 4, 0, true)),
            PatternComposer.candidates(activeWithout).take(4),
        )
        assertEquals(
            listOf(spec(4, 1, 3, 0, true), spec(4, 2, 3, 0, true), spec(4, 1, 2, 0, true), spec(4, 2, 2, 0, true)),
            PatternComposer.candidates(activeWith).take(4),
        )
    }

    @Test
    fun `propose ist deterministisch und blaettert ohne Wiederholung`() {
        wishes.forEach { wish ->
            val n = PatternComposer.candidates(wish).size
            val round = (0 until n).map { PatternComposer.propose(wish, it) }
            assertEquals(round, (0 until n).map { PatternComposer.propose(wish, it) })
            assertEquals("$wish", n, round.toSet().size)
            assertEquals(PatternComposer.propose(wish, 0), PatternComposer.propose(wish, n))
            assertEquals(PatternComposer.propose(wish, n - 1), PatternComposer.propose(wish, -1))
        }
    }

    @Test
    fun `wishOf des Vorschlags ergibt den Wunsch`() {
        wishes.forEach { wish -> assertEquals(wish, PatternComposer.wishOf(PatternComposer.propose(wish, 0))) }
    }

    @Test
    fun `wishOf leitet aus Tagesmustern ab`() {
        (0L until 500L).map { RandomPatternGenerator.forSeed(it).spec }.forEach { spec ->
            val wish = PatternComposer.wishOf(spec)
            assertEquals(spec.activating, wish.activating)
            assertEquals(spec.holdFull != null || spec.holdEmpty != null, wish.withHold)
        }
        assertEquals(calmWithout, PatternComposer.wishOf(PatternSpec(4.0, 0.0, 6.0, null, false)))
    }
}
