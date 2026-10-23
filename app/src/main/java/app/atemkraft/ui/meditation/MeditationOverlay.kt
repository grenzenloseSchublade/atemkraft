package app.atemkraft.ui.meditation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.formatClock
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.CalmHost
import app.atemkraft.ui.components.CircleTapTarget
import app.atemkraft.ui.components.FinishedPanel
import app.atemkraft.ui.components.GlowText
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.components.OverlayChrome
import app.atemkraft.ui.components.PauseFlash
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SessionControlBar
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.breathingDiscDiameter
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.rememberTapFlash
import app.atemkraft.ui.components.sessionSummaryParts
import app.atemkraft.ui.theme.Dimens

/**
 * Vollbild-Overlay der laufenden/abgeschlossenen Meditation – identischer Rahmen wie die
 * Atem-Session ([app.atemkraft.ui.session.SessionOverlay]) über das geteilte [OverlayChrome].
 * Am Ende dieselbe Frage samt Bilanz wie dort; System-Zurück minimiert, am Ende-Screen wirkt es
 * wie „Ende“ (Nutzerentscheidung 2026-10-19). Ruhe-Ansicht wie dort ([CalmHost]): Ein Tipp neben
 * den Kreis blendet Titel, Knöpfe und Systemleisten aus; mit [MeditationDisplay.autoCalm] von
 * selbst, sobald die Meditation läuft. Ring und Zeit schaltet [display] ab.
 */
@Composable
fun MeditationOverlay(
    state: MeditationUiState,
    display: MeditationDisplay,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
    // „Im Logbuch ansehen ›“ am Ende-Screen; ohne ihn kein Link.
    onOpenLogbook: (() -> Unit)? = null,
) {
    BackHandler { if (state.status == MeditationStatus.FINISHED) onEnd() else onMinimize() }
    CalmHost(
        canHide = state.status == MeditationStatus.PREPARING || state.status == MeditationStatus.RUNNING,
        autoCalm = display.autoCalm && state.status == MeditationStatus.RUNNING,
        // Ende und Neustart (Countdown) beginnen wieder mit allen Bedienelementen.
        reset = state.status == MeditationStatus.FINISHED || state.status == MeditationStatus.PREPARING,
    ) { controlsVisible ->
        OverlayChrome(
            title = stringResource(R.string.meditation_title),
            muted = state.muted,
            onMinimize = onMinimize,
            onToggleMute = onToggleMute,
            chromeVisible = controlsVisible,
        ) {
            MeditationContent(state, display, controlsVisible, onTogglePause, onTapHaptic, onRestart, onEnd, onOpenLogbook)
        }
    }
}

@Composable
private fun MeditationContent(
    state: MeditationUiState,
    display: MeditationDisplay,
    controlsVisible: Boolean,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
    onOpenLogbook: (() -> Unit)?,
) {
    if (state.status == MeditationStatus.FINISHED) {
        FinishedPanel(
            title = stringResource(R.string.session_done_question),
            onAgain = onRestart,
            onExit = onEnd,
            onOpenLogbook = onOpenLogbook,
            // Bilanz wie in der Atem-Session: die aktive (pausenbereinigte) Zeit, dieselbe Zahl
            // wie im Logbuch – der Controller setzt elapsedMs beim Beenden darauf.
            summary = sessionSummaryParts(durationMs = state.elapsedMs),
        )
    } else {
        RunningContent(
            state = state,
            display = display,
            controlsVisible = controlsVisible,
            onTogglePause = onTogglePause,
            onTapHaptic = onTapHaptic,
            onRestart = onRestart,
            onEnd = onEnd,
        )
    }
}

