package app.atemkraft.cue

import app.atemkraft.domain.ToneVolume
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

/**
 * AUDIO-01: Jeder Ton hat Attack ≥ 8 ms, Release ≥ 60 ms und eine Spitze ≤ 0,95 Full Scale
 * bei jeder Lautstärkestufe – sonst klickt es oder clippt.
 *
 * Wechselton und Gong werden als PCM erzeugt und gemessen ([synthesizeCue], [synthesizeGong]).
 * Gemessen wird der Pegelverlauf (gleitendes Maximum über eine Periode): Attack ist die Zeit
 * bis 90 % des Anfangspegels, Release die Zeit vom letzten 90-%-Punkt bis zum Tonende (ohne
 * nachfolgende Stille). Eine lineare Rampe der Länge L liefert 0,9 · L. Der Dauerton entsteht in einem Audio-Thread; er wird
 * über seine Generator-Konstanten geprüft (Pegel-Rampe, Grundpegel, Stufenfaktor).
 */
class ToneEnvelopeTest {

    private val sampleRate = ToneCuePlayer.SAMPLE_RATE
    private fun samplesFor(ms: Double) = (sampleRate * ms / 1000.0).toInt()

    /** Höchstes |Sample| relativ zu Full Scale. */
    private fun peakFs(pcm: ShortArray) = pcm.maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toDouble()

    /** Pegelverlauf: größtes |Sample| im Fenster [i, i + w) (vorwärts) bzw. (i − w, i] (rückwärts). */
    private fun envelope(pcm: ShortArray, forward: Boolean): IntArray {
        val w = samplesFor(ENVELOPE_WINDOW_MS)
        return IntArray(pcm.size) { i ->
            val range = if (forward) i until minOf(pcm.size, i + w) else maxOf(0, i - w + 1)..i
            range.maxOf { abs(pcm[it].toInt()) }
        }
    }

    /** Zeit (ms) vom Tonbeginn, bis der Pegel 90 % seines Anfangs-Maximums erreicht. */
    private fun attackMs(pcm: ShortArray): Double {
        val env = envelope(pcm, forward = false)
        val window = minOf(pcm.size, samplesFor(REFERENCE_WINDOW_MS))
        val ref = (0 until window).maxOf { env[it] }
        val start = pcm.indexOfFirst { it.toInt() != 0 }.coerceAtLeast(0)
        val reached = (0 until window).first { env[it] >= 0.9 * ref }
        return (reached - start) * 1000.0 / sampleRate
    }

    /** Zeit (ms), in der der Pegel vom 90-%-Punkt bis zum letzten hörbaren Sample abfällt. */
    private fun releaseMs(pcm: ShortArray): Double {
        // Ende des Tons: nachfolgende echte Stille (Gong) zählt nicht.
        var end = pcm.size
        while (end > 0 && pcm[end - 1].toInt() == 0) end--
        val env = envelope(pcm, forward = true)
        val from = maxOf(0, end - samplesFor(REFERENCE_WINDOW_MS))
        val ref = (from until end).maxOf { env[it] }
        val last90 = (from until end).last { env[it] >= 0.9 * ref }
        return (end - last90) * 1000.0 / sampleRate
    }

    private fun check(name: String, pcm: ShortArray): List<String> = buildList {
        val peak = peakFs(pcm)
        if (peak > MAX_PEAK_FS) add("$name: Spitze ${"%.3f".format(peak)} FS > $MAX_PEAK_FS")
        // Eine lineare Rampe der Länge L erreicht 90 % nach 0,9 · L.
        val attack = attackMs(pcm)
        if (attack < 0.9 * MIN_ATTACK_MS - TOLERANCE_MS) add("$name: Attack ${"%.1f".format(attack)} ms (90 %) < $MIN_ATTACK_MS ms")
        val release = releaseMs(pcm)
        if (release < 0.9 * MIN_RELEASE_MS - TOLERANCE_MS) add("$name: Release ${"%.1f".format(release)} ms (90 %) < $MIN_RELEASE_MS ms")
    }

    /** Die Messung selbst: Ein Ton mit 2 ms Attack und 10 ms Release muss auffallen. */
    @Test
    fun `Messung erkennt zu kurze Rampen`() {
        val n = samplesFor(180.0)
        val attack = samplesFor(2.0)
        val release = samplesFor(10.0)
        val pcm = ShortArray(n) { i ->
            val env = minOf(1.0, i.toDouble() / attack, (n - i).toDouble() / release)
            (sin(2 * Math.PI * 440 * i / sampleRate) * env * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }
        val problems = check("Probe", pcm)
        assertTrue(problems.joinToString(), problems.size == 2)
    }

    @Test
    fun `Wechseltöne halten Attack, Release und Spitze ein`() {
        val problems = ToneVolume.entries.flatMap { volume ->
            listOf(528.0, 396.0, 440.0).flatMap { hz ->
                check("Wechselton $hz Hz/$volume", synthesizeCue(hz, 180, cueVolumeScale(volume)))
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `Gong hält Attack, Release und Spitze ein`() {
        val problems = ToneVolume.entries.flatMap { volume ->
            listOf(true, false).flatMap { long ->
                check("Gong ${if (long) "lang" else "kurz"}/$volume", synthesizeGong(long, cueVolumeScale(volume)))
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `Dauerton hält Attack und Spitze ein`() {
        val fadeMs = ContinuousTonePlayer.GAIN_FADE_SECONDS * 1000
        assertTrue("Dauerton: Attack $fadeMs ms < $MIN_ATTACK_MS ms", fadeMs >= MIN_ATTACK_MS)
        val peak = ToneVolume.entries.maxOf { ContinuousTonePlayer.volumeScaleFor(it) } *
            ContinuousTonePlayer.AMPLITUDE / Short.MAX_VALUE
        assertTrue("Dauerton: Spitze $peak FS > $MAX_PEAK_FS", peak <= MAX_PEAK_FS)
    }

    /**
     * Bekannter Befund (noch ohne Backlog-ID): Der Dauerton blendet bei Pause, Stumm und Ende
     * mit derselben 15-ms-Rampe aus wie beim Einblenden – kürzer als 60 ms Release. Schlägt
     * fehl, sobald das behoben ist; dann diesen Test in die Prüfung oben übernehmen.
     */
    @Test
    fun `bekannt - Dauerton-Release unter 60 ms`() {
        val fadeMs = ContinuousTonePlayer.GAIN_FADE_SECONDS * 1000
        assertTrue(
            "Dauerton-Release ist jetzt $fadeMs ms ≥ $MIN_RELEASE_MS ms – Befund behoben, Test umstellen",
            fadeMs < MIN_RELEASE_MS,
        )
    }

    private companion object {
        const val MIN_ATTACK_MS = 8.0
        const val MIN_RELEASE_MS = 60.0
        const val MAX_PEAK_FS = 0.95

        /** Fenster für den Pegelverlauf: länger als eine Periode des tiefsten Teiltons (330 Hz ≈ 3 ms). */
        const val ENVELOPE_WINDOW_MS = 3.5

        /** Bezugsbereich am Anfang bzw. Ende des Tons für den Maximalpegel. */
        const val REFERENCE_WINDOW_MS = 150.0

        /** Messunschärfe durch Fenster und Sample-Raster. */
        const val TOLERANCE_MS = 0.5
    }
}
