package app.atemkraft.domain

import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/** Die nackten Parameter eines generierten Musters (Sekunden) – persistierbar. */
data class PatternSpec(
    val inhale: Double,
    val holdFull: Double?,
    val exhale: Double,
    val holdEmpty: Double?,
    val activating: Boolean,
)

/**
 * Liegt das Muster in den Leitplanken des Generators (siehe [RandomPatternGenerator])? Eine
 * Quelle für Generator-Test und Import: Von außen kommende Muster außerhalb dieser Grenzen
 * werden nicht übernommen, weil ihre Beschreibung („Halten ≤ 4 s" …) sonst falsch wäre und sie
 * ohne Sicherheitsabfrage starten.
 *
 * Bewusst weiter als der heutige Generator: 0,5-s-Werte und aktivierende Muster mit gleich
 * langem Ein- und Ausatmen erzeugte er bis Version 1.6.2; gespeicherte und exportierte Muster
 * aus dieser Zeit bleiben gültig. Leitplanken (Import) ⊇ Erzeugungsregeln
 * ([withinCreationRules], Muster des Tages): ruhig hier Atemzug 8,5–13 s, bei der
 * Erzeugung 9–13 s.
 */
fun PatternSpec.withinGuardrails(): Boolean {
    val phases = listOfNotNull(inhale, holdFull, exhale, holdEmpty)
    if (phases.any { !it.isFinite() || it < 0.0 || it * 2 != Math.rint(it * 2) }) return false
    if (inhale < 2.0 || exhale < 2.0 || exhale > 8.0) return false
    val cycle = phases.sum()
    return if (activating) {
        holdEmpty == null && (holdFull ?: 0.0) <= 2.0 && exhale <= inhale && cycle in 6.0..9.0
    } else {
        exhale >= inhale && (holdFull ?: 0.0) <= 4.0 && (holdEmpty ?: 0.0) <= 2.0 && cycle in 8.5..13.0
    }
}

/** Das Tagesmuster: die generierte Übung + ihre Parameter (für Chip + Speichern). */
data class DailyPattern(val exercise: Exercise, val spec: PatternSpec) {
    val activating: Boolean get() = spec.activating
}

/**
 * Erzeugt das „Muster des Tages": ein zufälliges, aber physiologisch plausibles Atemmuster,
 * **deterministisch aus dem Datum** (gleicher Tag ⇒ gleiches Muster). Bewusst als spielerische
 * Abwechslung positioniert – ohne Wirkversprechen (die Studienlage favorisiert für regelmäßiges
 * Üben EIN konstantes langsames Muster, siehe effectDetail-Text).
 *
 * Erzeugungsregeln ([PatternRules], [withinCreationRules]; Russo 2017, Zaccaro 2018,
 * Pranayama-Konventionen). Sie sind strenger als die Import-Leitplanken [withinGuardrails]:
 * Leitplanken (Import) ⊇ Erzeugungsregeln.
 * - ruhig: Zyklus 9–13 s (≈ 4,6–6,7 Atemzüge/min), Ausatmen ≥ Einatmen, Halten ≤ 4 s
 *   (nach dem Ausatmen ≤ 2 s);
 * - sanft aktivierend: Einatmen länger als Ausatmen (Brmhana der Atemlehre), Zyklus 6–9 s,
 *   Halten ≤ 2 s – nie Hyperventilation.
 *
 * Alle Phasen in ganzen Sekunden: So zählt man mit, der Countdown der Session stimmt mit der
 * Angabe überein, „Phasen anpassen“ (ganze Sekunden) zeigt dieselben Werte, und die
 * Verhältnisse sind ganzzahlig wie in der Atemlehre (4 : 6, 4 : 8). Halbe Sekunden spürt man
 * beim Atmen kaum, und für die Studienlage zählt das Tempo, nicht das genaue Verhältnis.
 *
 * Hier liegt nur die Erzeugung. Die Übung baut [GeneratedExercises], die Texte kommen aus
 * [PatternTexts], die Einordnung in die Atemlehre aus [PatternLore].
 */
object RandomPatternGenerator {

    fun forDate(date: LocalDate): DailyPattern = forSeed(date.toEpochDay())

    /** Muster aus beliebigem Seed – für „Neu generieren". */
    fun forSeed(seed: Long): DailyPattern {
        val random = Random(seed)
        return when (random.nextInt(100)) {
            in 0 until 19 -> calmNoHold(random)
            in 19 until 38 -> calmHoldFull(random)
            in 38 until 57 -> calm478Like(random)
            in 57 until 75 -> calmSoftBox(random)
            else -> gentlyActivating(random) // ~25 %
        }
    }

    // ---- Stile (alle Werte in ganzen Sekunden) ----

    private fun calmNoHold(r: Random): DailyPattern {
        val inhale = r.seconds(3, 5)
        val exhale = fitCalmCycle(inhale, r.exhaleFor(inhale, 1.0, 2.0), holds = 0)
        return build(inhale, null, exhale, null, activating = false)
    }

