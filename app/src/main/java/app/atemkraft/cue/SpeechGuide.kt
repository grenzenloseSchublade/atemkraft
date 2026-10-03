package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

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
class SpeechGuide(context: Context) : SpeechEngine {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _available = MutableStateFlow(false)

    /** true, sobald eine nutzbare deutsche Stimme bereitsteht. */
    override val available: StateFlow<Boolean> = _available.asStateFlow()

    @Volatile private var ready = false

    @Volatile private var pending = false

    @Volatile private var tts: TextToSpeech? = null
    private var utteranceCounter = 0

    /** Startet die TTS-Initialisierung (idempotent). Ergebnis landet in [available]. */
    override fun ensureInit() {
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

    /** Beste deutsche Offline-Stimme wählen (netzgebundene ausgelassen – offline-first). */
    private fun applyVoiceSelection(engine: TextToSpeech) {
        val best = runCatching {
            engine.voices.orEmpty()
                .filter {
                    it.locale.language == Locale.GERMAN.language &&
                        !it.isNetworkConnectionRequired &&
                        it.quality >= Voice.QUALITY_LOW
                }
                .maxByOrNull { it.quality }
        }.getOrNull()
        if (best != null) runCatching { engine.voice = best }
    }

    /**
     * Spricht [text], sofern bereit und die Medien-Lautstärke nicht auf 0 steht (dann wollte
     * der Nutzer bewusst Stille). Nicht-blockierend.
     */
    override fun speak(text: String) {
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

    /** Bricht laufende Ausgabe ab (z. B. Pause/Stopp), Engine bleibt bestehen. */
    override fun stop() {
        tts?.stop()
    }

    /** Gibt die Engine frei. Nach Aufruf wird nichts mehr gesprochen. */
    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        pending = false
    }
}
