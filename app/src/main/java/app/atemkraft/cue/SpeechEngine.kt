package app.atemkraft.cue

import kotlinx.coroutines.flow.StateFlow

/**
 * Gemeinsame Oberfläche der beiden Sprach-Engines ([SpeechGuide] = System-TTS-Fallback,
 * [PiperSpeechGuide] = neuronale Katalog-Stimmen), damit der Koordinator sie austauschbar
 * ansprechen kann.
 */
interface SpeechEngine {
    /** true, sobald die Engine eine nutzbare Stimme bereit hat. */
    val available: StateFlow<Boolean>

    /** Initialisierung anstoßen (idempotent, nicht-blockierend). */
    fun ensureInit()

    /** [text] sprechen (reiht sich hinter laufende Ansagen ein; still bei Medienlautstärke 0). */
    fun speak(text: String)

    /** Laufende Ausgabe abbrechen; die Engine bleibt nutzbar. */
    fun stop()

    /** Engine endgültig freigeben. */
    fun release()
}
