package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Übungen aus Mustern über alle erzeugbaren Specs: Dauer in der Wiederholung, Herkunft ändert nur
 * Texte, Bauchatmung nur als Einladung bei ruhigen Mustern, Hinweis ohne Umbruch zwischen Zahl
 * und Einheit. Werte des Tagesmusters friert `DailyPatternGoldenTest` ein, die Regeln
 * `PatternRulesTest`, die Herkunftstexte `ContentRulesTest`.
 */
class RandomPatternGeneratorTest {

    /** Die Dauer eines eigenen Musters steckt in `repeat`, nicht in einem Override (sonst „angepasst“). */
    @Test
    fun `minutes steuert Wiederholung und Vorgabedauer`() {
        creationRuleSpecs.forEach { spec ->
            listOf(1, 5, 30).forEach { minutes ->
                val exercise = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", PatternOrigin.CUSTOM, minutes)
                assertTrue("$spec $minutes min", exercise.buildTimeline().isNotEmpty())
                // Ein Atemzug ist höchstens 13 s lang, die Rundung bleibt also unter einer halben Minute.
                assertEquals("$spec $minutes min", minutes, exercise.defaultMinutes())
                assertEquals(1, exercise.rounds)
            }
            // Ohne Angabe bleibt die Vorgabe um 5 Minuten.
            val exercise = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", PatternOrigin.CUSTOM)
            assertTrue("$spec", exercise.defaultMinutes() in 4..6)
        }
    }

    @Test
    fun `Herkunft aendert nur Texte, nicht Phasen und Charakter`() {
        creationRuleSpecs.forEach { spec ->
            val today = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster")
            PatternOrigin.entries.forEach { origin ->
                val e = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin)
                assertEquals("$spec $origin", today.segments, e.segments)
                assertEquals(today.instructionHint, e.instructionHint)
                assertEquals(today.lore?.teaser, e.lore?.teaser)
                assertEquals(today.lore?.patternId, e.lore?.patternId)
                assertEquals(today.lore?.morePatternIds, e.lore?.morePatternIds)
                assertTrue(e.generated)
                assertEquals(spec.activating, e.activating)
            }
        }
    }

    /** Bauchatmung nur bei ruhigen Mustern und nur als Einladung, nie als Muss (Content-Linie). */
    @Test
    fun `Muster laden nur bei ruhig zur Bauchatmung ein`() {
        creationRuleSpecs.forEach { spec ->
            PatternOrigin.entries.forEach { origin ->
                val e = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin)
                assertEquals("$spec $origin", !spec.activating, e.instructionHint!!.contains("Bauch"))
                if (origin != PatternOrigin.TODAY) {
                    assertEquals("$spec $origin", !spec.activating, e.shortDescription.contains("Bauch"))
                    assertTrue("$spec $origin", !e.shortDescription.contains("empfohlen"))
                    assertTrue("$spec $origin", e.shortDescription.startsWith(RandomPatternGenerator.hintFor(spec)))
                }
            }
        }
    }

    @Test
    fun `Hinweis bricht nie zwischen Zahl und Einheit oder vor dem Trennpunkt um`() {
        creationRuleSpecs.forEach { spec ->
            val hint = RandomPatternGenerator.hintFor(spec)
            // Normales Leerzeichen vor „s“ oder „·“ wäre eine Umbruchstelle (TEXT-10).
            assertTrue(hint, !Regex("\\d s\\b| ·").containsMatchIn(hint))
            assertTrue(hint, hint.contains("\u00A0s"))
        }
    }
}
