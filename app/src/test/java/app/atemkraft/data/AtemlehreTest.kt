package app.atemkraft.data

import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.withinCreationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TEXT-13: Die Yoga-Lehre steht getrennt von der Studienlage. Hier steht fest, welche Übungen
 * einen Abschnitt „Aus der Yoga-Lehre“ tragen (nur yogischer Ursprung, Nutzerentscheidung), dass
 * jeder Verweis auf eine Muster-Karte der Atemlehre aufgeht und dass die Teaser zuschreiben statt
 * Wirkung zu behaupten. Die Teaser-Form prüft `ContentRulesTest lore-teaser`.
 */
class AtemlehreTest {

    private val builtInIds = BuiltInExercises.all.map { it.id }.toSet()
    private val knownExerciseIds = builtInIds + RandomPatternGenerator.ID

    /** Muster des Tages über viele Seeds, damit alle Stile und Grenzfälle vorkommen. */
    private val generated = (0L..199L).map(RandomPatternGenerator::forSeed)

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
        val lore = BuiltInExercises.all.mapNotNull { e -> e.lore?.let { e.id to it } } +
            generated.take(50).mapIndexed { seed, p -> "${RandomPatternGenerator.ID}#seed$seed" to p.exercise.lore }
        lore.forEach { (key, l) ->
            assertNotNull("$key ohne Yoga-Lehre", l)
            (listOf(l!!.patternId) + l.morePatternIds).forEach { id ->
                assertNotNull("$key verweist auf unbekannte Karte „$id“", Atemlehre.pattern(id))
            }
        }
    }

    @Test
    fun `Kennungen der Muster-Karten sind eindeutig`() {
        val ids = Atemlehre.patterns.map { it.id }
        assertEquals("doppelte Kennung: ${ids.groupBy { it }.filterValues { it.size > 1 }.keys}", ids.size, ids.toSet().size)
    }

    @Test
    fun `Übungen auf Muster-Karten und Einflüssen gibt es in der App`() {
        val referenced = Atemlehre.patterns.flatMap { p -> p.exerciseIds.map { "${p.id} → $it" to it } } +
            Atemlehre.influences.flatMap { inf -> inf.exerciseIds.map { "${inf.label} → $it" to it } }
        val unknown = referenced.filter { (_, id) -> id !in knownExerciseIds }.map { it.first }
        assertTrue("unbekannte Übung: $unknown", unknown.isEmpty())
    }

    @Test
    fun `Muster des Tages verweist je nach Verhältnis auf Brmhana, Langhana oder Samana`() {
        generated.forEachIndexed { seed, p ->
            val expected = when {
                p.spec.exhale < p.spec.inhale -> "brmhana"
                p.spec.exhale > p.spec.inhale -> "langhana"
                else -> "samana"
            }
            assertEquals("Seed $seed (${p.spec})", expected, p.exercise.lore?.patternId)
            if (p.spec.activating) assertEquals("Seed $seed aktivierend", "brmhana", expected)
            // Jedes Halten verweist zusätzlich auf seine Karte.
            val holds = listOfNotNull(
                "halten-voll".takeIf { p.spec.holdFull != null },
                "halten-leer".takeIf { p.spec.holdEmpty != null },
            )
            assertEquals("Seed $seed (${p.spec})", holds, p.exercise.lore?.morePatternIds)
        }
    }

    /**
     * Alle Muster, die Generator und Editor erzeugen dürfen: ganze Sekunden, Einatmen 2–8,
     * Ausatmen 2–8, Halten voll 0–4, Halten leer 0–2, beide Charaktere, gefiltert mit den
     * Erzeugungsregeln (191 ruhige + 18 aktivierende).
     */
    private val creatable: List<PatternSpec> = buildList {
        for (activating in listOf(false, true)) {
            for (inhale in 2..8) {
                for (exhale in 2..8) {
                    for (holdFull in 0..4) {
                        for (holdEmpty in 0..2) {
                            add(
                                PatternSpec(
                                    inhale.toDouble(),
                                    holdFull.takeIf { it > 0 }?.toDouble(),
                                    exhale.toDouble(),
                                    holdEmpty.takeIf { it > 0 }?.toDouble(),
                                    activating,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }.filter { it.withinCreationRules() }

    @Test
    fun `atemlehreRefs verweist bei allen erzeugbaren Mustern nur auf die fünf Karten`() {
        assertEquals(209, creatable.size)
        val allowed = setOf("brmhana", "langhana", "samana", "halten-voll", "halten-leer")
        val used = creatable.flatMap { spec ->
            val refs = RandomPatternGenerator.atemlehreRefs(spec)
            listOf(refs.patternId) + refs.morePatternIds
        }.toSet()
        assertEquals(allowed, used)
        used.forEach { assertNotNull("unbekannte Karte „$it“", Atemlehre.pattern(it)) }
        // Innerhalb der Regeln: ruhig → Langhana oder Samana, aktivierend → immer Brmhana.
        creatable.forEach { spec ->
            val id = RandomPatternGenerator.atemlehreRefs(spec).patternId
            if (spec.activating) assertEquals("$spec", "brmhana", id) else assertTrue("$spec", id in setOf("langhana", "samana"))
        }
    }

    @Test
    fun `Einordnung ist für jede Herkunft dieselbe`() {
        val specs = creatable + generated.map { it.spec }
        specs.forEach { spec ->
            val refs = RandomPatternGenerator.atemlehreRefs(spec)
            PatternOrigin.entries.forEach { origin ->
                val lore = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin, 5).lore
                assertNotNull("$spec $origin ohne Yoga-Lehre", lore)
                assertEquals("$spec $origin", refs.patternId, lore!!.patternId)
                assertEquals("$spec $origin", refs.morePatternIds, lore.morePatternIds)
            }
        }
    }

    @Test
    fun `Detail der Yoga-Lehre nennt die Herkunft ehrlich`() {
        creatable.forEach { spec ->
            fun detail(origin: PatternOrigin) = RandomPatternGenerator.exerciseFrom(spec, "muster-1", "Muster", origin).lore!!.detail
            assertTrue("$spec", detail(PatternOrigin.CUSTOM).startsWith("Du hast"))
            assertTrue("$spec", detail(PatternOrigin.SAVED_DAILY).startsWith("Das Muster stammt aus einem Muster des Tages"))
            assertTrue("$spec", detail(PatternOrigin.TODAY).startsWith("Das Muster ist zufällig erzeugt"))
        }
    }

    @Test
    fun `jede Muster-Karte hat ein Rhythmusbild mit positiven Dauern`() {
        Atemlehre.patterns.forEach { p ->
            assertTrue("${p.id}: Rhythmus leer", p.rhythm.isNotEmpty())
            assertTrue("${p.id}: Dauer ≤ 0 in ${p.rhythm}", p.rhythm.all { it.seconds > 0.0 })
        }
    }

    /** Zuschreibend statt behauptend (TEXT-13): „gilt in der Yoga-Lehre als …“, nie „wirkt“. */
    @Test
    fun `Teaser der Yoga-Lehre behaupten keine Wirkung`() {
        val claim = Regex("""\b(wirkt|bewirkt|hilft gegen)\b""", RegexOption.IGNORE_CASE)
        val teasers = BuiltInExercises.all.mapNotNull { e -> e.lore?.let { "${e.id}.lore" to it.teaser } } +
            generated.take(50).mapNotNull { p -> p.exercise.lore?.let { "${RandomPatternGenerator.ID}.lore" to it.teaser } } +
            Atemlehre.patterns.map { "atemlehre.${it.id}" to it.teaser } +
            listOf(
                "atemlehre.rulesTeaser" to Atemlehre.rulesTeaser,
                "atemlehre.influencesTeaser" to Atemlehre.influencesTeaser,
            )
        val findings = teasers.distinct().mapNotNull { (key, t) -> claim.find(t)?.let { "$key: „${it.value}“ in „$t“" } }
        assertTrue("Wirkbehauptung statt Zuschreibung:\n" + findings.joinToString("\n"), findings.isEmpty())
    }
}
