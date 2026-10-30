package app.atemkraft.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.ContinuousTonePlayer
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.PhaseAudio
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.UnplugWatcher
import app.atemkraft.cue.toCueEvent
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.SoundMode

/**
 * Ton der Vorschau im Muster-Editor: dieselben Klänge wie in der Übung – kurze Wechseltöne
 * ([ToneCuePlayer]) im Modus „Töne“, der durchgehende Ton ([ContinuousTonePlayer]) im Modus
 * „Dauerton“ –, mit der eingestellten Lautstärke und Zäsur ([CueSettings]). Im Modus „Aus“, bei
 * ausgeschaltetem Schalter ([enabled]) oder solange [blocked] ist (App im Hintergrund, eine Übung
 * läuft) klingt nichts. Haptik und Abschluss-Gong gehören nicht zur Vorschau.
 *
 * Getaktet wird von außen: [onPhase] zu jedem Phasenbeginn (wie `PhaseAudio` der Session),
 * [stop] bei Pause und Verlassen. Audio-Fokus wie die Session (`SessionAudioCoordinator`): transient
 * mit Ducken, angefordert vor jedem hörbaren Ton, abgegeben mit [stop]. Aufrufe nur vom
 * Main-Thread.
 */
class PreviewCuePlayer(private val output: PreviewSoundOutput) {

    private var settings: CueSettings? = null

    /** Seit dem letzten [stop] angefordert (auch wenn abgelehnt) – [stop] gibt dann ab. */
    private var focusHeld = false
    private var released = false

    /** Lautsprecher-Schalter der Vorschau; aus = sofort still. */
    var enabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (!audible) stop()
        }

    /** Vorübergehend still (App im Hintergrund, eine Übung oder Meditation läuft). */
    var blocked: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (!audible) stop()
        }

    /** Klingt ein [onPhase] gerade hörbar? */
    val audible: Boolean
        get() = enabled && !blocked && !released && (settings?.soundMode ?: SoundMode.OFF) != SoundMode.OFF

    /**
     * Übernimmt Klangmodus, Lautstärke und Zäsur; ein anderer Klangmodus beendet den laufenden Ton.
     * Vor dem ersten Aufruf klingt nichts.
     */
    fun updateSettings(next: CueSettings) {
        if (released || next == settings) return
        val modeChanged = next.soundMode != settings?.soundMode
        settings = next
        output.apply(next)
        if (modeChanged || !audible) stop()
    }

    /**
     * Beginn (bzw. Fortsetzen) einer Vorschau-Phase. [phaseBegins] = false beim Fortsetzen mitten
     * in einer Phase oder beim Einschalten des Tons: dann kein Wechselton (wie die Session beim
     * Fortsetzen), der Dauerton läuft mit [PhaseAudio.durationMs] als Restdauer an.
     */
    fun onPhase(phase: PhaseAudio, phaseBegins: Boolean = true) {
        if (!audible) return
        when (settings?.soundMode) {
            SoundMode.CUES -> if (phaseBegins) {
                holdFocus()
                output.cue(phase.type.toCueEvent())
            }

            SoundMode.CONTINUOUS -> {
                holdFocus()
                output.continuous(phase)
            }

            SoundMode.OFF, null -> Unit
        }
    }

    /** Pause, Verlassen, Hintergrund: Dauerton ausblenden, Audio-Fokus abgeben. Mehrfach aufrufbar. */
    fun stop() {
        if (released) return
        output.silence()
        if (focusHeld) {
            focusHeld = false
            output.abandonFocus()
        }
    }

    /**
     * Kopfhörer abgezogen, solange die Vorschau klingt: [action] (die Seite schaltet den Ton der
     * Vorschau aus, S-48).
     */
    fun onUnplugged(action: () -> Unit) = output.onUnplugged(action)

    /** Endgültig freigeben (Seite verlassen); danach klingt nichts mehr. */
    fun release() {
        if (released) return
        stop()
        released = true
        output.release()
    }

    /**
     * Vor jedem hörbaren Ton anfordern: Hält die Ausgabe den Fokus, tut der Antrag nichts; wurde er
     * abgelehnt (Anruf), fragt der nächste Ton neu, statt die Vorschau stumm zu verriegeln.
     */
    private fun holdFocus() {
        focusHeld = true
        output.requestFocus()
    }
}

