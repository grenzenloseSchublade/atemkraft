package app.atemkraft.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.atemkraft.cue.AudioFocusController
import app.atemkraft.cue.ToneCuePlayer
import app.atemkraft.domain.ToneVolume

/**
 * Vorhören des Gongs in den Einstellungen – eigene Instanz, unabhängig von laufenden Sessions. Hält
 * wie die Sessions Audio-Fokus, solange der Gong klingt (AUDIO-02): fremde Musik wird geduckt statt
 * überlagert und danach wieder laut. Ein Anruf blendet den Gong aus. Weitere Taps während der
 * Wiedergabe zählen nicht (kein Aufstauen). Aufrufe vom Main-Thread.
 */
class GongPreview(context: Context) {
    private val tone = ToneCuePlayer()
    private val focus = AudioFocusController(context) { lost -> if (lost) tone.silenceGong() }
    private val main = Handler(Looper.getMainLooper())
    private var playing = false

    fun play(volume: ToneVolume, long: Boolean) {
        if (playing) return
        focus.request()
        if (focus.lost) return // verweigert, etwa während eines Anrufs: still bleiben
        playing = true
        tone.setVolume(volume)
        // Fokus erst abgeben, wenn der Track nach Ton, Stille und Reserve gestoppt ist.
        tone.previewGong(long) {
            main.post {
                playing = false
                focus.abandon()
            }
        }
    }
}