@Composable
private fun RunningContent(
    state: MeditationUiState,
    display: MeditationDisplay,
    controlsVisible: Boolean,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
) {
    val preparing = state.status == MeditationStatus.PREPARING
    val paused = state.status == MeditationStatus.PAUSED
    val ringDescription = stringResource(R.string.cd_meditation_ring)
    val ringAction = stringResource(if (paused) R.string.circle_action_resume else R.string.circle_action_pause)
    // TalkBack-Ansage der Zustandswechsel (wie die Atem-Session ihren Phasennamen ansagt): als
    // Zustand am Titel („Meditation, Pausiert“), der sichtbare Name bleibt der gesprochene. Auch
    // „Läuft“ ist ein Zustand: Ohne ihn verschwand die Ansage beim Fortsetzen still (A11Y-04).
    val stateAnnounce = when {
        preparing -> stringResource(R.string.session_get_ready)
        paused -> stringResource(R.string.session_paused)
        else -> stringResource(R.string.meditation_running)
    }

    // Ruhiger, VOLL gefüllter Kreis (kein Wachstum); der Fortschritt steht in der Zahl.
    val timeText = when {
        preparing -> state.countdown.toString()
        state.mode == MeditationMode.TIMED -> formatClock(state.remainingMs)
        else -> formatClock(state.elapsedMs)
    }

    // Tap-Flash (geteilt mit der Atem-Session); die UI-Haptik spielt der Player im AppContainer
    // (AUDIO-04, -05).
    val tapFlash = rememberTapFlash()

    // Gleiches Gerüst wie die Atem-Session (SessionRunningLayout): Titel oben, Kreis im freien
    // Platz, Steuerung unten – nichts überlappt, auch bei großer Schrift.
    SessionRunningLayout(
        modifier = Modifier.fillMaxSize().padding(Dimens.SessionPadding),
        chromeVisible = controlsVisible,
        top = {
            // Titel oben (wie die Atem-Session ihren Übungsnamen zeigt) – nicht nur „nackte" Zeit.
            Text(
                text = stringResource(R.string.meditation_title),
                style = MaterialTheme.typography.titleSmall,
                // Wie der Übungsname der Atem-Session: voll, nicht gedämpft.
                color = MaterialTheme.colorScheme.onBackground,
                // liveRegion: TalkBack sagt Zustandswechsel (Vorbereitung/läuft/pausiert) an.
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        stateDescription = stateAnnounce
                    },
            )
        },
        bottom = {
            // Dieselbe Icon-Steuerung wie die Atem-Session; im Countdown nur „Beenden“.
            SessionControlBar(
                playing = !paused,
                onMain = onTogglePause,
                onRestart = onRestart,
                onStop = onEnd,
                preparing = preparing,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { side ->
        Box(contentAlignment = Alignment.Center) {
            // Fortschrittsbogen wie in der Atem-Session (nur Timer; offen = nur Spur).
            val progress = if (state.mode == MeditationMode.TIMED && !preparing) {
                (state.elapsedMs.toFloat() / (state.elapsedMs + state.remainingMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
            } else {
                null
            }
            BreathingCircle(
                fraction = 1f,
                progress = progress,
                ring = display.showProgress,
                modifier = Modifier.size(side),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (preparing) {
                        // Kleiner als die Zeit und nie breiter als die Scheibe, damit es auch bei
                        // großer Schrift im Kreis bleibt (bricht dann zwischen den Wörtern um).
                        GlowText(
                            text = stringResource(R.string.session_get_ready),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.widthIn(max = breathingDiscDiameter(side, 1f)),
                        )
                        Spacer(Modifier.height(Dimens.GapTiny))
                    }
                    // Ohne Zeit bleibt nur der stille Kreis; der Countdown vor dem Start zählt immer.
                    if (preparing || display.showTime) {
                        GlowText(text = timeText, style = MaterialTheme.typography.displaySmall)
                    }
                }
            }
            // Wie in der Atem-Session: Tippen auf den Kreis (bis zur Spur, nicht ins Quadrat
            // daneben) pausiert/setzt fort; im Countdown tut er nichts, fällt aber auch nicht zur
            // Ruhe-Ansicht durch.
            CircleTapTarget(
                side = side,
                description = ringDescription,
                clickLabel = ringAction,
                onTap = if (preparing) {
                    null
                } else {
                    { tapFlash.toggle(running = state.status == MeditationStatus.RUNNING, haptic = onTapHaptic, toggle = onTogglePause) }
                },
                swallowWhenIdle = true,
            )
            // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content).
            PauseFlash(alpha = tapFlash.alpha, isPause = tapFlash.isPause, maxSize = side)
        }
    }
}
