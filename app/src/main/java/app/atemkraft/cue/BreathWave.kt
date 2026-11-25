package app.atemkraft.cue

import kotlin.math.roundToInt

/**
 * Wellenform der Vibration „Welle“ für eine Phase: Die Stärke folgt dem Füllstand des Atemkreises
 * (0 = ausgeatmet, 1 = eingeatmet) – beim Einatmen schwillt sie an, beim Ausatmen klingt sie ab,
 * stetig über die Phasengrenzen wie der Kreis ([app.atemkraft.domain.CircleFractions]). Halten ist
 * still: Der Kreis steht, die Hand spürt den Halt am Verstummen (und an der Halte-Raste). Eine
 * gleichmäßig brummende Vibration über lange Halte-Phasen wäre unruhig und kostete Akku.
 *
 * Rein rechnend, ohne Android, damit die Amplituden prüfbar bleiben; abgespielt wird sie von
 * [HapticPlayer.wave].
 */
internal object BreathWave {
    /** Länge eines Schritts der Wellenform; 20 Schritte je Sekunde laufen für die Hand stetig. */
    const val STEP_MS = 50L

    /** Mindestanzahl Schritte – darunter (sehr kurze Phasen) bleibt es still. */
    private const val MIN_STEPS = 2

    /** Obergrenze, damit lange Phasen keine riesigen Felder an den Vibrator-Dienst geben. */
    const val MAX_STEPS = 400

    /** Stärke bei leerer Lunge: knapp spürbar, damit der Beginn des Einatmens nicht fehlt. */
    const val MIN_AMPLITUDE = 30

    /** Stärke bei voller Lunge: deutlich, aber unter dem Anschlag – die Welle soll tragen, nicht rütteln. */
    const val MAX_AMPLITUDE = 200

    /** Stärke (1…255) bei Füllstand [fill]. */
    fun amplitudeAt(fill: Float): Int = (MIN_AMPLITUDE + (MAX_AMPLITUDE - MIN_AMPLITUDE) * fill.coerceIn(0f, 1f)).roundToInt()

    /**
     * Wellenform für die (Rest-)Phase von Füllstand [from] nach [to] mit [durationMs] Restdauer, von
     * der schon [elapsedMs] vergangen sind (Fortsetzen nach der Pause: die Welle geht dort weiter,
     * wo der Kreis steht). `null`: still – Halten (kein Füllstandswechsel) oder zu kurz.
     */
    fun of(from: Float, to: Float, durationMs: Long, elapsedMs: Long = 0L): Waveform? {
        if (from == to || durationMs <= 0L) return null
        val steps = (durationMs / STEP_MS).coerceAtMost(MAX_STEPS.toLong()).toInt()
        if (steps < MIN_STEPS) return null
        val total = (durationMs + elapsedMs).toFloat()
        val start = from + (to - from) * (elapsedMs / total)
        val step = durationMs / steps
        val timings = LongArray(steps) { i -> if (i == steps - 1) durationMs - step * (steps - 1) else step }
        // Mitte jedes Schritts: Die Stärke eines Schritts ist der Füllstand in seiner Mitte.
        val amplitudes = IntArray(steps) { i -> amplitudeAt(start + (to - start) * ((i + 0.5f) / steps)) }
        return Waveform(timings, amplitudes)
    }
}

/** Zeiten (ms) und Stärken (1…255) einer Vibration, gleich lang. */
internal class Waveform(val timings: LongArray, val amplitudes: IntArray)
