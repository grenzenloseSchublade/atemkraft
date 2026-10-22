package app.atemkraft.domain

import app.atemkraft.domain.FreePhaseType.HOLD
import app.atemkraft.domain.FreePhaseType.IN
import app.atemkraft.domain.FreePhaseType.OUT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Freie Folge als lauffähige Übung: Phasen, Füllstand, Dauer, Einordnung, Texte, Sicherheit. */
class FreePatternExerciseTest {

    private fun pattern(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { FreePhase(it.first, it.second) })

    private fun template(label: String) = FreePatternTemplates.all.single { it.label == label }.pattern

    private fun exercise(pattern: FreePattern, minutes: Int? = null) = RandomPatternGenerator.exerciseFrom(pattern, "muster-1", "Eigenes Muster ${pattern.numbers()}", minutes)

    private val intense = pattern(IN to 4, HOLD to 12, OUT to 6)

    /** Vorlagen, Umwandlungen aus Tagesmustern und Randfälle. */
    private val samples: List<FreePattern> = FreePatternTemplates.all.map { it.pattern } +
        (0L until 5L).map { RandomPatternGenerator.forSeed(it).spec.toFreePattern() } +
        listOf(
            pattern(IN to 1, OUT to 1),
            pattern(OUT to 6, IN to 4),
            pattern(HOLD to 2, IN to 4, OUT to 4),
            intense,
            FreePattern(List(6) { FreePhase(IN, 30) } + List(6) { FreePhase(OUT, 30) }),
        )

    @Test
    fun `Timeline lauffaehig fuer alle Beispiele, Dauer und Rueckweg zur Folge`() {
        samples.forEach { p ->
            val exercise = exercise(p)
            val timeline = exercise.buildTimeline()
            assertEquals("$p", p.phases.size * exercise.segments.single().repeat, timeline.size)
            assertTrue(exercise.guided)
            assertTrue(exercise.generated)
            assertEquals(1, exercise.rounds)
            assertEquals(p.totalSeconds, exercise.cycleSeconds())
            assertEquals(p, RandomPatternGenerator.freePatternOf(exercise))
        }
        // Die Dauer steckt in der Wiederholung; ohne Angabe gilt die Vorgabe um 5 Minuten.
        assertEquals(5, exercise(template("Box"), 5).defaultMinutes())
        assertTrue(exercise(template("Box")).defaultMinutes() in 4..6)
        assertNull(RandomPatternGenerator.freePatternOf(RandomPatternGenerator.forSeed(3).exercise.copy(generated = false)))
    }

    @Test
    fun `Phasen und Fuellstand folgen der Folge`() {
        val viloma = exercise(template("Viloma I")).segments.single().phases
        assertEquals(
            listOf(
                PhaseType.INHALE,
                PhaseType.HOLD_FULL,
                PhaseType.INHALE,
                PhaseType.HOLD_FULL,
                PhaseType.INHALE,
                PhaseType.HOLD_FULL,
                PhaseType.EXHALE,
            ),
            viloma.map { it.type },
        )
        assertEquals(listOf("1. Stufe", "Pause", "2. Stufe", "Pause", "3. Stufe", "voll", null), viloma.map { it.note })
        assertEquals(1f / 3f, viloma[0].level!!.to, 1e-5f)
        assertEquals(1f / 3f, viloma[1].level!!.from, 1e-5f)
        assertEquals(1f, viloma[4].level!!.to, 1e-5f)
        // Halten oben und ganzes Ausatmen brauchen keinen Füllstand (Phasentyp genügt).
        assertNull(viloma[5].level)
        assertNull(viloma[6].level)

        val box = exercise(template("Box")).segments.single().phases
        assertEquals(listOf(PhaseType.INHALE, PhaseType.HOLD_FULL, PhaseType.EXHALE, PhaseType.HOLD_EMPTY), box.map { it.type })
        assertEquals(listOf(null, "voll", null, "leer"), box.map { it.note })
        assertTrue(box.all { (it.duration as PhaseDuration.Fixed).millis == 4000L })

        // Ein 4 · Aus 6 · Ein 4: das letzte und das erste Ein sind zwei Stufen eines Atemzugs.
        val wrap = exercise(pattern(IN to 4, OUT to 6, IN to 4)).segments.single().phases
        assertEquals(listOf("2. Stufe", null, "1. Stufe"), wrap.map { it.note })
    }

    /** Einordnung in die Atemlehre nach den Summen; Charakter und Lore folgen ihr. */
    @Test
    fun `Einordnung nach Summen`() {
        val cases = mapOf(
            template("4·6") to AtemlehreRefs("langhana", emptyList()),
            template("Box") to AtemlehreRefs("samana", listOf("halten-voll", "halten-leer")),
            pattern(IN to 6, OUT to 4) to AtemlehreRefs("brmhana", emptyList()),
            pattern(HOLD to 2, IN to 4, OUT to 6) to AtemlehreRefs("langhana", listOf("halten-leer")),
        )
        cases.forEach { (p, refs) ->
            assertEquals("$p", refs, RandomPatternGenerator.atemlehreRefs(p))
            val exercise = exercise(p)
            assertEquals("$p", refs.patternId == "brmhana", exercise.activating)
            assertEquals("$p", refs.morePatternIds, exercise.lore!!.morePatternIds)
        }
    }

    /** Freie Folgen kennen die Stil-Regeln der Tagesmuster nicht und versprechen nichts. */
    @Test
    fun `Texte ehrlich ohne Stil-Regeln`() {
        (FreePatternTemplates.all.map { it.pattern } + intense).forEach { p ->
            val exercise = exercise(p)
            val texts = listOfNotNull(exercise.shortDescription, exercise.effect, exercise.effectDetail, exercise.instructionHint)
            texts.forEach { text ->
                listOf("Leitplanken", "Ausatmen ist mindestens so lang", "heute", "zufällig").forEach { assertFalse(text, text.contains(it)) }
            }
            assertTrue(exercise.effectDetail!!.contains("Eine Studienlage zu genau diesem Muster gibt es nicht."))
            assertTrue("Teaser ≤ 95 Zeichen", exercise.effect.length <= 95)
            assertTrue("Lore-Teaser ≤ 95 Zeichen", exercise.lore!!.teaser.length <= 95)
            // Bauchatmung als Einladung nur bei ruhigen, nicht intensiven Mustern.
            assertEquals("$p", !exercise.activating && !p.isIntense, exercise.instructionHint!!.contains("Bauch"))
        }
    }

    @Test
    fun `nur intensive Muster sind caution mit Schwindel-Satz`() {
        samples.filterNot { it.isIntense }.forEach { p ->
            val exercise = exercise(p)
            assertFalse("$p", exercise.caution)
            assertTrue("$p", exercise.cautions.isEmpty())
        }
        val exercise = exercise(intense)
        assertTrue(exercise.caution)
        assertEquals(FreePattern.DIZZINESS_SENTENCE, exercise.cautions.first())
        // Wie Wim Hof und Viloma: ärztliche Abklärung, bei langem Halten „nie pressen“.
        assertTrue(exercise.cautions.any { it.contains("ärztlich abklären") && it.contains("Schwangerschaft") })
        assertTrue(exercise.cautions.any { it.contains("nie pressen") })
        assertTrue(exercise.shortDescription.endsWith(FreePattern.DIZZINESS_SENTENCE))
    }
}
