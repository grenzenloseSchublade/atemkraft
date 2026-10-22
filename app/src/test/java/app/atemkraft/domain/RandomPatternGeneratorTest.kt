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

    /** Der Import prüft gegen [withinGuardrails]: Alles, was der Generator erzeugt, muss durchgehen. */
    @Test
    fun `withinGuardrails akzeptiert jedes generierte Muster`() {
        (0L until 5000L).map { RandomPatternGenerator.forSeed(it) }.forEach { daily ->
            assertTrue("${daily.spec}", daily.spec.withinGuardrails())
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

    /** Ganze Sekunden: Countdown, „Phasen anpassen“ und Atemlehre-Verhältnisse passen zusammen. */
    @Test
    fun `alle Phasen in ganzen Sekunden`() {
        (0L until 5000L).map { RandomPatternGenerator.forSeed(it).spec }.forEach { spec ->
            listOfNotNull(spec.inhale, spec.holdFull, spec.exhale, spec.holdEmpty).forEach { seconds ->
                assertTrue("$spec", seconds % 1.0 == 0.0)
            }
        }
    }

    /** Aktivierend heißt Brmhana: Einatmen immer länger als Ausatmen, nie nur „zügig“. */
    @Test
    fun `aktivierende Muster atmen laenger ein als aus`() {
        (0L until 5000L).map { RandomPatternGenerator.forSeed(it).spec }.filter { it.activating }.forEach { spec ->
            assertTrue("$spec", spec.inhale > spec.exhale)
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

    /**
     * Charakter-Chip („ruhig“ / „sanft aktivierend“) und Familienzeile „Muster“ lesen
     * [Exercise.activating] und [Exercise.generated] – für das Tagesmuster wie für gespeicherte Muster.
     */
    @Test
    fun `Uebung traegt Charakter und Kennung generiert`() {
        val patterns = (0L until 200L).map { RandomPatternGenerator.forSeed(it) }
        val activating = patterns.first { it.activating }
        val calm = patterns.first { !it.activating }
        listOf(activating, calm).forEach { daily ->
            assertTrue(daily.exercise.generated)
            assertEquals(daily.spec.activating, daily.exercise.activating)
            val saved = RandomPatternGenerator.exerciseFrom(daily.spec, "muster-1", "Mein Muster")
            assertTrue(saved.generated)
            assertEquals(daily.spec.activating, saved.activating)
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

    /** Alle Wunsch-Vorschläge des Editors: jedes Muster, das ein eigenes Muster werden kann. */
    private val composerSpecs: List<PatternSpec> = listOf(false, true).flatMap { activating ->
        listOf(false, true).flatMap { PatternComposer.candidates(PatternWish(activating, it)) }
    }

    /** Die Dauer eines eigenen Musters steckt in `repeat`, nicht in einem Override (sonst „angepasst“). */
    @Test
    fun `minutes steuert Wiederholung und Vorgabedauer`() {
        val specs = composerSpecs + (0L until 50L).map { RandomPatternGenerator.forSeed(it).spec }
        listOf(1, 3, 5, 10, 30).forEach { minutes ->
            specs.forEach { spec ->
                PatternOrigin.entries.forEach { origin ->
                    val exercise = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin, minutes)
                    assertTrue("$spec $minutes min", exercise.buildTimeline().isNotEmpty())
                    // Ein Atemzug ist höchstens 13 s lang, die Rundung bleibt also unter einer halben Minute.
                    assertEquals("$spec $minutes min", minutes, exercise.defaultMinutes())
                    assertEquals(1, exercise.rounds)
                }
            }
        }
    }

    @Test
    fun `ohne minutes bleibt die Vorgabe um 5 Minuten`() {
        composerSpecs.forEach { spec ->
            val exercise = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", PatternOrigin.CUSTOM)
            assertTrue("$spec", exercise.defaultMinutes() in 4..6)
        }
    }

    /** Ohne Herkunft entsteht das Muster des Tages: Standardwerte = heutiges Verhalten. */
    @Test
    fun `Standard-Herkunft ist das Muster des Tages`() {
        (0L until 50L).map { RandomPatternGenerator.forSeed(it) }.forEach { daily ->
            val explicit = RandomPatternGenerator.exerciseFrom(
                daily.spec,
                RandomPatternGenerator.ID,
                daily.exercise.name,
                PatternOrigin.TODAY,
                null,
            )
            assertEquals(daily.exercise, explicit)
        }
    }

    @Test
    fun `Herkunft aendert nur Texte, nicht Phasen und Charakter`() {
        composerSpecs.forEach { spec ->
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
    fun `eigene und gespeicherte Muster laden nur bei ruhig zur Bauchatmung ein`() {
        composerSpecs.forEach { spec ->
            listOf(PatternOrigin.CUSTOM, PatternOrigin.SAVED_DAILY).forEach { origin ->
                val e = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin)
                assertEquals("$spec $origin", !spec.activating, e.shortDescription.contains("Bauch"))
                assertEquals("$spec $origin", !spec.activating, e.instructionHint!!.contains("Bauch"))
                assertTrue("$spec $origin", !e.shortDescription.contains("empfohlen"))
                assertTrue("$spec $origin", e.shortDescription.startsWith(RandomPatternGenerator.hintFor(spec)))
            }
        }
    }

    @Test
    fun `Texte je Herkunft`() {
        val spec = PatternComposer.propose(PatternWish(activating = false, withHold = true), 0)
        val custom = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Eigenes Muster", PatternOrigin.CUSTOM, 5)
        val saved = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Tagesmuster", PatternOrigin.SAVED_DAILY, 5)
        assertEquals("Dieses Muster hast du selbst zusammengestellt – in den Leitplanken der App.", custom.effect)
        assertEquals(
            "Dieses Muster stammt aus einem Muster des Tages und liegt in den Leitplanken der App.",
            saved.effect,
        )
        listOf(custom, saved).forEach { e ->
            assertTrue(e.effectDetail!!.contains("Eine Studienlage zu genau diesem Muster gibt es nicht."))
            assertTrue(!e.effectDetail!!.contains("Wirkversprechen"))
            assertTrue(!e.effect.contains("Wirkversprechen"))
        }
        assertTrue(custom.lore!!.detail.startsWith("Du hast das Muster selbst zusammengestellt;"))
        assertTrue(saved.lore!!.detail.startsWith("Das Muster stammt aus einem Muster des Tages;"))
        // Die Regelzeile nennt die Zahlen der Erzeugungsregeln.
        assertTrue(custom.effectDetail!!, custom.effectDetail!!.contains("4,6 bis 6,7 Atemzügen pro Minute"))
        val active = PatternComposer.propose(PatternWish(activating = true, withHold = false), 0)
        val customActive = RandomPatternGenerator.exerciseFrom(active, "muster-1", "Eigenes Muster", PatternOrigin.CUSTOM)
        assertTrue(customActive.effectDetail!!, customActive.effectDetail!!.contains("6,7 bis 10 Atemzügen pro Minute"))
    }

    @Test
    fun `Entwurf hat eine feste Id`() {
        assertEquals("muster-entwurf", RandomPatternGenerator.DRAFT_ID)
    }

    @Test
    fun `Hinweis bricht nie zwischen Zahl und Einheit oder vor dem Trennpunkt um`() {
        year.forEach { date ->
            val hint = RandomPatternGenerator.hintFor(RandomPatternGenerator.forDate(date).spec)
            // Normales Leerzeichen vor „s“ oder „·“ wäre eine Umbruchstelle (TEXT-10).
            assertTrue(hint, !Regex("\\d s\\b| ·").containsMatchIn(hint))
            assertTrue(hint, hint.contains("\u00A0s"))
        }
    }
}