/**
 * Klang-Ausgabe der Vorschau, getrennt von der Logik in [PreviewCuePlayer], damit Tests ohne
 * `AudioTrack` laufen. Die echte Ausgabe ist [AppPreviewSoundOutput].
 */
interface PreviewSoundOutput {
    /** Lautstärke und Zäsur des durchgehenden Tons übernehmen. */
    fun apply(settings: CueSettings)

    /** Kurzer Wechselton zum Phasenbeginn (Modus „Töne“). */
    fun cue(event: CueEvent)

    /** Durchgehender Ton: anlaufen lassen bzw. der neuen Phase folgen (Modus „Dauerton“). */
    fun continuous(phase: PhaseAudio)

    /** Durchgehenden Ton weich ausblenden und beenden; ohne laufenden Ton nichts. */
    fun silence()

    /** Fokus anfordern; mehrfach aufrufbar (gehalten = nichts, abgelehnt = neu fragen). */
    fun requestFocus()

    fun abandonFocus()

    /** Kopfhörer abgezogen, solange der Fokus gehalten ist: [action] aufrufen. Ohne Gerät nichts. */
    fun onUnplugged(action: () -> Unit) = Unit

    /** Threads und Audio-Fokus freigeben. */
    fun release()
}

/**
 * Echte Ausgabe: eigene Instanzen von [ToneCuePlayer] und [ContinuousTonePlayer] (die Session hält
 * ihre im ViewModel, `SessionAudioCoordinator`) und derselbe Audio-Fokus ([AudioFocusController]).
 * Der Dauerton wird wie am Session-Ende erst ausgeblendet und nach [FADE_OUT_MS] beendet – kein
 * Knacken; läuft die Vorschau vorher weiter, bleibt der Thread. Nimmt ein Anruf oder eine andere
 * App den Fokus, schweigt die Vorschau, bis er zurückkommt (AUDIO-02). Den Kopfhörer überwacht sie nur,
 * solange sie den Fokus hält (S-48).
 */
class AppPreviewSoundOutput(context: Context) : PreviewSoundOutput {
    private val tone = ToneCuePlayer()
    private val continuousTone = ContinuousTonePlayer()
    private val focus = AudioFocusController(context) { lost ->
        when {
            lost -> continuousTone.mute()
            continuousTone.isRunning -> continuousTone.unmute()
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val unplug = UnplugWatcher(context)
    private var unplugAction: () -> Unit = {}
    private val stopContinuous = Runnable { continuousTone.stop() }

    override fun apply(settings: CueSettings) {
        tone.setVolume(settings.volume)
        continuousTone.setVolume(settings.volume)
        continuousTone.setEmphasis(settings.transition)
    }

    override fun cue(event: CueEvent) {
        if (!focus.lost) tone.play(event)
    }

    override fun continuous(phase: PhaseAudio) {
        handler.removeCallbacks(stopContinuous)
        continuousTone.start()
        continuousTone.onPhase(phase.type, phase.durationMs, phase.open, raiseGain = !focus.lost, level = phase.level, elapsedMs = phase.elapsedMs)
    }

    override fun silence() {
        if (!continuousTone.isRunning) return
        continuousTone.mute()
        handler.removeCallbacks(stopContinuous)
        handler.postDelayed(stopContinuous, FADE_OUT_MS)
    }

    override fun requestFocus() {
        focus.request()
        unplug.watch { unplugAction() }
    }

    override fun abandonFocus() {
        unplug.stop()
        focus.abandon()
    }

    override fun onUnplugged(action: () -> Unit) {
        unplugAction = action
    }

    override fun release() {
        unplug.stop()
        handler.removeCallbacks(stopContinuous)
        continuousTone.stop()
        tone.release()
        focus.abandon()
    }

    private companion object {
        /** Wie im SessionAudioCoordinator: 60 ms Ausblende des Players plus Puffer-Latenz. */
        const val FADE_OUT_MS = 180L
    }
}
