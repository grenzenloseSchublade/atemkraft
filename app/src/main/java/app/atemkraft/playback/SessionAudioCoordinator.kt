package app.atemkraft.playback

import android.content.Context
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.ContinuousTonePlayer
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.cue.PhaseAudio
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.UnplugWatcher
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.SoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bindet Ton, Haptik und Audio-Fokus an den Session-Zustand – im Scope des
 * [BreathSessionController] (App-weit, eine Instanz im AppContainer), bewusst NICHT an den
 * UI-Lifecycle. Hinter dem Sperrbildschirm ist die Composition
 * eingefroren (collectAsStateWithLifecycle pausiert unterhalb von STARTED); dort
 * platzierte Effekte verpassen das Session-Ende, und der durchgehende Ton liefe endlos
 * weiter, bis die Abschluss-Abfrage bestätigt wird. Diese Collector laufen dagegen
 * genau so lange wie die Session selbst.
 */
class SessionAudioCoordinator(
    context: Context,
    private val settings: Flow<CueSettings>,
    /** Die eine Instanz aus dem AppContainer (AUDIO-04); Phasen-Haptik nutzt deren Drossel nicht. */
    private val hapticPlayer: HapticPlayer,
) : BreathAudio {
    private val tonePlayer = ToneCuePlayer()
    private val continuousPlayer = ContinuousTonePlayer()

    /** Anruf oder andere App hat den Fokus genommen: Ton schweigt, Ablauf und Haptik laufen weiter (AUDIO-02). */
    private val focusLost = MutableStateFlow(false)
    private val audioFocus = AudioFocusController(context) { focusLost.value = it }

    /** Kopfhörer abgezogen: nur während der Übung registriert (S-48, SEC-PLAT-01). */
    private val unplug = UnplugWatcher(context)
    private var onUnplugged: () -> Unit = {}

    /** Die zuletzt gelesenen Ton-Einstellungen (nach [bind]); für [finishTailMs]. */
    private var current: StateFlow<CueSettings>? = null

    override fun bind(
        scope: CoroutineScope,
        status: Flow<SessionStatus>,
        cues: Flow<CueEvent>,
        phaseAudio: Flow<PhaseAudio>,
        muted: Flow<Boolean>,
        onUnplugged: () -> Unit,
    ) {
        this.onUnplugged = onUnplugged
        val current = settings.stateIn(scope, SharingStarted.Eagerly, CueSettings())
        this.current = current
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
                // Stummschalter und Fokusverlust betreffen nur den Ton – Haptik folgt weiter der Einstellung.
                // Fokus vor jedem hörbaren Ton: Der erste Cue kommt vor dem Status RUNNING, und ein
                // abgelehnter Antrag (Anruf) wird so beim nächsten Ton wiederholt (AUDIO-02).
                if (toneWanted && !mutedState.value) {
                    audioFocus.request()
                    if (!focusLost.value) tonePlayer.play(event)
                }
                hapticPlayer.play(event, enabled = cfg.haptics)
            }
        }
        scope.launch {
            phaseAudio.collect { pa ->
                if (current.value.soundMode == SoundMode.CONTINUOUS) {
                    // Wie bei den Cues: zum Phasenbeginn anfordern, ein abgelehnter Antrag wird wiederholt.
                    if (!mutedState.value) audioFocus.request()
                    // Beim Stummschalten die Tonhöhe weiter mitführen (nur ohne Pegel), damit
                    // das Aufheben der Stummschaltung nicht auf einer alten Phase hängen bleibt.
                    continuousPlayer.onPhase(
                        pa.type,
                        pa.durationMs,
                        pa.open,
                        raiseGain = !mutedState.value && !focusLost.value,
                        level = pa.level,
                        elapsedMs = pa.elapsedMs,
                    )
                }
            }
        }
        scope.launch {
            // collectLatest: Ein Statuswechsel bricht einen laufenden Ausklang-Delay ab, statt
            // dahinter zu warten – ein schneller Neustart bekommt Fokus/Ton sofort.
            combine(status.distinctUntilChanged(), current, mutedState, focusLost) { st, cfg, m, lost ->
                AudioInputs(st, cfg, m, lost)
            }.collectLatest { applyStatus(it.status, it.cfg, it.muted, it.lost) }
        }
    }

    private data class AudioInputs(val status: SessionStatus, val cfg: CueSettings, val muted: Boolean, val lost: Boolean)

    /**
     * Innerhalb der Session läuft der Player-Thread (bei Countdown/Pause stumm),
     * bei FINISHED/IDLE wird ausgeblendet, der Thread beendet und der Audio-Fokus
     * abgegeben – unabhängig davon, ob die Abschluss-Abfrage schon bestätigt wurde.
     * [lost]: Ein Anruf oder eine andere App hält den Fokus – der Ton schweigt, bis er zurückkommt.
     */
    private suspend fun applyStatus(status: SessionStatus, cfg: CueSettings, muted: Boolean, lost: Boolean) {
        // „Nochmal“/neuer Start: Ein noch klingender Abschluss-Gong blendet kurz aus, statt in die
        // neue Übung hineinzuklingen; ebenso, wenn ein Anruf den Fokus nimmt.
        if (status == SessionStatus.PREPARING || lost) tonePlayer.silenceGong()
        val inSession = status != SessionStatus.IDLE && status != SessionStatus.FINISHED
        // Kopfhörer abgezogen: Der Controller schaltet stumm (wie bei Fokusverlust), die Übung läuft weiter.
        if (inSession) unplug.watch(onUnplugged) else unplug.stop()
        val audibleStatus = status == SessionStatus.RUNNING || status == SessionStatus.WAITING_FOR_USER
        val audible = audibleStatus && !muted && !lost

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
            // Fokus nur, solange etwas klingen kann (AUDIO-02): nicht im Countdown (dort klingt nichts,
            // den ersten Cue fordert der Collector an), nicht in der Pause (Weiter fordert ihn wieder
            // an) und nicht stummgeschaltet – fremde Medien bleiben sonst unnötig gedämpft.
            audibleStatus && cfg.soundMode != SoundMode.OFF && !muted ->
                audioFocus.request()

            // Am Session-Ende erst den Abschluss-Gong ausklingen lassen, bevor fremde Medien
            // wieder auf volle Lautstärke gehen – aber nicht, wenn stumm oder der Fokus fort ist
            // (dann kommt kein Gong). Dauer dynamisch vom Player (Kurz/Lang-Profil), wie in der Meditation.
            status == SessionStatus.FINISHED && cfg.soundMode != SoundMode.OFF && !muted && !lost -> {
                delay(tonePlayer.gongTotalMs() + ToneCuePlayer.GONG_END_RESERVE_MS)
                audioFocus.abandon()
            }

            else -> audioFocus.abandon()
        }
    }

    /**
     * Ausklang des Abschluss-Gongs: so lange hält der Controller den Foreground-Service nach dem
     * Ende noch, damit der Gong hinter dem Sperrbildschirm nicht abgeschnitten wird. Ohne Ton oder
     * stummgeschaltet kommt kein Gong (dann 0).
     */
    override fun finishTailMs(muted: Boolean): Long {
        val cfg = current?.value ?: return 0L
        return if (cfg.soundMode == SoundMode.OFF || muted) 0L else tonePlayer.gongTotalMs() + ToneCuePlayer.GONG_END_RESERVE_MS
    }

    private companion object {
        /** Deckt die 60-ms-Ausblende des Players (`GAIN_FADE_SECONDS`) plus Puffer-Latenz ab. */
        const val FADE_OUT_MS = 180L
    }
}
