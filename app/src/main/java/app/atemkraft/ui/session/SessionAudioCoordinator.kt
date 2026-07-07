package app.atemkraft.ui.session

import android.content.Context
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.ContinuousTonePlayer
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.SoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bindet Ton, Haptik und Audio-Fokus an den Session-Zustand – im Scope des ViewModels,
 * bewusst NICHT an den UI-Lifecycle. Hinter dem Sperrbildschirm ist die Composition
 * eingefroren (collectAsStateWithLifecycle pausiert unterhalb von STARTED); dort
 * platzierte Effekte verpassen das Session-Ende, und der durchgehende Ton liefe endlos
 * weiter, bis die Abschluss-Abfrage bestätigt wird. Diese Collector laufen dagegen
 * genau so lange wie die Session selbst.
 */
class SessionAudioCoordinator(
    context: Context,
    private val settings: Flow<CueSettings>,
) {
    private val tonePlayer = ToneCuePlayer()
    private val continuousPlayer = ContinuousTonePlayer()
    private val hapticPlayer = HapticPlayer(context.applicationContext)
    private val audioFocus = AudioFocusController(context)

    fun bind(
        scope: CoroutineScope,
        status: Flow<SessionStatus>,
        cues: Flow<CueEvent>,
        phaseAudio: Flow<PhaseAudio>,
    ) {
        val current = settings.stateIn(scope, SharingStarted.Eagerly, CueSettings())
        scope.launch {
            current.collect { cfg ->
                tonePlayer.setVolume(cfg.volume)
                continuousPlayer.setVolume(cfg.volume)
                continuousPlayer.setEmphasis(cfg.transition)
            }
        }
        scope.launch {
            cues.collect { event ->
                val cfg = current.value
                // Der Abschluss-Gong spielt in beiden Ton-Modi (beim durchgehenden Ton
                // als hörbares Ende nach dem Ausblenden); Phasen-Cues nur im CUES-Modus.
                val toneWanted = if (event == CueEvent.FINISH) {
                    cfg.soundMode != SoundMode.OFF
                } else {
                    cfg.soundMode == SoundMode.CUES
                }
                if (toneWanted) tonePlayer.play(event)
                if (cfg.haptics) hapticPlayer.play(event)
            }
        }
        scope.launch {
            phaseAudio.collect { pa ->
                if (current.value.soundMode == SoundMode.CONTINUOUS) {
                    continuousPlayer.onPhase(pa.type, pa.durationMs, pa.open)
                }
            }
        }
        scope.launch {
            combine(status.distinctUntilChanged(), current) { st, cfg -> applyStatus(st, cfg) }
                .collect()
        }
    }

    /**
     * Innerhalb der Session läuft der Player-Thread (bei Countdown/Pause stumm),
     * bei FINISHED/IDLE wird ausgeblendet, der Thread beendet und der Audio-Fokus
     * abgegeben – unabhängig davon, ob die Abschluss-Abfrage schon bestätigt wurde.
     */
    private suspend fun applyStatus(status: SessionStatus, cfg: CueSettings) {
        val inSession = status != SessionStatus.IDLE && status != SessionStatus.FINISHED
        val audible = status == SessionStatus.RUNNING || status == SessionStatus.WAITING_FOR_USER

        if (cfg.soundMode == SoundMode.CONTINUOUS && inSession) {
            continuousPlayer.start()
            if (!audible) continuousPlayer.mute()
        } else if (continuousPlayer.isRunning) {
            // Erst ausblenden, dann Thread beenden – kein harter Schnitt am Session-Ende.
            continuousPlayer.mute()
            delay(FADE_OUT_MS)
            continuousPlayer.stop()
        }

        when {
            inSession && cfg.soundMode != SoundMode.OFF -> audioFocus.request()
            // Am Session-Ende erst den Abschluss-Gong ausklingen lassen, bevor fremde
            // Medien wieder auf volle Lautstärke gehen.
            status == SessionStatus.FINISHED && cfg.soundMode != SoundMode.OFF -> {
                delay(FINISH_CUE_MS)
                audioFocus.abandon()
            }
            else -> audioFocus.abandon()
        }
    }

    /** Beim Wegräumen des ViewModels aufrufen: gibt Threads und Audio-Fokus frei. */
    fun release() {
        continuousPlayer.stop()
        tonePlayer.release()
        audioFocus.abandon()
    }

    private companion object {
        /** Deckt die ~15-ms-Ausblende des Players plus Puffer-Latenz ab. */
        const val FADE_OUT_MS = 120L

        /** Dauer des Abschluss-Gongs (2,2 s) mit etwas Reserve. */
        const val FINISH_CUE_MS = 2400L
    }
}
