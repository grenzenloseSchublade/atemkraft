package app.atemkraft.domain

import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.Pranayama
import app.atemkraft.domain.ExerciseFilter.NO_HOLD
import app.atemkraft.domain.ExerciseFilter.SHORT
import app.atemkraft.domain.ExerciseFilter.YOGA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Filterreihe über der Übungsliste (MUSTER-12): Jeder Filter teilt die angeleiteten Übungen
 * sinnvoll (mindestens 3, nicht alle), mehrere gelten zugleich, leere Familien fallen weg.
 */
class ExerciseFilterTest {

    private val groups = BreathingFamily.entries
        .map { f -> f to BuiltInExercises.all.filter { it.family == f && it.guided } }
        .filter { (_, list) -> list.isNotEmpty() }

    private fun ids(active: Set<ExerciseFilter>) = ExerciseFilters.apply(groups, active, Pranayama.exerciseIds).flatMap { (_, l) -> l.map { it.id } }

    @Test
    fun `Filter teilen die Liste, UND-verknuepft, leere Familien fallen weg`() {
        val total = groups.sumOf { it.second.size }
        ExerciseFilter.entries.forEach { filter ->
            val hits = ids(setOf(filter)).size
            assertTrue("$filter trifft $hits von $total – teilt die Liste nicht", hits in 3 until total)
        }
        // Kurz: wie am Startknopf gerundet; Wim Hof (offene Phasen) nie, auch wenn die Untergrenze passte.
        assertEquals(listOf("physiological-sigh", "4-7-8", "bhramari", "box-4-4-4-4"), ids(setOf(SHORT)))
        assertTrue("wim-hof" !in ids(setOf(SHORT)))
        // Halten in Stufen (Viloma) ist auch Halten; Box-Atmung und 4-7-8 sind nur verwandt mit dem Yoga.
        assertTrue(ids(setOf(NO_HOLD)).none { it.startsWith("viloma") || it == "box-4-4-4-4" })
        assertTrue(ids(setOf(YOGA)).none { it == "box-4-4-4-4" || it == "4-7-8" })
        assertEquals(listOf("bhramari"), ids(setOf(SHORT, NO_HOLD, YOGA)))
        // Nur Familien mit Treffern: „Kurz“ trifft weder „Aktivieren“ noch „Atem schulen“.
        val families = ExerciseFilters.apply(groups, setOf(SHORT), Pranayama.exerciseIds).map { it.first }
        assertEquals(listOf(BreathingFamily.DOWNREGULATE, BreathingFamily.BALANCE), families)
        assertEquals(groups, ExerciseFilters.apply(groups, emptySet(), Pranayama.exerciseIds))
    }
}
