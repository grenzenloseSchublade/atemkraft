package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Hält für die Dauer einer Session transienten Audio-Fokus (duckt fremde Medien sanft,
 * statt sie zu stoppen). API 26+.
 */
class AudioFocusController(context: Context) {

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
            .build()

    fun request() {
        audioManager.requestAudioFocus(request)
    }

    fun abandon() {
        audioManager.abandonAudioFocusRequest(request)
    }
}
