package app.atemkraft.cue

import android.content.Context
import androidx.annotation.StringRes
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.domain.PhaseType
import kotlinx.coroutines.flow.StateFlow

/**
 * Sprachansage der Phasen in der Atemübung („ein“, „halten“, „aus“): ein dritter Kanal neben Ton
 * und Vibration, damit die Übung auch mit geschlossenen Augen und ohne Vibration führt. Dieselben
 * Stimmen wie die Meditation – die dort gewählte neuronale Stimme ([PiperSpeechGuide], offline nach
 * dem Download), sonst das System-TTS ([SpeechGuide]); Netz braucht nur der Download per Tap.
 *
 * Eigene Engine-Instanzen, weil der Ablauf der Atemübung unabhängig von der Meditation läuft; das
 * Modell der Stimme lädt nur, solange eine Übung mit Ansage läuft ([prepare]), und wird danach
 * freigegeben ([release]). Jede Ansage verdrängt die vorige ([say]): Eine verspätete Ansage (Modell
 * lädt noch) darf nie in die nächste Phase hineinsprechen.
 */
class PhaseVoice(context: Context, voiceModelManager: VoiceModelManager) {
    private val appContext = context.applicationContext
    private val piper = PiperSpeechGuide(context, voiceModelManager)
    private val system = SpeechGuide(context)

    /** Neuronale Stimme installiert (dann sie, sonst System-TTS). */
    private val piperReady: StateFlow<Boolean> = piper.available

    /** Spricht überhaupt eine Stimme? Ohne Stimme bleibt es beim Wechselton. */
    val available: Boolean get() = piper.available.value || system.available.value

    /** Die im Tab Meditation gewählte Stimme (null = System-TTS). */
    fun setActiveVoice(voiceId: String?) = piper.setActiveVoice(voiceId)

    /** Vor der Übung: System-TTS anbinden und das Modell der Stimme laden (idempotent). */
    fun prepare() {
        system.ensureInit()
        piper.ensureInit()
    }

    /** Ansage für eine Phase; verdrängt eine noch laufende oder wartende. */
    fun say(word: PhaseWord) {
        stop()
        val text = appContext.getString(word.text)
        if (piperReady.value) piper.speak(text) else system.speak(text)
    }

    /** Laufende Ansage abbrechen (Pause, Ende vor dem Gong, Stummschalten). */
    fun stop() {
        piper.stop()
        system.stop()
    }

    /** Nach der Übung: Modell freigeben, die Wahl der Stimme bleibt. */
    fun release() = piper.unload()
}

/** Was die Stimme zu Beginn einer Phase sagt. */
enum class PhaseWord(@StringRes val text: Int) {
    INHALE(R.string.say_inhale),
    INHALE_MORE(R.string.say_inhale_more),
    EXHALE(R.string.say_exhale),
    HOLD(R.string.say_hold),
    REST(R.string.say_rest),
    ;

    companion object {
        /**
         * Kürzeste Phase mit Ansage: Ein Wort braucht samt Anlauf der Stimme gut eine halbe bis eine
         * Sekunde; kürzere Phasen (Feueratmung, Seufzer-Nachatmen) würden sich stauen und hinter
         * dem Atem herlaufen. Sie behalten ihren Wechselton.
         */
        const val MIN_SPOKEN_MS = 1_500L

        /**
         * Wort zum Beginn einer Phase, oder `null` (keine Ansage): zu kurz, oder ein Fortsetzen
         * mitten in der Phase ([elapsedMs] > 0) – angesagt wird nur ein echter Beginn. Offene Phasen
         * (Halten bis zum Atemimpuls) werden immer angesagt.
         */
        fun at(type: PhaseType, durationMs: Long, open: Boolean, elapsedMs: Long): PhaseWord? {
            if (!open && (elapsedMs > 0L || durationMs < MIN_SPOKEN_MS)) return null
            return when (type) {
                PhaseType.INHALE -> INHALE
                PhaseType.INHALE_TOP_UP -> INHALE_MORE
                PhaseType.EXHALE -> EXHALE
                PhaseType.HOLD_FULL, PhaseType.HOLD_EMPTY -> HOLD
                PhaseType.REST -> REST
            }
        }
    }
}
