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
import kotlinx.coroutines.flow.collectLatest
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
        muted: Flow<Boolean>,
    ) {
        val current = settings.stateIn(scope, SharingStarted.Eagerly, CueSettings())
        // In-Session-Stummschalter: übersteuert transient den Ton (nicht die Einstellung).
        val mutedState = muted.stateIn(scope, SharingStarted.Eagerly, false)
        scope.launch {
            current.collect { cfg ->
                tonePlayer.setVolume(cfg.volume)
                tonePlayer.setGongLong(cfg.gongLong)
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
                // Stummschalter betrifft nur den Ton – Haptik folgt weiter der Einstellung.
                if (toneWanted && !mutedState.value) tonePlayer.play(event)
                if (cfg.haptics) hapticPlayer.play(event)
            }
        }
        scope.launch {
            phaseAudio.collect { pa ->
                if (current.value.soundMode == SoundMode.CONTINUOUS) {
                    // Beim Stummschalten die Tonhöhe weiter mitführen (nur ohne Pegel), damit
                    // das Aufheben der Stummschaltung nicht auf einer alten Phase hängen bleibt.
                    continuousPlayer.onPhase(pa.type, pa.durationMs, pa.open, raiseGain = !mutedState.value)
                }
            }
        }
        scope.launch {
            // collectLatest: Ein Statuswechsel bricht einen laufenden Ausklang-Delay ab, statt
            // dahinter zu warten – ein schneller Neustart bekommt Fokus/Ton sofort.
            combine(status.distinctUntilChanged(), current, mutedState) { st, cfg, m ->
                Triple(st, cfg, m)
            }.collectLatest { (st, cfg, m) -> applyStatus(st, cfg, m) }
        }
    }

    /**
     * Innerhalb der Session läuft der Player-Thread (bei Countdown/Pause stumm),
     * bei FINISHED/IDLE wird ausgeblendet, der Thread beendet und der Audio-Fokus
     * abgegeben – unabhängig davon, ob die Abschluss-Abfrage schon bestätigt wurde.
     */
    private suspend fun applyStatus(status: SessionStatus, cfg: CueSettings, muted: Boolean) {
        val inSession = status != SessionStatus.IDLE && status != SessionStatus.FINISHED
        val audible = (status == SessionStatus.RUNNING || status == SessionStatus.WAITING_FOR_USER) &&
            !muted

        if (cfg.soundMode == SoundMode.CONTINUOUS && inSession) {
            continuousPlayer.start()
            if (audible) continuousPlayer.unmute() else continuousPlayer.mute()
        } else if (continuousPlayer.isRunning) {
            // Erst ausblenden, dann Thread beenden – kein harter Schnitt am Session-Ende.
            continuousPlayer.mute()
            delay(FADE_OUT_MS)
            continuousPlayer.stop()
        }

        when {
            // In der Pause den Fokus abgeben, damit fremde Medien nicht unnötig gedämpft bleiben,
            // solange die Session still ruht (resume() fordert ihn wieder an).
            inSession && status != SessionStatus.PAUSED && cfg.soundMode != SoundMode.OFF ->
                audioFocus.request()
            // Am Session-Ende erst den Abschluss-Gong ausklingen lassen, bevor fremde Medien
            // wieder auf volle Lautstärke gehen – aber nicht, wenn stumm (dann kommt kein Gong).
            // Dauer dynamisch vom Player (Kurz/Lang-Profil), wie in der Meditation.
            status == SessionStatus.FINISHED && cfg.soundMode != SoundMode.OFF && !muted -> {
                delay(tonePlayer.gongTotalMs().toLong() + FINISH_MARGIN_MS)
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

        /** Reserve über die Gong-Gesamtdauer hinaus, bevor der Fokus abgegeben wird. */
        const val FINISH_MARGIN_MS = 300L
    }
}
