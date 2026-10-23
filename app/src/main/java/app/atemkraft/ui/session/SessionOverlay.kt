package app.atemkraft.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.atemkraft.data.SessionDisplay
import app.atemkraft.playback.SessionStatus
import app.atemkraft.playback.SessionUiState
import app.atemkraft.ui.components.CalmHost
import app.atemkraft.ui.components.OverlayChrome

/**
 * Vollbild-Session über dem Scaffold; Rahmen (Minimieren + Mute) über das geteilte [OverlayChrome].
 *
 * Ruhe-Ansicht ([CalmHost]): Ein Tipp neben den Kreis blendet alle Bedienelemente aus
 * (Kopfleiste, Titel, Anleitung, Anleitungszeile, „Als Nächstes“, Knöpfe, Systemleisten), ein
 * weiterer wieder ein; Kreis, Phase, Sekunden und Fortschrittsbogen bleiben. Mit
 * [SessionDisplay.autoCalm] kommt die Ruhe von selbst, solange die Übung läuft (nicht im
 * Countdown). Pause und Ende zeigen alles; ein Neustart („Von vorne“, „Nochmal“) beginnt wieder
 * mit allem.
 *
 * System-Zurück minimiert zur Mini-Leiste wie der Chevron; am Ende-Screen wirkt es wie „Ende“
 * (Nutzerentscheidung 2026-10-19) – dann läuft nichts mehr, das weiterlaufen müsste.
 */
@Composable
fun SessionOverlay(
    state: SessionUiState,
    display: SessionDisplay,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    // „Im Logbuch ansehen ›“ am Ende-Screen; ohne ihn kein Link.
    onOpenLogbook: (() -> Unit)? = null,
    // „In der Stille bleiben ›“ am Ende-Screen: weiter in die stille Meditation; ohne ihn kein Link.
    onIntoSilence: (() -> Unit)? = null,
) {
    BackHandler { if (state.status == SessionStatus.FINISHED) onStop() else onMinimize() }
    CalmHost(
        canHide = state.status in HIDEABLE,
        autoCalm = display.autoCalm && state.status != SessionStatus.PREPARING,
        // Ende und Neustart (Countdown) beginnen wieder mit allen Bedienelementen.
        reset = state.status == SessionStatus.FINISHED || state.status == SessionStatus.PREPARING,
        modifier = modifier,
    ) { controlsVisible ->
        OverlayChrome(
            title = state.exerciseName,
            muted = state.muted,
            onMinimize = onMinimize,
            onToggleMute = onToggleMute,
            chromeVisible = controlsVisible,
        ) {
            SessionScreen(
                state = state,
                display = display,
                controlsVisible = controlsVisible,
                onTogglePause = onTogglePause,
                onTapHaptic = onTapHaptic,
                onContinue = onContinue,
                onRestart = onRestart,
                onStop = onStop,
                onOpenLogbook = onOpenLogbook,
                onIntoSilence = onIntoSilence,
            )
        }
    }
}

/** Zustände, in denen die Ruhe-Ansicht greift (nicht in der Pause, nicht am Ende). */
private val HIDEABLE = setOf(SessionStatus.PREPARING, SessionStatus.RUNNING, SessionStatus.WAITING_FOR_USER)
