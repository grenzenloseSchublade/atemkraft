package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dünner Wrapper um die geräteeigene [TextToSpeech]-Engine für die gesprochene
 * Meditations-Anleitung. Läuft lokal (offline, sofern eine deutsche Stimme installiert ist),
 * ohne Netz. Fehlt eine deutsche Stimme oder scheitert die Initialisierung, bleibt
 * [available] `false` und der Sprach-Schalter im UI wird deaktiviert – die App ist sonst
 * voll funktionsfähig.
 *
 * Best-Practices: `speak()` erst nach erfolgreichem `onInit`, Sprache/Offline-Stimme geprüft,
 * ruhige Rate/Tonhöhe, kein Sprechen bei stummer Medien-Lautstärke, `shutdown()` beim Teardown.
 */
/** Eine wählbare Stimme (id = [android.speech.tts.Voice.getName], label = menschenlesbar). */
data class VoiceOption(val id: String, val label: String)

class SpeechGuide(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _available = MutableStateFlow(false)
    /** true, sobald eine nutzbare deutsche Stimme bereitsteht. */
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    /** Verfügbare deutsche Offline-Stimmen (beste zuerst); leer, solange nicht initialisiert. */
    val voices: StateFlow<List<VoiceOption>> = _voices.asStateFlow()

    @Volatile private var ready = false
    @Volatile private var pending = false
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var preferredVoiceId: String? = null
    private var utteranceCounter = 0

    /** Startet die TTS-Initialisierung (idempotent). Ergebnis landet in [available]. */
    fun ensureInit() {
        // Guard über ready/pending statt `tts != null`: ein Fehl-Init blockiert so keinen Retry.
        if (ready || pending) return
        pending = true
        tts = TextToSpeech(appContext) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                failInit()
                return@TextToSpeech
            }
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            val langResult = engine.setLanguage(Locale.GERMANY)
            val langOk = langResult != TextToSpeech.LANG_MISSING_DATA &&
                langResult != TextToSpeech.LANG_NOT_SUPPORTED
            if (!langOk) {
                failInit()
                return@TextToSpeech
            }
            engine.setSpeechRate(0.85f)
            engine.setPitch(0.9f)
            applyVoiceSelection(engine)
            pending = false
            ready = true
            _available.value = true
        }
    }

    /**
     * Deutsche Offline-Stimmen (beste Qualität zuerst) auflisten und die bevorzugte bzw. beste
     * setzen. Netzgebundene Stimmen werden ausgelassen (offline-first).
     */
    private fun applyVoiceSelection(engine: TextToSpeech) {
        val german = runCatching {
            engine.voices.orEmpty()
                .filter {
                    it.locale.language == Locale.GERMAN.language &&
                        !it.isNetworkConnectionRequired &&
                        it.quality >= Voice.QUALITY_LOW
                }
                .sortedByDescending { it.quality }
        }.getOrDefault(emptyList())

        _voices.value = german.mapIndexed { i, v -> VoiceOption(v.name, labelFor(v, i)) }

        val chosen = german.firstOrNull { it.name == preferredVoiceId } ?: german.firstOrNull()
        if (chosen != null) runCatching { engine.voice = chosen }
    }

    private fun labelFor(voice: Voice, index: Int): String {
        val quality = when {
            voice.quality >= Voice.QUALITY_VERY_HIGH -> "sehr hoch"
            voice.quality >= Voice.QUALITY_HIGH -> "hoch"
            voice.quality >= Voice.QUALITY_NORMAL -> "mittel"
            else -> "einfach"
        }
        return "Stimme ${index + 1} · $quality"
    }

    /** Bevorzugte Stimme setzen (null = automatisch beste); wirkt sofort, falls schon bereit. */
    fun selectVoice(voiceId: String?) {
        preferredVoiceId = voiceId
        tts?.let { applyVoiceSelection(it) }
    }

    /**
     * Spricht [text], sofern bereit und die Medien-Lautstärke nicht auf 0 steht (dann wollte
     * der Nutzer bewusst Stille). Nicht-blockierend.
     */
    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready) return
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, "med-${utteranceCounter++}")
    }

    /**
     * Fehlgeschlagene Initialisierung sauber verwerfen: Engine freigeben und `tts` nullen,
     * damit ein späterer [ensureInit] (z. B. nach Nachinstallieren der deutschen Stimme)
     * erneut versucht statt am `tts != null`-Guard hängenzubleiben.
     */
    private fun failInit() {
        _available.value = false
        ready = false
        pending = false
        tts?.shutdown()
        tts = null
    }

    /**
     * Probe-Ansage für die Stimmen-Auswahl: unterbricht eine laufende Ausgabe (QUEUE_FLUSH) und
     * spielt [text] mit der aktuell gesetzten Stimme. Anders als [speak] ignoriert es die
     * „Medien-Lautstärke 0"-Regel – ein bewusster Tap soll Rückmeldung geben.
     */
    fun preview(text: String) {
        val engine = tts ?: return
        if (!ready) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "med-preview-${utteranceCounter++}")
    }

    /** Bricht laufende Ausgabe ab (z. B. Pause/Stopp), Engine bleibt bestehen. */
    fun stop() {
        tts?.stop()
    }

    /** Gibt die Engine frei. Nach Aufruf wird nichts mehr gesprochen. */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        pending = false
    }
}
