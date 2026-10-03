package app.atemkraft.ui.session

import app.atemkraft.domain.PhaseType

/** Skala des Kreises im Start-Countdown (Vorbereitung): halb gefüllt. */
internal const val PREPARE_FRACTION = 0.5f

/**
 * Was der Atemkreis zeigt: im Countdown [PREPARE_FRACTION], sonst [breathingFraction].
 * Eigene Datei, damit FractionContinuityTest die Übergänge ohne UI prüfen kann (MOTION-03).
 */
internal fun circleFraction(state: SessionUiState): Float = if (state.status == SessionStatus.PREPARING) PREPARE_FRACTION else breathingFraction(state)

/** Anzeigeskala des Kreises: 0f ausgeatmet … 1f eingeatmet. Soll stetig über Phasengrenzen sein (MOTION-03, bekannte Sprünge: S-11). */
internal fun breathingFraction(state: SessionUiState): Float {
    val progress = if (state.phaseTotalMs > 0L) {
        ((state.phaseTotalMs - state.remainingMs).toFloat() / state.phaseTotalMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    return when (state.phaseType) {
        PhaseType.INHALE, PhaseType.INHALE_TOP_UP -> progress
        PhaseType.EXHALE -> 1f - progress
        PhaseType.HOLD_FULL -> 1f
        PhaseType.HOLD_EMPTY -> 0f
        PhaseType.REST -> 0.5f
        null -> 0.5f
    }
}
