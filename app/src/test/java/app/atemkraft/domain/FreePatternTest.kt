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
    fun `Regeln - Grenzfaelle gueltig, Verstoesse einzeln benannt`() {
        assertTrue(pattern(IN to 1, OUT to 1).isValid())
        assertTrue(pattern(IN to 30, OUT to 30).isValid())
        assertTrue(FreePattern(List(6) { FreePhase(IN, 4) } + List(6) { FreePhase(OUT, 4) }).isValid())
        assertTrue(pattern(OUT to 6, IN to 4).isValid())

        assertEquals(listOf(FreePatternProblem.TOO_FEW_PHASES, FreePatternProblem.NO_EXHALE), pattern(IN to 4).validate())
        assertEquals(
            listOf(FreePatternProblem.TOO_MANY_PHASES),
            FreePattern(List(7) { FreePhase(IN, 4) } + List(6) { FreePhase(OUT, 4) }).validate(),
        )
        assertEquals(listOf(FreePatternProblem.SECONDS_OUT_OF_RANGE), pattern(IN to 0, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.SECONDS_OUT_OF_RANGE), pattern(IN to 31, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.NO_INHALE), pattern(HOLD to 4, OUT to 4).validate())
        assertEquals(listOf(FreePatternProblem.NO_EXHALE), pattern(IN to 4, HOLD to 4).validate())
        assertFalse(FreePattern(emptyList()).isValid())
    }

    // ---- Stufen / Füllstand ----

    @Test
    fun `Fuellstand der Vorlagen - Box, Viloma I, Seufzer, Wechsel`() {
        val third = 1.0 / 3
        mapOf(
            "Box" to listOf(0.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0, 0.0 to 0.0),
            "Viloma I" to listOf(0.0 to third, third to third, third to 2 * third, 2 * third to 2 * third, 2 * third to 1.0, 1.0 to 1.0, 1.0 to 0.0),
            "Seufzer" to listOf(0.0 to 0.5, 0.5 to 1.0, 1.0 to 0.0),
            "Wechsel" to listOf(0.0 to 1.0, 1.0 to 0.0, 0.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0),
        ).forEach { (label, levels) -> assertLevels(levels, template(label).levels()) }

        val box = template("Box")
        assertEquals(listOf("Einatmen", "Halten voll", "Ausatmen", "Halten leer"), box.phases.indices.map(box::labelAt))
        // Viloma: die Pausen zwischen den Stufen heißen nur „Halten“.
        val viloma = template("Viloma I")
        assertEquals(HoldPosition.BETWEEN, viloma.holdPositionAt(1))
        assertEquals(HoldPosition.FULL, viloma.holdPositionAt(5))
        assertNull(viloma.holdPositionAt(0))
        assertEquals("Halten", viloma.labelAt(1))
        assertEquals("Halten voll", viloma.labelAt(5))
    }

    @Test
    fun `Start mit Aus oder Halten und zyklische Kontinuitaet`() {
        assertLevels(listOf(1.0 to 0.0, 0.0 to 1.0), pattern(OUT to 6, IN to 4).levels())
        // Halten vor dem ersten Aus steht oben, vor dem ersten Ein unten.
        val holdFirst = pattern(HOLD to 2, OUT to 3, OUT to 3, IN to 4)
        assertLevels(listOf(1.0 to 1.0, 1.0 to 0.5, 0.5 to 0.0, 0.0 to 1.0), holdFirst.levels())
        assertEquals("Halten voll", holdFirst.labelAt(0))
        assertEquals("Halten leer", pattern(HOLD to 2, IN to 4, OUT to 4).labelAt(0))

        // Ein 4 · Aus 6 · Ein 4: letztes und erstes Ein sind ein Atemzug in zwei Stufen, kein Sprung.
        val wrap = pattern(IN to 4, OUT to 6, IN to 4)
        assertLevels(listOf(0.5 to 1.0, 1.0 to 0.0, 0.0 to 0.5), wrap.levels())
        // Über alle Folgen: Das Ende eines Durchlaufs ist der Anfang des nächsten.
        (listOf(wrap, pattern(OUT to 3, IN to 4, OUT to 3), holdFirst) + FreePatternTemplates.all.map { it.pattern }).forEach { free ->
            val levels = free.levels()
            levels.indices.forEach { i -> assertEquals("$free Phase $i", levels[i].to, levels[(i + 1) % levels.size].from, 1e-5f) }
        }
    }

    // ---- Atemzüge, Intensität ----

    @Test
    fun `Atemzuege zaehlen Laeufe von Ein`() {
        assertEquals(1, template("Viloma I").breathCount())
        assertEquals(1, template("Seufzer").breathCount())
        assertEquals(2, template("Wechsel").breathCount())
        assertEquals("6", template("4·6").breathsPerMinuteText())
        assertEquals("3,3", template("Viloma I").breathsPerMinuteText())
        // Ein Lauf über das Folgenende zählt einmal; der Kreis beginnt beim ersten ganzen Ein.
        val wrap = pattern(IN to 4, OUT to 6, IN to 4)
        assertEquals(1, wrap.breathCount())
        assertEquals(listOf(1, 2, 0), wrap.cycleOrder())
    }

    /** Sicherheitsgrenzen: Halten voll über 10 s, Halten leer über 8 s, über 20 Atemzüge pro Minute. */
    @Test
    fun `Intensitaets-Schwellen samt Hinweistext`() {
        assertTrue(pattern(IN to 4, HOLD to 10, OUT to 8).intensityReasons().isEmpty())
        assertEquals(listOf(IntensityReason.LONG_HOLD_FULL), pattern(IN to 4, HOLD to 11, OUT to 8).intensityReasons())
        assertTrue(pattern(IN to 4, OUT to 8, HOLD to 8).intensityReasons().isEmpty())
        assertEquals(listOf(IntensityReason.LONG_HOLD_EMPTY), pattern(IN to 4, OUT to 8, HOLD to 9).intensityReasons())
        // 3 s je Atemzug = genau 20/min: noch nicht intensiv; auch über das Folgenende hinweg.
        assertTrue(pattern(IN to 1, OUT to 2).intensityReasons().isEmpty())
        assertTrue(pattern(IN to 1, OUT to 1, IN to 1).intensityReasons().isEmpty())
        assertEquals(listOf(IntensityReason.FAST_BREATHING), pattern(IN to 1, OUT to 1).intensityReasons())

        val all = pattern(IN to 1, HOLD to 11, OUT to 1, HOLD to 9, IN to 1, OUT to 1, IN to 1, OUT to 1, IN to 1, OUT to 1, IN to 1, OUT to 1)
        // 5 Atemzüge in 30 s = 10/min: nur die Haltegründe, in fester Reihenfolge.
        assertEquals(listOf(IntensityReason.LONG_HOLD_FULL, IntensityReason.LONG_HOLD_EMPTY), all.intensityReasons())
        val note = all.intensityNote()!!
        // Ehrlich: Der Hinweis lässt sich in den Einstellungen abschalten.
        assertTrue(note, note.contains("solange er in den Einstellungen nicht abgeschaltet ist"))
        assertTrue(note.endsWith(FreePattern.DIZZINESS_SENTENCE))
        assertNull(template("4·6").intensityNote())
    }

    @Test
    fun `Halten wird nach Fuellstand zugeordnet und am Stueck zusammengezaehlt`() {
        // Pause bei halb voll zählt zur oberen Schwelle, bei einem Drittel (Viloma II) zur unteren.
        assertTrue(pattern(IN to 2, HOLD to 9, IN to 2, OUT to 6).intensityReasons().isEmpty())
        assertEquals(listOf(IntensityReason.LONG_HOLD_EMPTY), pattern(IN to 6, OUT to 2, HOLD to 2, OUT to 2, HOLD to 9, OUT to 2).intensityReasons())
        // Zweimal 10 s voll = 20 s, zweimal 8 s leer = 16 s am Stück.
        assertEquals(listOf(IntensityReason.LONG_HOLD_FULL), pattern(IN to 4, HOLD to 10, HOLD to 10, OUT to 4).intensityReasons())
        assertEquals(listOf(IntensityReason.LONG_HOLD_EMPTY), pattern(OUT to 4, HOLD to 8, HOLD to 8, IN to 4).intensityReasons())
        // Halten am Ende und am Anfang stoßen beim Wiederholen aneinander: 10 s leer.
        assertEquals(listOf(IntensityReason.LONG_HOLD_EMPTY), FreePattern.parse("hold:5,in:4,out:4,hold:5")!!.intensityReasons())
        // Getrennte Halten zählen einzeln.
        assertTrue(pattern(IN to 4, HOLD to 6, OUT to 4, HOLD to 6).intensityReasons().isEmpty())
    }

    @Test
    fun `Vorlagen sind gueltig und keine ist intensiv`() {
        FreePatternTemplates.all.forEach {
            assertTrue(it.label, it.pattern.isValid())
            assertFalse(it.label, it.pattern.isIntense)
        }
        assertEquals("Box", FreePatternTemplates.matching(pattern(IN to 4, HOLD to 4, OUT to 4, HOLD to 4))?.label)
        assertNull(FreePatternTemplates.matching(pattern(IN to 4, OUT to 7)))
    }

    // ---- Textform ----

    @Test
    fun `Textform Roundtrip fuer alle Vorlagen`() {
        assertEquals("in:4,hold:1,out:6", pattern(IN to 4, HOLD to 1, OUT to 6).toCompact())
        FreePatternTemplates.all.forEach { assertEquals(it.pattern, FreePattern.parse(it.pattern.toCompact())) }
        // Robust gegen Leerzeichen, Großschreibung und führende Nullen.
        assertEquals(pattern(IN to 4, OUT to 6), FreePattern.parse(" IN : 4 , Out:06 "))
    }

    @Test
    fun `Parser lehnt Ungueltiges ab`() {
        listOf(
            "",
            "in:4",
            "in:4,breathe:6",
            "in:4.5,out:6",
            "in:0,out:6",
            "in:31,out:6",
            "in:4,hold:4",
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
        // Halbe Sekunden älterer Muster werden gerundet.
        assertEquals(pattern(IN to 5, OUT to 5), PatternSpec(4.5, null, 5.0, null, activating = true).toFreePattern())
        creationRuleSpecs.forEach { daily ->
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
    fun `Bearbeiten - Klemmen, Einfuegen, Entfernen`() {
        val base = template("4·6")
        assertEquals(30, base.withSeconds(0, 99).phases[0].seconds)
        assertEquals(1, base.withSeconds(1, 0).phases[1].seconds)
        assertEquals(base, base.withSeconds(5, 3))

        assertEquals(pattern(IN to 4, HOLD to 2, OUT to 6), base.inserted(0, HOLD))
        assertEquals(pattern(IN to 4, OUT to 6, IN to 4), base.inserted(null, IN))
        val full = FreePattern(List(6) { FreePhase(IN, 1) } + List(6) { FreePhase(OUT, 1) })
        assertFalse(full.canAdd)
        assertEquals(full, full.inserted(null, OUT))

        assertFalse(base.canRemove(0))
        assertEquals(base, base.removed(1))
        val seufzer = template("Seufzer")
        assertTrue(seufzer.canRemove(0))
        assertFalse(seufzer.canRemove(2))
        assertEquals(pattern(IN to 1, OUT to 6), seufzer.removed(0))
    }

    @Test
    fun `Worte bleiben zusammen`() {
        val p = pattern(IN to 4, HOLD to 1, OUT to 6)
        assertEquals("4·1·6", p.numbers())
        // Name und Wert bleiben zusammen (geschütztes Leerzeichen), mehrwortige Namen dürfen umbrechen.
        assertEquals("Einatmen\u00A04\u00A0s${DOT_SEPARATOR}Halten voll\u00A01\u00A0s${DOT_SEPARATOR}Ausatmen\u00A06\u00A0s", p.hint())
    }
}
