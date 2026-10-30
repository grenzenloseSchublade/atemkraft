package app.atemkraft.playback

import android.content.Context
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.PiperSpeechGuide
import app.atemkraft.cue.SpeechEngine
import app.atemkraft.cue.SpeechGuide
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.UnplugWatcher
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.supervisedScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Bündelt die Audio-Bausteine des Meditations-Tabs: Klangschalen-Gong ([ToneCuePlayer]),
 * Audio-Fokus und gesprochene Anleitung. Für die Sprache gibt es die **neuronalen Katalog-Stimmen**
 * ([PiperSpeechGuide] – die aktive Stimme wird gewählt) und als **Fallback** die geräteeigene
 * [SpeechGuide]. Ist eine neuronale Stimme aktiv, wird sie genutzt; sonst das System-TTS.
 */
class MeditationAudioCoordinator(
    context: Context,
    private val voiceModelManager: VoiceModelManager,
) : MeditationAudio {
    private val tonePlayer = ToneCuePlayer()

    private val piper = PiperSpeechGuide(context, voiceModelManager)
    private val system = SpeechGuide(context)

    /**
     * Nimmt ein Anruf oder eine andere App den Fokus, verstummen Gong und Ansage sofort; die
     * Meditation läuft still weiter, bis er zurückkommt (AUDIO-02).
     */
    private val audioFocus = AudioFocusController(context) { lost ->
        if (lost) {
            stopSpeech()
            tonePlayer.silenceGong()
        }
    }

    /** Kopfhörer abgezogen: nur während einer Meditation registriert ([watchUnplug]). */
    private val unplug = UnplugWatcher(context)

    private val scope = supervisedScope(Dispatchers.Main.immediate)

    /**
     * Ist die aktive neuronale Stimme installiert (ihr Modell lädt erst [prepareSpeech])? Steuert
     * Routing UND UI (gleiche Quelle); eine Ansage wartet hinter dem Laden.
     */
    override val piperReady: StateFlow<Boolean> = piper.available

    /** Verfügbarkeit einer Stimme (neuronal ODER System) – für den Sprach-Schalter im UI. */
    override val speechAvailable: StateFlow<Boolean> =
        combine(piper.available, system.available) { p, s -> p || s }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** Transienter In-Session-Stummschalter (übersteuert Gong/Sprache, ohne die Einstellung zu ändern). */
    @Volatile private var muted = false

    override fun setMuted(value: Boolean) {
        muted = value
        if (value) stopSpeech() // laufende Ansage sofort verstummen
    }

    /** Aktive Engine: Piper, sobald einsatzbereit; sonst System-TTS-Fallback. */
    private fun engine(): SpeechEngine = if (piperReady.value) piper else system

    /** System-TTS anbinden, damit [speechAvailable] stimmt – ohne ein Sprachmodell zu laden. */
    override fun checkSpeech() = system.ensureInit()

    /** Für eine Meditation mit Sprache: System-TTS und das Modell der aktiven Stimme laden (falls installiert). */
    override fun prepareSpeech() {
        system.ensureInit()
        piper.ensureInit()
    }

    /** Modell der neuronalen Stimme freigeben (nach der Meditation, bei Speicherdruck; S-45). */
    override fun releaseSpeech() = piper.unload()

    override fun watchUnplug(onUnplugged: (() -> Unit)?) {
        if (onUnplugged != null) unplug.watch(onUnplugged) else unplug.stop()
    }

    /** Aktive neuronale Stimme wählen (null = keine → Fallback). */
    override fun setActiveVoice(voiceId: String?) = piper.setActiveVoice(voiceId)

    /** Ist [voiceId] die aktuell gewünschte (aktive) Stimme? */
    override fun isActiveVoice(voiceId: String): Boolean = piper.isDesired(voiceId)

    /**
     * Stimme entfernen: erst die Engine sicher freigeben (falls diese Stimme geladen ist), DANN die
     * Dateien löschen – nacheinander auf dem seriellen Engine-Thread, damit keine laufende
     * Synthese auf gelöschte Dateien zugreift.
     */
    override fun deleteVoice(voiceId: String) = piper.releaseVoiceThen(voiceId) { voiceModelManager.delete(voiceId) }

    override fun updateVolume(volume: ToneVolume) = tonePlayer.setVolume(volume)

    /** Gong-Ausklang lang/kurz setzen. */
    override fun setGongLong(long: Boolean) = tonePlayer.setGongLong(long)

    /** Gesamt-Wiedergabedauer des Gongs (Ton + Stille) fürs aktuelle Profil. */
    override fun gongTotalMs(): Int = tonePlayer.gongTotalMs()

    override fun abandonFocus() = audioFocus.abandon()

    /** Blendet einen noch klingenden Gong kurz aus (neuer Start, „Nochmal“). */
    override fun silenceGong() = tonePlayer.silenceGong()

    /**
     * Weicher Klangschalen-Gong (Start, Intervall, Ende) – außer stummgeschaltet oder ohne Fokus. Der
     * Fokus wird erst hier angefordert, wenn wirklich etwas klingt (nicht im Countdown); ein
     * abgelehnter Antrag (Anruf) wird so beim nächsten Ton wiederholt (AUDIO-02).
     */
    override fun gong() {
        if (muted) return
        audioFocus.request()
        if (!audioFocus.lost) tonePlayer.play(CueEvent.FINISH)
    }

    /** Ansage – wie [gong] mit Fokus erst beim Sprechen. */
    override fun speak(text: String) {
        if (muted) return
        audioFocus.request()
        if (audioFocus.lost) return
        engine().speak(text)
    }

    /** Laufende Ansage abbrechen – beide Engines, damit nichts hängen bleibt. */
    override fun stopSpeech() {
        piper.stop()
        system.stop()
    }

    /** Beim Wegräumen aufrufen: gibt Engines, Player und Audio-Fokus frei. */
    fun release() {
        unplug.stop()
        piper.release()
        system.release()
        tonePlayer.release()
        audioFocus.abandon()
    }
}
