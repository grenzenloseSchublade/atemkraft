package app.atemkraft.ui.session

import androidx.compose.runtime.Composable
import app.atemkraft.ui.components.OverlayChrome

/** Vollbild-Session über dem Scaffold; Rahmen (Minimieren + Mute) über das geteilte [OverlayChrome]. */
@Composable
fun SessionOverlay(
    state: SessionUiState,
    showNextPhase: Boolean,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
) {
    OverlayChrome(muted = state.muted, onMinimize = onMinimize, onToggleMute = onToggleMute) {
        SessionScreen(
            state = state,
            showNextPhase = showNextPhase,
            onTogglePause = onTogglePause,
            onTapHaptic = onTapHaptic,
            onContinue = onContinue,
            onRestart = onRestart,
            onStop = onStop,
        )
    }
}
