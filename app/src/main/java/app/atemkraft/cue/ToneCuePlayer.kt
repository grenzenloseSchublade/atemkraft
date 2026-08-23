package app.atemkraft.cue

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.atemkraft.domain.ToneVolume
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
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
        executor.execute {
            when (event) {
                CueEvent.INHALE -> synthesizeAndPlay(528.0, durationMs = 180)
                CueEvent.EXHALE -> synthesizeAndPlay(396.0, durationMs = 180)
                CueEvent.HOLD -> synthesizeAndPlay(440.0, durationMs = 180)
                CueEvent.FINISH -> synthesizeAndPlayGong()
            }
        }
    }

    /**
     * Abschluss-Gong: klangschalenartig statt Piep – weicher Anschlag, Grundton mit
     * feiner Verstimmung (typisches Schweben) plus inharmonische Obertöne, die schneller
     * abklingen als der Grundton. Amplituden normiert (Summe 1) → kein Clipping.
     */
    private fun synthesizeAndPlayGong() {
        if (released) return
        val sampleCount = SAMPLE_RATE * GONG_DURATION_MS / 1000
        val samples = ShortArray(sampleCount)
        // Teilton: Frequenz (Hz), Amplitude, Abkling-Zeitkonstante tau (s).
        // Grundton + Schwebungspartner klingen bewusst langsam aus (großes tau) → langer,
        // weicher Nachhall; die hohen Obertöne bleiben kurz (nur der Anschlags-Schimmer).
        val partials = listOf(
            Triple(330.0, 0.50, 1.7),
            Triple(331.6, 0.20, 1.7),
            Triple(894.0, 0.20, 0.45),
            Triple(1698.0, 0.10, 0.20),
        )
        val attackSamples = (SAMPLE_RATE * 0.008).toInt()
        // Langer, weicher Ausklang: Das Fenster ist so bemessen, dass der Grundton bis zum Ende
        // fast verklungen ist (~3 %), damit der Gong natürlich austönt statt abgeschnitten zu wirken.
        val releaseSamples = (SAMPLE_RATE * 0.9).toInt()

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            var v = 0.0
            for ((freq, amp, tau) in partials) {
                v += amp * sin(2.0 * PI * freq * t) * exp(-t / tau)
            }
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > sampleCount - releaseSamples ->
                    (sampleCount - i).toDouble() / releaseSamples
                else -> 1.0
            }
            samples[i] = (v * envelope * AMPLITUDE * volumeScale).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        playSamples(samples, GONG_DURATION_MS)
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
        playSamples(samples, durationMs)
    }

    /** Spielt fertige Samples blockierend über einen MODE_STATIC-Track ab. */
    private fun playSamples(samples: ShortArray, durationMs: Int) {
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

        /** Länge des Gongs inkl. langem, natürlichem Ausklingen (Grundton verklingt ~vollständig). */
        const val GONG_DURATION_MS = 5800
    }
}
