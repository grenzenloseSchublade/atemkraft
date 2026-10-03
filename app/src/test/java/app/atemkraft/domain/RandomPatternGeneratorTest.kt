package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Invarianten des Tagesmuster-Generators über ein ganzes Jahr Seeds: Determinismus,
 * physiologische Leitplanken je Charakter, lauffähige Timeline, Namensformat.
 */
class RandomPatternGeneratorTest {

    private val year: List<LocalDate> =
        (0L until 365L).map { LocalDate.of(2026, 1, 1).plusDays(it) }

    private fun phaseSeconds(exercise: Exercise): Map<PhaseType, Double> = exercise.segments.single().phases.associate { phase ->
        phase.type to (phase.duration as PhaseDuration.Fixed).millis / 1000.0
    }

    @Test
    fun `gleiches Datum ergibt identisches Muster`() {
        year.forEach { date ->
            val a = RandomPatternGenerator.forDate(date)
            val b = RandomPatternGenerator.forDate(date)
            assertEquals(a.exercise, b.exercise)
            assertEquals(a.activating, b.activating)
        }
    }

    @Test
    fun `ruhige Muster halten die Leitplanken ein`() {
        year.map { RandomPatternGenerator.forDate(it) }
            .filter { !it.activating }
            .forEach { daily ->
                val p = phaseSeconds(daily.exercise)
                val inhale = p.getValue(PhaseType.INHALE)
                val exhale = p.getValue(PhaseType.EXHALE)
                val holdFull = p[PhaseType.HOLD_FULL] ?: 0.0
                val holdEmpty = p[PhaseType.HOLD_EMPTY] ?: 0.0
                val cycle = inhale + exhale + holdFull + holdEmpty

                assertTrue("Aus ($exhale) >= Ein ($inhale)", exhale >= inhale)
                assertTrue("Zyklus $cycle in 8,5..13", cycle in 8.5..13.0)
                assertTrue("Halten voll $holdFull <= 4", holdFull <= 4.0)
                assertTrue("Halten leer $holdEmpty <= 2", holdEmpty <= 2.0)
            }
    }

    @Test
    fun `aktivierende Muster bleiben sanft`() {
        val activating = year.map { RandomPatternGenerator.forDate(it) }.filter { it.activating }
        assertTrue("aktivierende Muster kommen vor", activating.isNotEmpty())
        activating.forEach { daily ->
            val p = phaseSeconds(daily.exercise)
            val inhale = p.getValue(PhaseType.INHALE)
            val exhale = p.getValue(PhaseType.EXHALE)
            val holdFull = p[PhaseType.HOLD_FULL] ?: 0.0
            val cycle = inhale + exhale + holdFull

            assertTrue("Ein ($inhale) >= Aus ($exhale)", inhale >= exhale)
            assertTrue("Zyklus $cycle in 6..9", cycle in 6.0..9.0)
            assertTrue("Halten $holdFull <= 2", holdFull <= 2.0)
            assertTrue("kein Halten nach dem Ausatmen", PhaseType.HOLD_EMPTY !in p)
        }
    }

    @Test
    fun `Timeline lauffaehig und Vorgabedauer um 5 Minuten`() {
        year.forEach { date ->
            val exercise = RandomPatternGenerator.forDate(date).exercise
            assertTrue(exercise.buildTimeline().isNotEmpty())
            assertTrue("defaultMinutes in 4..6", exercise.defaultMinutes() in 4..6)
            assertEquals(RandomPatternGenerator.ID, exercise.id)
            assertTrue(exercise.guided)
        }
    }

    @Test
    fun `Name und Hinweis sind gefuellt und konsistent`() {
        year.forEach { date ->
            val exercise = RandomPatternGenerator.forDate(date).exercise
            assertTrue(exercise.name.startsWith("Tagesmuster "))
            assertTrue(exercise.name.contains("·"))
            assertTrue(!exercise.instructionHint.isNullOrBlank())
            // Ruhige Muster leiten zur Bauchatmung an; aktivierende bleiben neutral.
            val daily = RandomPatternGenerator.forDate(date)
            assertEquals(!daily.activating, exercise.instructionHint!!.contains("Bauch"))
            // Keine Wirkversprechen-Floskeln im Effekt-Teaser (Content-Linie).
            assertTrue(exercise.effect.contains("ohne spezifisches Wirkversprechen"))
        }
    }
}
