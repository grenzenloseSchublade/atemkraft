package app.atemkraft.data

import app.atemkraft.domain.EvidenceLevel
import app.atemkraft.domain.EvidenceLevel.LITTLE_STUDIED
import app.atemkraft.domain.EvidenceLevel.STUDIED
import app.atemkraft.domain.EvidenceLevel.WELL_SUPPORTED
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.creationRuleSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEXT-07: Jede eingebaute Übung trägt genau eine Studienlage; die Zuordnung steht hier als
 * Tabelle fest. Wer eine Stufe ändert oder eine Übung ergänzt, ändert diese Tabelle bewusst mit
 * (Kriterien und Hoch-/Herabstufen im STYLEGUIDE, TEXT-07). Das Vorsichts-Etikett ist keine
 * Stufe und wird getrennt festgehalten.
 */
class EvidenceLevelTest {

    private val expected: Map<String, EvidenceLevel> = linkedMapOf(
        "resonanz" to WELL_SUPPORTED,
        "cyclic-sighing" to STUDIED,
        "physiological-sigh" to STUDIED,
        "4-7-8" to STUDIED,
        "box-4-4-4-4" to STUDIED,
        "bhramari" to STUDIED,
        "lippenbremse" to STUDIED,
        "nadi-shodhana" to STUDIED,
        "sky" to STUDIED,
        "buteyko" to STUDIED,
        "wim-hof" to STUDIED,
        "zwerchfell" to STUDIED,
        "sitali" to STUDIED,
        "walking-breath" to LITTLE_STUDIED,
        "nasenatmung" to LITTLE_STUDIED,
        "feueratmung" to LITTLE_STUDIED,
        "viloma-ein" to LITTLE_STUDIED,
        "viloma-aus" to LITTLE_STUDIED,
    )

    @Test
    fun `jede eingebaute Übung hat genau die festgelegte Stufe`() {
        val actual = BuiltInExercises.all.associate { it.id to it.evidence }
        assertEquals(expected.keys.sorted(), actual.keys.sorted())
        expected.forEach { (id, level) -> assertEquals("Studienlage von $id", level, actual[id]) }
    }

    @Test
    fun `Vorsicht nur bei Wim Hof und Feueratmung`() {
        val caution = BuiltInExercises.all.filter { it.caution }.map { it.id }.toSet()
        assertEquals(setOf("wim-hof", "feueratmung"), caution)
    }

    /** Muster des Tages, eigene und gespeicherte Muster sind nicht untersucht (TEXT-07, MUSTER-06). */
    @Test
    fun `generierte, eigene und gespeicherte Muster haben keine Studienlage und keine Vorsicht`() {
        creationRuleSpecs.forEach { spec ->
            PatternOrigin.entries.forEach { origin ->
                val e = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin, 5)
                assertNull("$origin $spec mit Studienlage", e.evidence)
                assertEquals("$origin $spec mit Vorsicht", false, e.caution)
                assertTrue("$origin $spec mit Quellen", e.references.isEmpty())
            }
        }
    }
}
