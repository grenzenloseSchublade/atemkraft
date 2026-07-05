package app.atemkraft.cue

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.atemkraft.domain.ToneVolume
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Spielt kurze, weiche Sinus-Töne bei Phasenwechseln. Die Töne werden zur Laufzeit
 * synthetisiert (keine Audio-Dateien im APK), klingen je Phase unterschiedlich und
 * funktionieren vollständig offline.
 *
 * Läuft über den Medien-Stream (USAGE_MEDIA), damit die Töne auch im Vibrationsmodus
 * hörbar sind und über die normale Medien-Lautstärke geregelt werden. Aufrufe sind
 * nicht-blockierend (eigener Thread).
 */
class ToneCuePlayer {

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var released = false

    @Volatile
    private var volumeScale = 1f

    fun setVolume(volume: ToneVolume) {
        // Cue-Töne haben höheren Grundpegel (0.5) – Anhebung mit Reserve gegen Clipping.
        volumeScale = when (volume) {
            ToneVolume.QUIET -> 0.9f
            ToneVolume.MEDIUM -> 1.4f
            ToneVolume.LOUD -> 1.85f
        }
    }

    fun play(event: CueEvent) {
        if (released) return
        val frequencyHz = when (event) {
            CueEvent.INHALE -> 528.0
            CueEvent.EXHALE -> 396.0
            CueEvent.HOLD -> 440.0
            CueEvent.FINISH -> 660.0
        }
        val durationMs = if (event == CueEvent.FINISH) 420 else 180
        executor.execute { synthesizeAndPlay(frequencyHz, durationMs) }
    }

    private fun synthesizeAndPlay(frequencyHz: Double, durationMs: Int) {
        if (released) return
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(sampleCount)
        val attackSamples = (SAMPLE_RATE * 0.012).toInt()        // 12 ms Einblende
        val releaseSamples = (SAMPLE_RATE * 0.06).toInt()        // 60 ms Ausblende

        for (i in 0 until sampleCount) {
            val angle = 2.0 * PI * i * frequencyHz / SAMPLE_RATE
            // Linearer Hüllkurven-Verlauf gegen Knackgeräusche.
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > sampleCount - releaseSamples ->
                    (sampleCount - i).toDouble() / releaseSamples
                else -> 1.0
            }
            samples[i] = (sin(angle) * envelope * AMPLITUDE * volumeScale).toInt().toShort()
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack(
            attributes,
            format,
            samples.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        try {
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep((durationMs + 60).toLong())
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** Gibt den Hintergrund-Thread frei. Nach Aufruf werden keine Töne mehr gespielt. */
    fun release() {
        released = true
        executor.shutdown()
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val AMPLITUDE = 0.5 * Short.MAX_VALUE
    }
}
