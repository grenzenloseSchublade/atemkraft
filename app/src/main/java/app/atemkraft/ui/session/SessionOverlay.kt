package app.atemkraft.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.components.OverlayChrome
import app.atemkraft.ui.components.rememberTouchExplorationEnabled

/**
 * Vollbild-Session über dem Scaffold; Rahmen (Minimieren + Mute) über das geteilte [OverlayChrome].
 *
 * Ruhe-Ansicht: Ein Tipp neben den Kreis blendet alle Bedienelemente aus (Kopfleiste, Titel,
 * Anleitung, Anleitungszeile, „Als Nächstes“, Knöpfe), ein weiterer wieder ein; Kreis, Phase,
 * Sekunden und Fortschrittsbogen bleiben. In der Pause und am Ende ist alles sichtbar (sonst
 * käme man nicht an „Beenden“), nach dem Fortsetzen gilt wieder die gewählte Ansicht; ein
 * Neustart („Von vorne“, „Nochmal“) beginnt wieder mit allem. Mit TalkBack wird nie ausgeblendet;
 * ein Tastendruck (Hardware-Tastatur, D-Pad) blendet wieder ein. Der Tipp auf den Kreis bleibt
 * Pause/Weiter: Der Kreis verbraucht ihn selbst, hier kommen nur Tipps auf freie Fläche an.
 *
 * System-Zurück minimiert zur Mini-Leiste wie der Chevron; am Ende-Screen wirkt es wie „Ende“
 * (Nutzerentscheidung 2026-10-19) – dann läuft nichts mehr, das weiterlaufen müsste.
 */
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
    modifier: Modifier = Modifier,
) {
    var calm by rememberSaveable { mutableStateOf(false) }
    val touchExploration = rememberTouchExplorationEnabled()
    val canHide = !touchExploration && state.status in HIDEABLE
    val controlsVisible = !(calm && canHide)
    val currentCanHide by rememberUpdatedState(canHide)
    // Ende und Neustart (Countdown) beginnen wieder mit allen Bedienelementen.
    val restarted = state.status == SessionStatus.FINISHED || state.status == SessionStatus.PREPARING
    LaunchedEffect(restarted) { if (restarted) calm = false }
    BackHandler { if (state.status == SessionStatus.FINISHED) onStop() else onMinimize() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { if (currentCanHide) calm = !calm } }
            .onPreviewKeyEvent { event ->
                // Erster Tastendruck in der Ruhe-Ansicht blendet nur ein, statt unsichtbar etwas auszulösen.
                val hidden = !controlsVisible
                if (hidden && event.type == KeyEventType.KeyDown) calm = false
                hidden
            },
    ) {
        OverlayChrome(
            title = state.exerciseName,
            muted = state.muted,
            onMinimize = onMinimize,
            onToggleMute = onToggleMute,
            chromeVisible = controlsVisible,
        ) {
            SessionScreen(
                state = state,
                showNextPhase = showNextPhase,
                controlsVisible = controlsVisible,
                onTogglePause = onTogglePause,
                onTapHaptic = onTapHaptic,
                onContinue = onContinue,
                onRestart = onRestart,
                onStop = onStop,
            )
        }
    }
}

/** Zustände, in denen die Ruhe-Ansicht greift (nicht in der Pause, nicht am Ende). */
private val HIDEABLE = setOf(SessionStatus.PREPARING, SessionStatus.RUNNING, SessionStatus.WAITING_FOR_USER)
