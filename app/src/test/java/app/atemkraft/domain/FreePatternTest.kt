package app.atemkraft.domain

import app.atemkraft.domain.FreePhaseType.HOLD
import app.atemkraft.domain.FreePhaseType.IN
import app.atemkraft.domain.FreePhaseType.OUT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Freie Phasenfolge („Frei gestalten“): Regeln, Stufen, Intensität, Textform, Vorlagen, Bearbeiten. */
class FreePatternTest {

    private fun pattern(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { FreePhase(it.first, it.second) })

    private fun template(label: String) = FreePatternTemplates.all.single { it.label == label }.pattern

    private fun assertLevels(expected: List<Pair<Double, Double>>, actual: List<BreathLevel>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEachIndexed { i, (want, got) ->
            assertEquals("Phase $i von", want.first, got.from.toDouble(), 1e-5)
            assertEquals("Phase $i bis", want.second, got.to.toDouble(), 1e-5)
        }
    }

    // ---- Regeln ----

    @Test
    fun `gueltige Grenzfaelle`() {
        assertTrue(pattern(IN to 1, OUT to 1).isValid())
        assertTrue(pattern(IN to 30, OUT to 30).isValid())
        assertTrue(FreePattern(List(6) { FreePhase(IN, 4) } + List(6) { FreePhase(OUT, 4) }).isValid())
        assertTrue(pattern(OUT to 6, IN to 4).isValid())
    }

    @Test
    fun `Regelverstoesse werden einzeln benannt`() {
        assertEquals(listOf(FreePatternProblem.TOO_FEW_PHASES, FreePatternProblem.NO_EXHALE), pattern(IN to 4).validate())
        assertEquals(
            listOf(FreePatternProblem.TOO_MANY_PHASES),
            FreePattern(List(7) { FreePhase(IN, 4) } + List(6) { FreePhase(OUT, 4) }).validate(),
        )
        assertEquals(listOf(FreePatternProblem.SECONDS_OUT_OF_RANGE), pattern(IN to 0, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.SECONDS_OUT_OF_RANGE), pattern(IN to 31, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.SECONDS_OUT_OF_RANGE), pattern(IN to -1, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.NO_INHALE), pattern(HOLD to 4, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.NO_EXHALE), pattern(IN to 4, HOLD to 4).validate())
        assertFalse(FreePattern(emptyList()).isValid())
    }

    // ---- Stufen / Füllstand ----

    @Test
    fun `einfaches Muster laeuft leer bis voll und zurueck`() {
        val box = template("Box")
        assertLevels(listOf(0.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0, 0.0 to 0.0), box.levels())
        assertEquals(listOf("Einatmen", "Halten voll", "Ausatmen", "Halten leer"), box.phases.indices.map(box::labelAt))
    }

    @Test
    fun `Viloma I steigt in Dritteln mit Pausen`() {
        val viloma = template("Viloma I")
        val third = 1.0 / 3
        assertLevels(
            listOf(
                0.0 to third,
                third to third,
                third to 2 * third,
                2 * third to 2 * third,
                2 * third to 1.0,
                1.0 to 1.0,
                1.0 to 0.0,
            ),
            viloma.levels(),
        )
        assertEquals(HoldPosition.BETWEEN, viloma.holdPositionAt(1))
        assertEquals(HoldPosition.FULL, viloma.holdPositionAt(5))
        assertNull(viloma.holdPositionAt(0))
        assertEquals("Halten", viloma.labelAt(1))
        assertEquals("Halten voll", viloma.labelAt(5))
    }

    @Test
    fun `Seufzer teilt den Anstieg auf zwei Ein`() {
        assertLevels(listOf(0.0 to 0.5, 0.5 to 1.0, 1.0 to 0.0), template("Seufzer").levels())
    }

    @Test
    fun `Start mit Aus beginnt voll`() {
        assertLevels(listOf(1.0 to 0.0, 0.0 to 1.0), pattern(OUT to 6, IN to 4).levels())
        // Halten vor dem ersten Aus steht oben.
        val holdFirst = pattern(HOLD to 2, OUT to 3, OUT to 3, IN to 4)
        assertLevels(listOf(1.0 to 1.0, 1.0 to 0.5, 0.5 to 0.0, 0.0 to 1.0), holdFirst.levels())
        assertEquals("Halten voll", holdFirst.labelAt(0))
        // Halten vor dem ersten Ein steht unten.
        assertEquals("Halten leer", pattern(HOLD to 2, IN to 4, OUT to 4).labelAt(0))
    }

    @Test
    fun `Wechsel endet jeden Lauf an der Grenze`() {
        assertLevels(
            listOf(0.0 to 1.0, 1.0 to 0.0, 0.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0),
            template("Wechsel").levels(),
        )
    }

