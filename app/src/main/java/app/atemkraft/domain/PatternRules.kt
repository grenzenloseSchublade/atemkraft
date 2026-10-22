package app.atemkraft.domain

import kotlin.math.roundToInt

/** Die vier einstellbaren Phasen eines Musters (Editor-Stepper, Kurven-Griffe). */
enum class PatternField { INHALE, HOLD_FULL, EXHALE, HOLD_EMPTY }

/**
 * Erzeugungsregeln für Muster, die die App selbst entstehen lässt: Muster des Tages und
 * Muster-Editor („Eigenes Muster“). Eine Quelle für Generator, Editor und Tests.
 *
 * Bewusst eine strengere Teilmenge der Import-Leitplanken ([withinGuardrails]): Leitplanken
 * (Import) ⊇ Erzeugungsregeln. Ein im Editor erstelltes Muster wird beim Import also nie
 * verworfen und startet nie ohne Sicherheitsabfrage. Unterschiede zu den Leitplanken:
 * - nur ganze Sekunden (Leitplanken: Vielfache von 0,5 s);
 * - ruhig: Atemzug 9–13 s, ≈ 4,6–6,7 Atemzüge pro Minute (Leitplanken: 8,5–13 s);
 * - sanft aktivierend: Ausatmen echt kürzer als Einatmen (Brmhana), wie der Generator seit 1.6.3.
 *
 * Gemeinsam: Einatmen ≥ 2 s, Ausatmen 2–8 s; ruhig Ausatmen ≥ Einatmen, Halten voll ≤ 4 s,
 * Halten leer ≤ 2 s; sanft aktivierend ohne Halten leer, Halten voll ≤ 2 s, Atemzug 6–9 s
 * (≈ 6,7–10 Atemzüge pro Minute, nie forciert). Belege wie beim Generator: Russo, Santarelli &
 * O'Rourke 2017 (Breathe), Zaccaro et al. 2018 (Front. Hum. Neurosci.), Pranayama-Konventionen.
 */
object PatternRules {
    const val INHALE_MIN = 2
    const val EXHALE_MIN = 2
    const val EXHALE_MAX = 8
    const val CALM_HOLD_FULL_MAX = 4
    const val CALM_HOLD_EMPTY_MAX = 2
    const val CALM_CYCLE_MIN = 9
    const val CALM_CYCLE_MAX = 13
    const val ACTIVE_HOLD_FULL_MAX = 2
    const val ACTIVE_CYCLE_MIN = 6
    const val ACTIVE_CYCLE_MAX = 9

    /**
     * Suchbereich für [rangeFor]: Keine Phase kann länger sein als der längste erlaubte
     * Atemzug, darüber gibt es also sicher keinen gültigen Wert.
     */
    internal val SEARCH_RANGE = 0..CALM_CYCLE_MAX
}

/**
 * Setzt [field] auf [seconds] ganze Sekunden. Ein Halten mit 0 s heißt „ohne Halten“ und wird
 * zu `null`, damit es im Muster gar nicht erst als Phase auftaucht.
 */
fun PatternSpec.with(field: PatternField, seconds: Int): PatternSpec {
    val value = seconds.toDouble()
    // Nur genau 0 heißt „ohne“; negative Werte bleiben stehen und fallen durch die Regeln.
    val hold = value.takeIf { seconds != 0 }
    return when (field) {
        PatternField.INHALE -> copy(inhale = value)
        PatternField.HOLD_FULL -> copy(holdFull = hold)
        PatternField.EXHALE -> copy(exhale = value)
        PatternField.HOLD_EMPTY -> copy(holdEmpty = hold)
    }
}

/**
 * Länge von [field] in ganzen Sekunden; ein fehlendes Halten zählt als 0. Ältere Muster mit
 * halben Sekunden (bis 1.6.2) werden kaufmännisch gerundet (4,5 → 5).
 */
fun PatternSpec.secondsOf(field: PatternField): Int = when (field) {
    PatternField.INHALE -> inhale
    PatternField.HOLD_FULL -> holdFull ?: 0.0
    PatternField.EXHALE -> exhale
    PatternField.HOLD_EMPTY -> holdEmpty ?: 0.0
}.roundToInt()

/**
 * Hält das Muster die Erzeugungsregeln ein (siehe [PatternRules])? Ein vorhandenes Halten muss
 * mindestens 1 s lang sein; 0 s schreibt man als `null` (siehe [with]).
 */
fun PatternSpec.withinCreationRules(): Boolean {
    val phases = listOfNotNull(inhale, holdFull, exhale, holdEmpty)
    if (phases.any { !it.isFinite() || it < 0.0 || it != Math.rint(it) }) return false
    if (holdFull != null && holdFull < 1.0) return false
    if (holdEmpty != null && holdEmpty < 1.0) return false
    if (inhale < PatternRules.INHALE_MIN || exhale < PatternRules.EXHALE_MIN || exhale > PatternRules.EXHALE_MAX) {
        return false
    }
    val cycle = phases.sum()
    return if (activating) {
        holdEmpty == null &&
            (holdFull ?: 0.0) <= PatternRules.ACTIVE_HOLD_FULL_MAX &&
            exhale < inhale &&
            cycle >= PatternRules.ACTIVE_CYCLE_MIN &&
            cycle <= PatternRules.ACTIVE_CYCLE_MAX
    } else {
        exhale >= inhale &&
            (holdFull ?: 0.0) <= PatternRules.CALM_HOLD_FULL_MAX &&
            (holdEmpty ?: 0.0) <= PatternRules.CALM_HOLD_EMPTY_MAX &&
            cycle >= PatternRules.CALM_CYCLE_MIN &&
            cycle <= PatternRules.CALM_CYCLE_MAX
    }
}

/**
 * Die Werte, die [field] bei sonst unveränderten Phasen annehmen darf: kleinster bis größter
 * Wert v, für den `with(field, v)` die Erzeugungsregeln einhält. Jede Regel ist in v linear,
 * deshalb ist die Menge lückenlos; bei einem gültigen Muster enthält sie den aktuellen Wert.
 *
 * `null` heißt „nicht wählbar“: Halten leer bei sanft aktivierenden Mustern, oder es gibt gar
 * keinen gültigen Wert (nur bei einem Muster, das die Regeln schon in anderen Phasen verletzt).
 * Die Grenzen sind dynamisch (z. B. Einatmen 6 erst ab Ausatmen 6), Stepper und Kurven-Griffe
 * klemmen darauf, sodass ein ungültiges Muster nie entsteht.
 */
fun PatternSpec.rangeFor(field: PatternField): IntRange? {
    if (activating && field == PatternField.HOLD_EMPTY) return null
    val valid = PatternRules.SEARCH_RANGE.filter { with(field, it).withinCreationRules() }
    return if (valid.isEmpty()) null else valid.first()..valid.last()
}
