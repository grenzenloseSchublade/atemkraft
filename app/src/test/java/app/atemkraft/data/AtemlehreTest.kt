package app.atemkraft.data

import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.creationRuleSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEXT-13: Die Yoga-Lehre steht getrennt von der Studienlage. Hier steht fest, welche Übungen
 * einen Abschnitt „Aus der Yoga-Lehre“ tragen (nur yogischer Ursprung, Nutzerentscheidung) und
 * dass jeder Querverweis der Atemlehre aufgeht. Teaser-Form und Zuschreibung statt
 * Wirkbehauptung prüft `ContentRulesTest lore-teaser`.
 */
class AtemlehreTest {

    @Test
    fun `genau die Übungen yogischen Ursprungs tragen Yoga-Lehre`() {
        val withLore = BuiltInExercises.all.filter { it.lore != null }.map { it.id }.toSet()
        assertEquals(
            setOf("bhramari", "sitali", "nadi-shodhana", "feueratmung", "sky", "box-4-4-4-4", "4-7-8", "viloma-ein", "viloma-aus"),
            withLore,
        )
    }

    @Test
    fun `jeder Verweis auf eine Muster-Karte geht auf`() {
        val ids = Atemlehre.patterns.map { it.id }
        assertEquals("doppelte Kennung: ${ids.groupBy { it }.filterValues { it.size > 1 }.keys}", ids.size, ids.toSet().size)
        Atemlehre.patterns.forEach { p ->
            assertTrue("${p.id}: Rhythmus leer oder Dauer ≤ 0", p.rhythm.isNotEmpty() && p.rhythm.all { it.seconds > 0.0 })
        }

        // Übungen → Karten: eingebaute Lehre und jedes erzeugbare Muster (Tagesmuster, Editor).
        val refs = BuiltInExercises.all.mapNotNull { e -> e.lore?.let { e.id to listOf(it.patternId) + it.morePatternIds } } +
            creationRuleSpecs.map { spec ->
                val r = RandomPatternGenerator.atemlehreRefs(spec)
                // Aktivierend heißt Brmhana, ruhig Langhana oder Samana.
                if (spec.activating) assertEquals("$spec", "brmhana", r.patternId) else assertTrue("$spec", r.patternId in setOf("langhana", "samana"))
                "$spec" to listOf(r.patternId) + r.morePatternIds
            }
        refs.forEach { (key, cards) -> cards.forEach { assertNotNull("$key verweist auf unbekannte Karte „$it“", Atemlehre.pattern(it)) } }

        // Karten und Einflüsse → Übungen.
        val known = BuiltInExercises.all.map { it.id }.toSet() + RandomPatternGenerator.ID
        val referenced = Atemlehre.patterns.flatMap { p -> p.exerciseIds.map { "${p.id} → $it" to it } } +
            Atemlehre.influences.flatMap { inf -> inf.exerciseIds.map { "${inf.label} → $it" to it } }
        val unknown = referenced.filter { (_, id) -> id !in known }.map { it.first }
        assertTrue("unbekannte Übung: $unknown", unknown.isEmpty())
    }
}
