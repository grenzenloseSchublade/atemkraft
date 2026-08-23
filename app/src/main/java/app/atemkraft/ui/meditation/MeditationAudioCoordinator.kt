package app.atemkraft.ui.meditation

import android.content.Context
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.PiperSpeechGuide
import app.atemkraft.cue.SpeechGuide
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.VoiceOption
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.domain.ToneVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
) {
    private val tonePlayer = ToneCuePlayer()
    private val audioFocus = AudioFocusController(context)

    private val piper = PiperSpeechGuide(context, voiceModelManager)
    private val system = SpeechGuide(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Ist die aktive neuronale Stimme einsatzbereit? Steuert Routing UND UI (gleiche Quelle). */
    val piperReady: StateFlow<Boolean> = piper.available

    /** Verfügbarkeit einer Stimme (neuronal ODER System) – für den Sprach-Schalter im UI. */
    val speechAvailable: StateFlow<Boolean> =
        combine(piper.available, system.available) { p, s -> p || s }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** Installierte neuronale Stimmen (id + Anzeigename). */
    val voices: StateFlow<List<VoiceOption>> = piper.voices

    private fun activePiper(): Boolean = piperReady.value

    /** Beide Engines vorbereiten (System sofort, Piper baut die aktive Stimme, falls installiert). */
    fun prepareSpeech() {
        system.ensureInit()
        piper.ensureInit()
    }

    /** Aktive neuronale Stimme wählen (null = keine → Fallback). */
    fun setActiveVoice(voiceId: String?) = piper.setActiveVoice(voiceId)

    /** Ist [voiceId] die aktuell gewünschte (aktive) Stimme? */
    fun isActiveVoice(voiceId: String): Boolean = piper.isDesired(voiceId)

    /**
     * Stimme entfernen: erst die Engine sicher freigeben (falls diese Stimme geladen ist), DANN die
     * Dateien löschen – nacheinander auf dem seriellen Engine-Thread, damit keine laufende
     * Synthese auf gelöschte Dateien zugreift.
     */
    fun deleteVoice(voiceId: String) = piper.releaseVoiceThen(voiceId) { voiceModelManager.delete(voiceId) }

    fun previewVoice(text: String) = if (activePiper()) piper.preview(text) else system.preview(text)

    fun updateVolume(volume: ToneVolume) = tonePlayer.setVolume(volume)

    /** Gong-Ausklang lang/kurz setzen. */
    fun setGongLong(long: Boolean) = tonePlayer.setGongLong(long)

    /** Gesamt-Wiedergabedauer des Gongs (Ton + Stille) fürs aktuelle Profil. */
    fun gongTotalMs(): Int = tonePlayer.gongTotalMs()

    fun requestFocus() = audioFocus.request()

    fun abandonFocus() = audioFocus.abandon()

    /** Weicher Klangschalen-Gong (Start, Intervall, Ende). */
    fun gong() = tonePlayer.play(CueEvent.FINISH)

    fun speak(text: String) = if (activePiper()) piper.speak(text) else system.speak(text)

    /** Laufende Ansage abbrechen – beide Engines, damit nichts hängen bleibt. */
    fun stopSpeech() {
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
