package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Hält transienten Audio-Fokus, solange etwas hörbar ist (duckt fremde Medien sanft, statt sie zu
 * stoppen; AUDIO-02). API 26+.
 *
 * Fokusverlust behandelt der Listener: Nimmt ein Anruf oder eine andere App den Fokus
 * (`AUDIOFOCUS_LOSS`, `AUDIOFOCUS_LOSS_TRANSIENT`), meldet [onLostChange] `true` und der Aufrufer
 * schweigt – Ablauf und Haptik laufen weiter, die App bewertet und pausiert nichts von selbst. Mit
 * `AUDIOFOCUS_GAIN` (Anruf vorbei) kommt `false`, der Ton klingt wieder. Will eine andere App nur
 * kurz ducken (Navigation), wird Atemkraft vom System leiser gestellt (automatisches Ducken ab API
 * 26); dafür braucht es keine eigene Behandlung. Nach einem dauerhaften Verlust holt erst das
 * nächste [request] nach einem [abandon] den Fokus zurück (Pause/Weiter, Ton aus und an) – nicht
 * jede Zustandsänderung, damit die Musik der anderen App nicht ungefragt wieder geduckt wird.
 *
 * Aufrufe und Rückrufe auf dem Main-Thread.
 */
class AudioFocusController(context: Context, private val onLostChange: (lost: Boolean) -> Unit = {}) {

    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val request: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
            .build()

    /** Angefordert und nicht abgegeben; ein weiteres [request] fordert dann nicht erneut an. */
    private var held = false

    /** Hat ein anderer Ton den Fokus genommen (oder wurde er verweigert)? Dann schweigt der Aufrufer. */
    var lost = false
        private set

    fun request() {
        if (held) return
        val granted = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        // Verweigert (etwa während eines Anrufs): still bleiben und beim nächsten Aufruf neu fragen.
        held = granted
        setLost(!granted)
    }

    fun abandon() {
        held = false
        audioManager.abandonAudioFocusRequest(request)
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> setLost(false)
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> setLost(true)
            // AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: Das System duckt selbst.
        }
    }

    private fun setLost(value: Boolean) {
        if (lost == value) return
        lost = value
        onLostChange(value)
    }
}