    private fun calmHoldFull(r: Random): DailyPattern {
        val inhale = r.seconds(3, 5)
        // Halten so kappen, dass selbst mit minimalem Ausatmen (= Einatmen) Zyklus <= 13 s bleibt.
        val hold = r.seconds(1, minOf(4, CALM_CYCLE_MAX - 2 * inhale))
        val exhale = fitCalmCycle(inhale, r.exhaleFor(inhale, 1.2, 2.0), holds = hold)
        return build(inhale, hold, exhale, null, activating = false)
    }

    /** Nach 4-7-8: kurzes Einatmen, Halten, doppelt so langes Ausatmen – in 13 s nur mit 3 s Einatmen. */
    private fun calm478Like(r: Random): DailyPattern {
        val inhale = 3
        val hold = r.seconds(2, 4)
        val exhale = fitCalmCycle(inhale, 2 * inhale, holds = hold)
        return build(inhale, hold, exhale, null, activating = false)
    }

    private fun calmSoftBox(r: Random): DailyPattern {
        // Enge Grenzen, damit auch der Maximal-Wurf (4 + 3 + 5 + 2) mit dem Nachjustieren <= 13 s bleibt.
        val inhale = r.seconds(3, 4)
        val holdFull = r.seconds(2, 3)
        val holdEmpty = r.seconds(1, 2)
        val exhale = fitCalmCycle(inhale, r.seconds(inhale, inhale + 1), holds = holdFull + holdEmpty)
        return build(inhale, holdFull, exhale, holdEmpty, activating = false)
    }

    private fun gentlyActivating(r: Random): DailyPattern {
        val inhale = r.seconds(3, 5)
        // Ausatmen immer kürzer als Einatmen (Brmhana), aber nie unter 2 s.
        var exhale = r.seconds(maxOf(ACTIVE_EXHALE_MIN, inhale - 2), inhale - 1)
        var hold = if (r.nextBoolean()) r.seconds(1, 2) else 0
        // Zyklus sanft in 6–9 s halten: erst über das Halten, dann über das Ausatmen.
        while (inhale + hold + exhale < ACTIVE_CYCLE_MIN && hold < 2) hold++
        while (inhale + hold + exhale < ACTIVE_CYCLE_MIN && exhale < inhale - 1) exhale++
        while (inhale + hold + exhale > ACTIVE_CYCLE_MAX && hold > 0) hold--
        while (inhale + hold + exhale > ACTIVE_CYCLE_MAX && exhale > ACTIVE_EXHALE_MIN) exhale--
        return build(inhale, hold.takeIf { it > 0 }, exhale, null, activating = true)
    }

    /** Ausatmen so nachjustieren, dass der ruhige Zyklus in 9–13 s liegt (Aus ≥ Ein bleibt). */
    private fun fitCalmCycle(inhale: Int, exhaleStart: Int, holds: Int): Int {
        var exhale = exhaleStart.coerceIn(inhale, CALM_EXHALE_MAX)
        while (inhale + holds + exhale < CALM_CYCLE_MIN && exhale < CALM_EXHALE_MAX) exhale++
        while (inhale + holds + exhale > CALM_CYCLE_MAX && exhale > inhale) exhale--
        return exhale
    }

    private fun build(
        inhale: Int,
        holdFull: Int?,
        exhale: Int,
        holdEmpty: Int?,
        activating: Boolean,
    ): DailyPattern {
        val spec = PatternSpec(inhale.toDouble(), holdFull?.toDouble(), exhale.toDouble(), holdEmpty?.toDouble(), activating)
        return DailyPattern(GeneratedExercises.exerciseFrom(spec, ExerciseIds.DAILY, "Tagesmuster ${PatternTexts.numbersOf(spec)}"), spec)
    }

    // ---- Helfer ----

    /** Ganze Sekunden zwischen [min] und [max] (inklusive). */
    private fun Random.seconds(min: Int, max: Int): Int = nextInt(min, max + 1)

    /** Ausatmen als Vielfaches des Einatmens (Verhältnis zufällig in [min]..[max]), gerundet. */
    private fun Random.exhaleFor(inhale: Int, min: Double, max: Double): Int = (inhale * (min + nextDouble() * (max - min))).roundToInt()

    // Eine Quelle für die Grenzen: die Erzeugungsregeln (PatternRules).
    private const val CALM_CYCLE_MIN = PatternRules.CALM_CYCLE_MIN
    private const val CALM_CYCLE_MAX = PatternRules.CALM_CYCLE_MAX
    private const val CALM_EXHALE_MAX = PatternRules.EXHALE_MAX
    private const val ACTIVE_CYCLE_MIN = PatternRules.ACTIVE_CYCLE_MIN
    private const val ACTIVE_CYCLE_MAX = PatternRules.ACTIVE_CYCLE_MAX
    private const val ACTIVE_EXHALE_MIN = PatternRules.EXHALE_MIN
}
