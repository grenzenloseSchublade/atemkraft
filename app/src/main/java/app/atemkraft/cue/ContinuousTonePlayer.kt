package app.atemkraft.cue

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Durchgehender, beruhigender Sinuston, dessen Tonhöhe der Atemphase folgt:
 * Einatmen gleitet hoch (G3→C4), Ausatmen runter, Halten konstant (oben bzw. unten).
 * Reine Quarte (4:3), leise; exponentieller Glide (perzeptiv gleichmäßig) und weiche
 * Amplituden-Hüllkurve gegen Knackser. Ein durchgehender Oszillator → keine Brüche.
 *
 * Quelle des Schemas: RESPeRATE/Paced-Breathing + Psychoakustik-Recherche.
 */
class ContinuousTonePlayer {

    @Volatile private var thread: Thread? = null
    @Volatile private var running = false

    // Vom Aufrufer gesetzt, vom Audio-Thread gelesen.
    @Volatile private var startFreq = LOW
    @Volatile private var endFreq = LOW
    @Volatile private var rampTotalSamples = 0L
    @Volatile private var rampPos = 0L
    @Volatile private var targetGain = 0f
    @Volatile private var pendingArticulation = false

    // Wechsel-Zäsur (einstellbar): Tiefpunkt + Längen.
    @Volatile private var articLow = 0.35f
    @Volatile private var articDownSamples = (0.05f * SAMPLE_RATE).toInt()
    @Volatile private var articUpSamples = (0.30f * SAMPLE_RATE).toInt()

    // Lautstärke-Skalierung (Leise/Mittel/Laut) relativ zum Grundpegel.
    @Volatile private var volumeScale = 1f

    fun setVolume(volume: ToneVolume) {
        // Nochmals angehoben, damit „Laut" deutlich trägt (Peak ~0.8 → kein Clipping).
        volumeScale = when (volume) {
            ToneVolume.QUIET -> 1.2f
            ToneVolume.MEDIUM -> 2.0f
            ToneVolume.LOUD -> 3.2f
        }
    }

    fun setEmphasis(emphasis: TransitionEmphasis) {
        when (emphasis) {
            TransitionEmphasis.SOFT -> {
                articLow = 1f // keine hörbare Zäsur
                articDownSamples = (0.04f * SAMPLE_RATE).toInt()
                articUpSamples = (0.20f * SAMPLE_RATE).toInt()
            }
            TransitionEmphasis.MEDIUM -> {
                articLow = 0.45f
                articDownSamples = (0.05f * SAMPLE_RATE).toInt()
                articUpSamples = (0.28f * SAMPLE_RATE).toInt()
            }
            TransitionEmphasis.STRONG -> {
                // Nahe Stille + länger → klar hörbares „kurz weg, blüht wieder auf".
                articLow = 0.02f
                articDownSamples = (0.07f * SAMPLE_RATE).toInt()
                articUpSamples = (0.48f * SAMPLE_RATE).toInt()
            }
        }
    }

    /** True, solange der Audio-Thread läuft (auch stummgeschaltet). */
    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
        running = true
        targetGain = 0f
        thread = Thread { runLoop() }.apply { isDaemon = true; start() }
    }

    /** Neue Phase: setzt den Frequenz-Glide und blendet den Ton ein. */
    fun onPhase(type: PhaseType, durationMs: Long, open: Boolean) {
        val (f0, f1) = freqsFor(type)
        startFreq = f0
        endFreq = f1
        rampTotalSamples = if (open || durationMs <= 0L) 0L else durationMs * SAMPLE_RATE / 1000L
        rampPos = 0L
        targetGain = 1f
        // Kurze Lautstärke-Zäsur markiert den Phasenwechsel hörbar (ohne harten Beep).
        pendingArticulation = true
    }

    /** Blendet den Ton aus (Pause/Ende), Thread läuft weiter. */
    fun mute() {
        targetGain = 0f
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun runLoop() {
        // Identitäts-Check gegen stop()/start() in schneller Folge: sieht ein alter Thread
        // das (geteilte) running-Flag wieder auf true, gehört es bereits dem Nachfolger –
        // ohne diesen Check spielte er als Waise weiter (doppelter Ton).
        val self = Thread.currentThread()
        val track = buildTrack()
        track.play()
        val buffer = ShortArray(BUFFER_SAMPLES)
        var angle = 0.0
        var gain = 0f
        val fadeStep = 1f / (0.015f * SAMPLE_RATE) // ~15 ms Ein-/Ausblende
        // Phasenwechsel-Zäsur (einstellbar): kurz absenken und wieder anschwellen.
        var curLow = articLow
        var curDown = articDownSamples
        var curUp = articUpSamples
        var articStage = 2 // 0 = absenken, 1 = anschwellen, 2 = ruhend
        var articPos = 0
        try {
            while (running && thread === self) {
                if (pendingArticulation) {
                    pendingArticulation = false
                    curLow = articLow
                    curDown = articDownSamples
                    curUp = articUpSamples
                    if (curLow < 1f) { articStage = 0; articPos = 0 } else articStage = 2
                }
                for (i in 0 until BUFFER_SAMPLES) {
                    val freq = if (rampTotalSamples <= 0L) {
                        endFreq
                    } else {
                        val t = rampPos.coerceAtMost(rampTotalSamples).toDouble() / rampTotalSamples
                        startFreq * (endFreq / startFreq).pow(t)
                    }
                    if (rampPos < rampTotalSamples) rampPos++
                    angle += 2.0 * PI * freq / SAMPLE_RATE
                    if (angle > 2.0 * PI) angle -= 2.0 * PI
                    gain = (gain + if (gain < targetGain) fadeStep else -fadeStep).coerceIn(0f, 1f)
                    val articulation = when (articStage) {
                        0 -> {
                            val a = 1f - (1f - curLow) * (articPos.toFloat() / curDown)
                            if (++articPos >= curDown) { articStage = 1; articPos = 0 }
                            a
                        }
                        1 -> {
                            val a = curLow + (1f - curLow) * (articPos.toFloat() / curUp)
                            if (++articPos >= curUp) articStage = 2
                            a
                        }
                        else -> 1f
                    }
                    buffer[i] = (sin(angle) * gain * articulation * AMPLITUDE * volumeScale)
                        .toInt().toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun buildTrack(): AudioTrack {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        return AudioTrack(
            attributes,
            format,
            maxOf(minBuffer, BUFFER_SAMPLES * 2 * 4),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }

    private fun freqsFor(type: PhaseType): Pair<Double, Double> = when (type) {
        PhaseType.INHALE, PhaseType.INHALE_TOP_UP -> LOW to HIGH
        PhaseType.EXHALE -> HIGH to LOW
        PhaseType.HOLD_FULL -> HIGH to HIGH
        PhaseType.HOLD_EMPTY, PhaseType.REST -> LOW to LOW
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SAMPLES = 512
        const val LOW = 196.0    // G3
        const val HIGH = 294.0   // D4 (reine Quinte über G3 – deutlichere Richtung)
        const val AMPLITUDE = 0.25 * Short.MAX_VALUE
    }
}
