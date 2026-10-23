package app.atemkraft.cue

import app.atemkraft.domain.BreathLevel
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

/**
 * Audio-Info zum Phasenbeginn für den durchgehenden Ton (Typ + Dauer; open = ohne feste Dauer).
 * Liegt bei den Reizen, weil Session (`SessionAudioCoordinator`) und Editor-Vorschau
 * (`PreviewCuePlayer`) sie gleichermaßen an die Ton-Ausgabe geben.
 */
data class PhaseAudio(val type: PhaseType, val durationMs: Long, val open: Boolean, val level: BreathLevel? = null)
