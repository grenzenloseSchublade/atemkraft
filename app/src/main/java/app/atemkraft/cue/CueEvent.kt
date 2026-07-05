package app.atemkraft.cue

import app.atemkraft.domain.PhaseType

/**
 * Auslöser für einen Begleit-Reiz (Ton/Haptik) beim Wechsel in eine neue Phase.
 * Bewusst gröber als [PhaseType], weil mehrere Phasentypen denselben Reiz teilen.
 */
enum class CueEvent { INHALE, EXHALE, HOLD, FINISH }

/** Welcher Reiz gehört zum Beginn einer Phase dieses Typs. */
fun PhaseType.toCueEvent(): CueEvent = when (this) {
    PhaseType.INHALE, PhaseType.INHALE_TOP_UP -> CueEvent.INHALE
    PhaseType.EXHALE -> CueEvent.EXHALE
    PhaseType.HOLD_FULL, PhaseType.HOLD_EMPTY, PhaseType.REST -> CueEvent.HOLD
}
