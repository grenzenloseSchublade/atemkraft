package app.atemkraft.ui.meditation

import android.content.Context
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.SpeechGuide
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.cue.VoiceOption
import app.atemkraft.domain.ToneVolume
import kotlinx.coroutines.flow.StateFlow

/**
 * Bündelt die Audio-Bausteine des Meditations-Tabs: den vorhandenen Klangschalen-Gong
 * ([ToneCuePlayer] mit [CueEvent.FINISH]) für Start-/Intervall-/End-Gong, den Audio-Fokus
 * und die gesprochene Anleitung ([SpeechGuide]). Bewusst imperativ – aufgerufen aus den
 * Coroutinen des [MeditationViewModel] (also im `viewModelScope`), damit Töne/Sprache auch
 * hinter dem Sperrbildschirm weiterlaufen.
 */
class MeditationAudioCoordinator(context: Context) {

    private val tonePlayer = ToneCuePlayer()
    private val audioFocus = AudioFocusController(context)
    private val speech = SpeechGuide(context)

    /** Verfügbarkeit einer deutschen TTS-Stimme (für den Sprach-Schalter im UI). */
    val speechAvailable: StateFlow<Boolean> = speech.available

    /** Wählbare deutsche Stimmen (für die Auswahl im UI). */
    val voices: StateFlow<List<VoiceOption>> = speech.voices

    /** Startet die TTS-Initialisierung (idempotent) – früh aufrufen, damit das UI Bescheid weiß. */
    fun prepareSpeech() = speech.ensureInit()

    /** Bevorzugte Stimme setzen (null = automatisch beste). */
    fun selectVoice(voiceId: String?) = speech.selectVoice(voiceId)

    fun updateVolume(volume: ToneVolume) = tonePlayer.setVolume(volume)

    fun requestFocus() = audioFocus.request()

    fun abandonFocus() = audioFocus.abandon()

    /** Weicher Klangschalen-Gong (Start, Intervall, Ende). */
    fun gong() = tonePlayer.play(CueEvent.FINISH)

    fun speak(text: String) = speech.speak(text)

    fun stopSpeech() = speech.stop()

    /** Beim Wegräumen des ViewModels aufrufen: gibt Engine, Player und Audio-Fokus frei. */
    fun release() {
        speech.release()
        tonePlayer.release()
        audioFocus.abandon()
    }
}
