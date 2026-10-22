package app.atemkraft.domain

import app.atemkraft.domain.FreePatternProblem.NO_EXHALE
import app.atemkraft.domain.FreePatternProblem.SECONDS_OUT_OF_RANGE
import app.atemkraft.domain.FreePatternProblem.TOO_FEW_PHASES
import app.atemkraft.domain.FreePatternProblem.TOO_MANY_PHASES
import app.atemkraft.domain.FreePhaseType.IN
import app.atemkraft.domain.FreePhaseType.OUT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sicherheitsgrenzen der freien Folge („Frei gestalten“): 1–30 s je Phase, 2–12 Phasen. */
class FreePatternTest {

    private fun pattern(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { FreePhase(it.first, it.second) })

    @Test
    fun `Grenzen der freien Folge - pruefen, klemmen, Parser`() {
        val twelve = FreePattern(List(6) { FreePhase(IN, 4) } + List(6) { FreePhase(OUT, 4) })
        assertTrue(pattern(IN to 1, OUT to 1).isValid())
        assertTrue(pattern(IN to 30, OUT to 30).isValid())
        assertTrue(twelve.isValid())
        assertEquals(listOf(SECONDS_OUT_OF_RANGE), pattern(IN to 0, OUT to 4).validate())
        assertEquals(listOf(SECONDS_OUT_OF_RANGE), pattern(IN to 31, OUT to 4).validate())
        assertEquals(listOf(TOO_FEW_PHASES, NO_EXHALE), pattern(IN to 4).validate())
        assertEquals(listOf(TOO_MANY_PHASES), FreePattern(listOf(FreePhase(IN, 4)) + twelve.phases).validate())

        // Der Editor klemmt, statt die Grenzen zu verlassen.
        val base = pattern(IN to 4, OUT to 6)
        assertEquals(30, base.withSeconds(0, 99).phases[0].seconds)
        assertEquals(1, base.withSeconds(1, 0).phases[1].seconds)
        assertFalse(twelve.canAdd)
        assertEquals(twelve, twelve.inserted(null, OUT))
        assertFalse(base.canRemove(0))

        // Gespeicherte oder importierte Folgen jenseits der Grenzen werden nicht gelesen.
        listOf("in:4", "in:0,out:6", "in:31,out:6", List(13) { "in:1" }.joinToString(",") + ",out:1")
            .forEach { assertNull(it, FreePattern.parse(it)) }
    }
}
