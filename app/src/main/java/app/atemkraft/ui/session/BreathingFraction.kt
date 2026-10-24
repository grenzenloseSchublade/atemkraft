package app.atemkraft.ui.session

import app.atemkraft.domain.CircleFractions
import app.atemkraft.playback.SessionStatus
import app.atemkraft.playback.SessionUiState

/**
 * Was der Atemkreis zeigt: 0f ausgeatmet … 1f eingeatmet. Der Füllstand gleitet mit dem
 * Fortschritt der Phase von ihrem Start zu ihrem Ende ([SessionUiState.circle], im Ablauf stetig
 * gesetzt, MOTION-03); in der Vorbereitung steht er am Start der ersten Phase, ohne Phase (Ende) in
 * der Mitte. Eigene Datei, damit die Übergänge ohne UI prüfbar bleiben.
 */
internal fun circleFraction(state: SessionUiState): Float {
    if (state.status == SessionStatus.PREPARING) return state.circle.from
    if (state.phaseType == null) return CircleFractions.REST
    val progress = if (state.phaseTotalMs > 0L) {
        ((state.phaseTotalMs - state.remainingMs).toFloat() / state.phaseTotalMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    return state.circle.from + (state.circle.to - state.circle.from) * progress
}