    // ---- Atemzüge, Intensität ----

    @Test
    fun `Atemzuege zaehlen Laeufe von Ein`() {
        assertEquals(1, template("Viloma I").breathCount())
        assertEquals(1, template("Seufzer").breathCount())
        assertEquals(2, template("Wechsel").breathCount())
        assertEquals("6", template("4·6").breathsPerMinuteText())
        assertEquals("3,3", template("Viloma I").breathsPerMinuteText())
        assertEquals(6.0, template("4·6").breathsPerMinute(), 1e-9)
    }

    @Test
    fun `Intensitaet Halten voll erst ueber 10 s`() {
        assertTrue(pattern(IN to 4, HOLD to 10, OUT to 8).intensityReasons().isEmpty())
        assertEquals(listOf(IntensityReason.LONG_HOLD_FULL), pattern(IN to 4, HOLD to 11, OUT to 8).intensityReasons())
    }

    @Test
    fun `Intensitaet Halten leer erst ueber 8 s`() {
        assertTrue(pattern(IN to 4, OUT to 8, HOLD to 8).intensityReasons().isEmpty())
        assertEquals(listOf(IntensityReason.LONG_HOLD_EMPTY), pattern(IN to 4, OUT to 8, HOLD to 9).intensityReasons())
    }

    @Test
    fun `Halten ab halb voll zaehlt als nach dem Einatmen`() {
        // Seufzer-artig: Pause bei halb voll zählt zur oberen Schwelle (wie im Design).
        val half = pattern(IN to 2, HOLD to 9, IN to 2, OUT to 6)
        assertTrue(half.intensityReasons().isEmpty())
        // Viloma II: Pause bei einem Drittel zählt zur unteren Schwelle.
        val third = pattern(IN to 6, OUT to 2, HOLD to 2, OUT to 2, HOLD to 9, OUT to 2)
        assertEquals(listOf(IntensityReason.LONG_HOLD_EMPTY), third.intensityReasons())
    }

    @Test
    fun `Intensitaet Tempo erst ueber 20 Atemzuege pro Minute`() {
        // 3 s je Atemzug = genau 20/min: noch nicht intensiv.
        assertTrue(pattern(IN to 1, OUT to 2).intensityReasons().isEmpty())
        // 2 s je Atemzug = 30/min.
        assertEquals(listOf(IntensityReason.FAST_BREATHING), pattern(IN to 1, OUT to 1).intensityReasons())
        // Zwei Atemzüge in 5 s = 24/min.
        assertTrue(pattern(IN to 1, OUT to 1, IN to 1, OUT to 2).isIntense)
    }

    @Test
    fun `mehrere Gruende in fester Reihenfolge und Hinweistext`() {
        val all = pattern(IN to 1, HOLD to 11, OUT to 1, HOLD to 9, IN to 1, OUT to 1, IN to 1, OUT to 1, IN to 1, OUT to 1, IN to 1, OUT to 1)
        // 5 Atemzüge in 30 s = 10/min: nur die Haltegründe.
        assertEquals(listOf(IntensityReason.LONG_HOLD_FULL, IntensityReason.LONG_HOLD_EMPTY), all.intensityReasons())
        val note = all.intensityNote()!!
        assertTrue(note, note.startsWith("Intensiv: Halten nach dem Einatmen über 10 s, Halten nach dem Ausatmen über 8 s."))
        assertTrue(note.endsWith(FreePattern.DIZZINESS_SENTENCE))
        assertNull(template("4·6").intensityNote())
    }

    @Test
    fun `Vorlagen sind gueltig und keine ist intensiv`() {
        assertEquals(listOf("4·6", "Box", "4-7-8", "Seufzer", "Viloma I", "Wechsel"), FreePatternTemplates.all.map { it.label })
        FreePatternTemplates.all.forEach {
            assertTrue(it.label, it.pattern.isValid())
            assertFalse(it.label, it.pattern.isIntense)
        }
        assertEquals(template("4·6"), FreePattern.DEFAULT)
        assertEquals("Box", FreePatternTemplates.matching(pattern(IN to 4, HOLD to 4, OUT to 4, HOLD to 4))?.label)
        assertNull(FreePatternTemplates.matching(pattern(IN to 4, OUT to 7)))
    }

    // ---- Textform ----

    @Test
    fun `Textform Roundtrip fuer alle Vorlagen`() {
        assertEquals("in:4,hold:1,out:6", pattern(IN to 4, HOLD to 1, OUT to 6).toCompact())
        FreePatternTemplates.all.forEach { assertEquals(it.pattern, FreePattern.parse(it.pattern.toCompact())) }
    }

