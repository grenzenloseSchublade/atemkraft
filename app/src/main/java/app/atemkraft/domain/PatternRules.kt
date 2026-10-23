package app.atemkraft.domain

import kotlin.math.round
import kotlin.math.roundToInt

/** Die vier Phasen eines Musters des Tages ([PatternSpec]). */
enum class PatternField { INHALE, HOLD_FULL, EXHALE, HOLD_EMPTY }

/**
 * Erzeugungsregeln für das Muster des Tages, das die App selbst entstehen lässt. Eine Quelle für
 * Generator und Tests. Eigene Muster aus dem Editor sind freie Folgen mit eigenen, festen Grenzen
 * ([FreePattern]); für sie gelten diese Stil-Regeln nicht (Nachtrag 4 „Frei gestalten“).
 *
 * Bewusst eine strengere Teilmenge der Import-Leitplanken ([withinGuardrails]): Leitplanken
 * (Import) ⊇ Erzeugungsregeln. Ein gespeichertes Muster des Tages wird beim Import also nie
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
 * mindestens 1 s lang sein; 0 s schreibt man als `null`.
 */
fun PatternSpec.withinCreationRules(): Boolean {
    val phases = listOfNotNull(inhale, holdFull, exhale, holdEmpty)
    if (phases.any { !it.isFinite() || it < 0.0 || it != round(it) }) return false
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
