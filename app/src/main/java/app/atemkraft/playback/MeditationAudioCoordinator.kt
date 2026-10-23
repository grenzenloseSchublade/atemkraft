package app.atemkraft.playback

import android.content.Context
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.PiperSpeechGuide
import app.atemkraft.cue.SpeechEngine
import app.atemkraft.cue.SpeechGuide
import app.atemkraft.cue.ToneCuePlayer
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
    private val audioFocus = AudioFocusController(context)

    private val piper = PiperSpeechGuide(context, voiceModelManager)
    private val system = SpeechGuide(context)

    private val scope = supervisedScope(Dispatchers.Main.immediate)

    /** Ist die aktive neuronale Stimme einsatzbereit? Steuert Routing UND UI (gleiche Quelle). */
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

    /** Beide Engines vorbereiten (System sofort, Piper baut die aktive Stimme, falls installiert). */
    override fun prepareSpeech() {
        system.ensureInit()
        piper.ensureInit()
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

    override fun requestFocus() = audioFocus.request()

    override fun abandonFocus() = audioFocus.abandon()

    /** Weicher Klangschalen-Gong (Start, Intervall, Ende) – außer stummgeschaltet. */
    override fun gong() {
        if (!muted) tonePlayer.play(CueEvent.FINISH)
    }

    override fun speak(text: String) {
        if (muted) return
        engine().speak(text)
    }

    /** Laufende Ansage abbrechen – beide Engines, damit nichts hängen bleibt. */
    override fun stopSpeech() {
        piper.stop()
        system.stop()
    }

    /** Beim Wegräumen aufrufen: gibt Engines, Player und Audio-Fokus frei. */
    fun release() {
        piper.release()
        system.release()
        tonePlayer.release()
        audioFocus.abandon()
    }
}