    @Test
    fun `Parser ist robust gegen Leerzeichen und Grossschreibung`() {
        assertEquals(pattern(IN to 4, OUT to 6), FreePattern.parse(" IN : 4 , Out:06 "))
    }

    @Test
    fun `Parser lehnt Ungueltiges ab`() {
        listOf(
            null,
            "",
            "in:4",
            "in:4,out:",
            "in:4,out:6,",
            "in:4;out:6",
            "in:4,breathe:6",
            "in:4.5,out:6",
            "in:-1,out:6",
            "in:+4,out:6",
            "in:0,out:6",
            "in:31,out:6",
            "in:999,out:6",
            "in:4:1,out:6",
            "in:4,hold:4",
            "hold:4,out:6",
            "in:٤,out:6",
            List(13) { "in:1" }.joinToString(",") + ",out:1",
            "in:4,out:6" + " ".repeat(300),
        ).forEach { assertNull("$it", FreePattern.parse(it)) }
    }

    // ---- Umwandlung aus dem Muster des Tages ----

    @Test
    fun `PatternSpec wird zur freien Folge`() {
        val spec = PatternSpec(inhale = 4.0, holdFull = 2.0, exhale = 6.0, holdEmpty = 1.0, activating = false)
        assertEquals(pattern(IN to 4, HOLD to 2, OUT to 6, HOLD to 1), spec.toFreePattern())
        val noHold = PatternSpec(inhale = 4.5, holdFull = null, exhale = 5.0, holdEmpty = null, activating = true)
        assertEquals(pattern(IN to 5, OUT to 5), noHold.toFreePattern())
        (0L until 300L).forEach { seed ->
            val daily = RandomPatternGenerator.forSeed(seed).spec
            val free = daily.toFreePattern()
            assertTrue("$daily", free.isValid())
            assertFalse("$daily", free.isIntense)
            assertEquals(RandomPatternGenerator.numbersOf(daily), free.numbers())
            // Gleiche Einordnung wie das Tagesmuster.
            assertEquals(RandomPatternGenerator.atemlehreRefs(daily), RandomPatternGenerator.atemlehreRefs(free))
        }
    }

    // ---- Bearbeiten ----

    @Test
    fun `Sekunden werden auf 1 bis 30 geklemmt`() {
        val base = template("4·6")
        assertEquals(30, base.withSeconds(0, 99).phases[0].seconds)
        assertEquals(1, base.withSeconds(1, 0).phases[1].seconds)
        assertEquals(base, base.withSeconds(5, 3))
    }

    @Test
    fun `Einfuegen hinter der Auswahl oder am Ende`() {
        val base = template("4·6")
        assertEquals(pattern(IN to 4, HOLD to 2, OUT to 6), base.inserted(0, HOLD))
        assertEquals(pattern(IN to 4, OUT to 6, IN to 4), base.inserted(null, IN))
        assertEquals(2, base.insertIndex(null))
        assertEquals(1, base.insertIndex(0))
        val full = FreePattern(List(6) { FreePhase(IN, 1) } + List(6) { FreePhase(OUT, 1) })
        assertFalse(full.canAdd)
        assertEquals(full, full.inserted(null, OUT))
    }

    @Test
    fun `Entfernen nur wenn die Folge gueltig bleibt`() {
        val base = template("4·6")
        assertFalse(base.canRemove(0))
        assertEquals(base, base.removed(1))
        val seufzer = template("Seufzer")
        assertTrue(seufzer.canRemove(0))
        assertFalse(seufzer.canRemove(2))
        assertEquals(pattern(IN to 1, OUT to 6), seufzer.removed(0))
        assertFalse(seufzer.canRemove(7))
    }

    @Test
    fun `Achse waechst mit dem Muster in geraden Sekunden`() {
        assertEquals(16, template("4·6").axisSeconds())
        assertEquals(12, pattern(IN to 2, OUT to 2).axisSeconds())
        assertEquals(26, template("Wechsel").axisSeconds())
        assertEquals(26, pattern(IN to 4, HOLD to 7, OUT to 8).axisSeconds())
    }

    @Test
    fun `Worte und Ansage`() {
        val p = pattern(IN to 4, HOLD to 1, OUT to 6)
        assertEquals("4·1·6", p.numbers())
        assertEquals("Einatmen 4\u00A0s${DOT_SEPARATOR}Halten voll 1\u00A0s${DOT_SEPARATOR}Ausatmen 6\u00A0s", p.hint())
        assertEquals("Einatmen 4 Sekunden, Halten voll 1 Sekunde, Ausatmen 6 Sekunden", p.spokenDescription())
    }
}
