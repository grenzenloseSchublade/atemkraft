package app.atemkraft.domain

import kotlin.math.abs

/**
 * Was sich jemand im Muster-Editor wünscht: sanft aktivierend oder ruhig, mit oder ohne Halten.
 * Die Dauer gehört nicht dazu, sie ändert nur die Minuten, nie die Phasen.
 */
data class PatternWish(val activating: Boolean, val withHold: Boolean)

/**
 * Wunsch-Generator des Muster-Editors: Er macht aus einem [PatternWish] einen typischen
 * Vorschlag und blättert auf Wunsch durch alle weiteren passenden Muster.
 *
 * Ohne Zufall und unabhängig vom Muster des Tages ([RandomPatternGenerator] bleibt unberührt):
 * Er zählt alle Muster in ganzen Sekunden auf, die die Erzeugungsregeln ([withinCreationRules])
 * einhalten und zum Wunsch passen, und sortiert sie fest nach „Typikalität“.
 *
 * Passend heißt:
 * - ruhig: Ausatmen echt länger als Einatmen (Langhana). Gleich langes Ein- und Ausatmen
 *   (Samana) entsteht nur durch Feinjustieren, die Einordnung zeigt es dann ehrlich an.
 * - sanft aktivierend: Ausatmen kürzer als Einatmen, kein Halten leer (schon durch die Regeln).
 * - ohne Halten: beide Halten fehlen; mit Halten: mindestens eines ist da.
 *
 * Sortierung (in dieser Reihenfolge, jeweils kleiner zuerst):
 * 1. Abstand des Einatmens von 4 s;
 * 2. Abstand des Verhältnisses von 1 : 1,5 (ruhig Aus/Ein, aktivierend Ein/Aus);
 * 3. Abstand des Atemzugs von 10 s (ruhig) bzw. 7,5 s (aktivierend);
 * 4. Halten insgesamt;
 * 5. Halten leer (bei gleichem Halten zuerst Halten voll);
 * 6. Einatmen, Halten voll, Ausatmen, Halten leer (macht die Reihenfolge eindeutig).
 *
 * Alle Vergleiche rechnen ganzzahlig, damit Gleichstände nicht an Rundungen hängen.
 */
object PatternComposer {

    /** Alle passenden Muster zu [wish], der typische Vorschlag zuerst; nie leer. */
    fun candidates(wish: PatternWish): List<PatternSpec> = cache.getValue(wish)

    /**
     * Vorschlag Nummer [variant] zu [wish]; „Anderer Vorschlag“ ist `variant + 1`. Läuft nach
     * dem letzten Muster wieder von vorn, auch negative Werte sind erlaubt.
     */
    fun propose(wish: PatternWish, variant: Int): PatternSpec {
        val list = candidates(wish)
        return list[Math.floorMod(variant, list.size)]
    }

    /** Die Wünsche, zu denen [spec] passt (Tagesmuster übernehmen, Bearbeiten). */
    fun wishOf(spec: PatternSpec): PatternWish = PatternWish(
        activating = spec.activating,
        withHold = (spec.holdFull ?: 0.0) > 0.0 || (spec.holdEmpty ?: 0.0) > 0.0,
    )

    private val wishes: List<PatternWish> =
        listOf(false, true).flatMap { activating -> listOf(false, true).map { PatternWish(activating, it) } }

    private val cache: Map<PatternWish, List<PatternSpec>> = wishes.associateWith { compute(it) }

    private fun compute(wish: PatternWish): List<PatternSpec> {
        val all = buildList {
            for (inhale in PatternRules.INHALE_MIN..PatternRules.EXHALE_MAX) {
                for (exhale in PatternRules.EXHALE_MIN..PatternRules.EXHALE_MAX) {
                    for (holdFull in 0..PatternRules.CALM_HOLD_FULL_MAX) {
                        for (holdEmpty in 0..PatternRules.CALM_HOLD_EMPTY_MAX) {
                            add(
                                PatternSpec(
                                    inhale = inhale.toDouble(),
                                    holdFull = holdFull.takeIf { it > 0 }?.toDouble(),
                                    exhale = exhale.toDouble(),
                                    holdEmpty = holdEmpty.takeIf { it > 0 }?.toDouble(),
                                    activating = wish.activating,
                                ),
                            )
                        }
                    }
                }
            }
        }
        val matching = all.filter { it.withinCreationRules() && matches(it, wish) }
        check(matching.isNotEmpty()) { "Kein Muster für $wish" }
        return matching.sortedWith(typicality(wish.activating))
    }

    private fun matches(spec: PatternSpec, wish: PatternWish): Boolean {
        val character = if (wish.activating) spec.exhale < spec.inhale else spec.exhale > spec.inhale
        return character && wishOf(spec) == wish
    }

    private fun typicality(activating: Boolean): Comparator<PatternSpec> = compareBy<PatternSpec> { abs(it.ein - 4) }
        .thenBy { ratioDistance(it, activating) }
        .thenBy { cycleDistance(it, activating) }
        .thenBy { it.voll + it.leer }
        .thenBy { it.leer }
        // Lexikografisch; Halten leer steht schon in Schlüssel 5.
        .thenBy { it.ein }
        .thenBy { it.voll }
        .thenBy { it.aus }

    /** |Verhältnis − 1,5| als Bruch, damit Gleichstände exakt bleiben. */
    private fun ratioDistance(spec: PatternSpec, activating: Boolean): Fraction {
        val (long, short) = if (activating) spec.ein to spec.aus else spec.aus to spec.ein
        return Fraction(abs(2 * long - 3 * short), 2 * short)
    }

    /** Doppelter Abstand des Atemzugs vom Ziel (10 s bzw. 7,5 s), ganzzahlig. */
    private fun cycleDistance(spec: PatternSpec, activating: Boolean): Int {
        val cycle = spec.ein + spec.voll + spec.aus + spec.leer
        return if (activating) abs(2 * cycle - 15) else abs(2 * cycle - 20)
    }

    private class Fraction(val num: Int, val den: Int) : Comparable<Fraction> {
        override fun compareTo(other: Fraction): Int = (num.toLong() * other.den).compareTo(other.num.toLong() * den)
    }

    private val PatternSpec.ein: Int get() = secondsOf(PatternField.INHALE)
    private val PatternSpec.voll: Int get() = secondsOf(PatternField.HOLD_FULL)
    private val PatternSpec.aus: Int get() = secondsOf(PatternField.EXHALE)
    private val PatternSpec.leer: Int get() = secondsOf(PatternField.HOLD_EMPTY)
}
