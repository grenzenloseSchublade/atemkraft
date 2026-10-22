package app.atemkraft.domain

import app.atemkraft.domain.FreePhaseType.HOLD
import app.atemkraft.domain.FreePhaseType.IN
import app.atemkraft.domain.FreePhaseType.OUT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        (0L until 40L).map { RandomPatternGenerator.forSeed(it).spec.toFreePattern() } +
        listOf(
            pattern(IN to 1, OUT to 1),
            pattern(OUT to 6, IN to 4),
            pattern(HOLD to 2, IN to 4, OUT to 4),
            intense,
            FreePattern(List(6) { FreePhase(IN, 30) } + List(6) { FreePhase(OUT, 30) }),
        )

    @Test
    fun `Timeline lauffaehig fuer alle Beispiele`() {
        samples.forEach { p ->
            val exercise = exercise(p)
            val timeline = exercise.buildTimeline()
            assertTrue("$p", timeline.isNotEmpty())
            assertEquals("$p", p.phases.size * exercise.segments.single().repeat, timeline.size)
            assertTrue(exercise.guided)
            assertTrue(exercise.generated)
            assertEquals(1, exercise.rounds)
            assertEquals(p.totalSeconds, exercise.cycleSeconds())
        }
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
        assertTrue(box.all { it.level == null })
        assertTrue(box.all { (it.duration as PhaseDuration.Fixed).millis == 4000L })

        val startOut = exercise(pattern(OUT to 6, IN to 4)).segments.single().phases
        assertNull(startOut[0].level)
        assertNull(startOut[1].level)
    }

    @Test
    fun `Dauer folgt minutes`() {
        FreePatternTemplates.all.forEach { tpl ->
            listOf(1, 3, 5, 10, 30).forEach { minutes ->
                assertEquals("${tpl.label} $minutes", minutes, exercise(tpl.pattern, minutes).defaultMinutes())
            }
            assertTrue(tpl.label, exercise(tpl.pattern).defaultMinutes() in 4..6)
        }
    }

    @Test
    fun `Rueckweg zur freien Folge`() {
        samples.forEach { p -> assertEquals(p, RandomPatternGenerator.freePatternOf(exercise(p))) }
        val daily = RandomPatternGenerator.forSeed(3)
        assertEquals(daily.spec.toFreePattern(), RandomPatternGenerator.freePatternOf(daily.exercise))
        val builtIn = daily.exercise.copy(generated = false)
        assertNull(RandomPatternGenerator.freePatternOf(builtIn))
    }

    @Test
    fun `Einordnung nach Summen`() {
        assertEquals(AtemlehreRefs("langhana", emptyList()), RandomPatternGenerator.atemlehreRefs(template("4·6")))
        assertEquals(AtemlehreRefs("samana", listOf("halten-voll", "halten-leer")), RandomPatternGenerator.atemlehreRefs(template("Box")))
        assertEquals(AtemlehreRefs("langhana", listOf("halten-voll")), RandomPatternGenerator.atemlehreRefs(template("4-7-8")))
        // Viloma I: 6 s ein, 6 s aus; die Pausen zwischen den Stufen verweisen auf keine Karte.
        assertEquals(AtemlehreRefs("samana", listOf("halten-voll")), RandomPatternGenerator.atemlehreRefs(template("Viloma I")))
        assertEquals(AtemlehreRefs("langhana", listOf("halten-voll")), RandomPatternGenerator.atemlehreRefs(template("Wechsel")))
        assertEquals(AtemlehreRefs("brmhana", emptyList()), RandomPatternGenerator.atemlehreRefs(pattern(IN to 6, OUT to 4)))
        assertEquals(
            AtemlehreRefs("langhana", listOf("halten-leer")),
            RandomPatternGenerator.atemlehreRefs(pattern(HOLD to 2, IN to 4, OUT to 6)),
        )
    }

    @Test
    fun `Charakter und Lore aus der Einordnung`() {
        val active = exercise(pattern(IN to 6, OUT to 4))
        assertTrue(active.activating)
        assertEquals("brmhana", active.lore!!.patternId)
        assertFalse(exercise(template("4·6")).activating)
        val box = exercise(template("Box"))
        assertEquals(listOf("halten-voll", "halten-leer"), box.lore!!.morePatternIds)
        assertTrue(box.lore!!.detail.startsWith("Du hast das Muster selbst zusammengestellt"))
        assertTrue(box.lore!!.detail.contains("Halten nach dem Ausatmen"))
    }

    @Test
    fun `Texte ehrlich ohne Stil-Regeln`() {
        samples.forEach { p ->
            val exercise = exercise(p)
            val texts = listOfNotNull(exercise.shortDescription, exercise.effect, exercise.effectDetail, exercise.instructionHint)
            texts.forEach { text ->
                assertFalse(text, text.contains("Leitplanken"))
                assertFalse(text, text.contains("Ausatmen ist mindestens so lang"))
                assertFalse(text, text.contains("heute"))
                assertFalse(text, text.contains("zufällig"))
            }
            assertTrue(exercise.shortDescription.contains("von dir selbst zusammengestellt"))
            assertTrue(exercise.effectDetail!!.contains("Eine Studienlage zu genau diesem Muster gibt es nicht."))
            assertTrue("Teaser ≤ 95 Zeichen", exercise.effect.length <= 95)
            assertTrue("Lore-Teaser ≤ 95 Zeichen", exercise.lore!!.teaser.length <= 95)
            assertTrue(exercise.instructionHint!!.startsWith(p.hint()))
        }
        val calm = exercise(template("4·6"))
        assertEquals(
            "Einatmen 4\u00A0s${DOT_SEPARATOR}Ausatmen 6\u00A0s. 6 Atemzüge pro Minute, von dir selbst zusammengestellt.",
            calm.shortDescription,
        )
        assertTrue(calm.instructionHint!!.contains("Bauch"))
        assertFalse(exercise(pattern(IN to 6, OUT to 4)).instructionHint!!.contains("Bauch"))
    }

    @Test
    fun `nur intensive Muster sind caution mit Schwindel-Satz`() {
        samples.filterNot { it.isIntense }.forEach { p ->
            val exercise = exercise(p)
            assertFalse("$p", exercise.caution)
            assertTrue("$p", exercise.cautions.isEmpty())
            assertFalse(exercise.effect.contains(FreePattern.DIZZINESS_SENTENCE))
        }
        val exercise = exercise(intense)
        assertTrue(exercise.caution)
        assertEquals(FreePattern.DIZZINESS_SENTENCE, exercise.cautions.first())
        assertTrue(exercise.cautions.any { it.contains("Halten nach dem Einatmen über 10 s") })
        assertTrue(exercise.effect.contains(FreePattern.DIZZINESS_SENTENCE))
        assertTrue(exercise.shortDescription.endsWith(FreePattern.DIZZINESS_SENTENCE))
        assertTrue(exercise.effectDetail!!.contains("Sicherheitshinweis"))
        assertFalse(exercise.instructionHint!!.contains("Bauch"))
        assertNotNull(intense.intensityNote())
    }

    @Test
    fun `ungueltige Folge wird abgewiesen`() {
        val invalid = pattern(IN to 4)
        val thrown = runCatching { RandomPatternGenerator.exerciseFrom(invalid, "x", "x") }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException)
    }
}
