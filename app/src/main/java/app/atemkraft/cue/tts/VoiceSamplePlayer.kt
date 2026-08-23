package app.atemkraft.cue.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Spielt die kurzen, mitgelieferten Vorhör-Clips (in `assets/voice_samples`) einer Stimme ab –
 * **vor** dem Download, damit man Stimmen vergleichen kann, ohne 60–110 MB zu laden. Immer nur ein
 * Clip gleichzeitig; app-weit.
 */
class VoiceSamplePlayer(context: Context) {

    private val appContext = context.applicationContext

    @Volatile private var player: MediaPlayer? = null

    /** Vorherigen Clip stoppen und den Asset-Clip [assetPath] abspielen. */
    fun play(assetPath: String) {
        stop()
        val afd = runCatching { appContext.assets.openFd(assetPath) }.getOrNull() ?: return
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            mp.setOnCompletionListener { it.release(); if (player === it) player = null }
            mp.setOnErrorListener { p, _, _ -> p.release(); if (player === p) player = null; true }
            mp.prepare()
            player = mp
            mp.start()
        } catch (_: Exception) {
            // Bei Fehler die native Instanz sicher freigeben (kein Leak).
            runCatching { mp.release() }
        } finally {
            runCatching { afd.close() }
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
    }

    fun release() = stop()
}
