package app.atemkraft.cue

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.atemkraft.domain.ToneVolume
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Spielt kurze, weiche Sinus-Töne bei Phasenwechseln. Die Töne werden zur Laufzeit
 * synthetisiert (keine Audio-Dateien im APK), klingen je Phase unterschiedlich und
 * funktionieren vollständig offline.
 *
 * Läuft über den Medien-Stream (USAGE_MEDIA), damit die Töne auch im Vibrationsmodus
 * hörbar sind und über die normale Medien-Lautstärke geregelt werden. Aufrufe sind
 * nicht-blockierend.
 *
 * **Zeitverhalten:** Phasen-Töne und Gong laufen auf getrennten Threads. Ein Phasen-Ton belegt
 * seinen Thread nur für Synthese und Track-Aufbau; den Track räumt ein späterer Auftrag weg. So
 * kommt der erste Ton nach „Nochmal“ nie hinter einem 6–13 s langen Gong, und 0,5-s-Phasen
 * (Feueratmung) stauen sich nicht. Ein noch wartender Ton entfällt, sobald ein neuerer kommt. Ein
 * neuer Gong (oder [silenceGong]) blendet einen noch klingenden kurz aus, statt hinter ihm zu warten.
 */
class ToneCuePlayer {

    /** Kurze Wechseltöne: Aufbau sofort, Freigabe zeitversetzt auf demselben Thread. */
    private val cueExecutor = Executors.newSingleThreadScheduledExecutor()

    /** Gong: eigener Thread, er hält ihn für die ganze Ausklangdauer. */
    private val gongExecutor = Executors.newSingleThreadExecutor()

    /** Laufende Nummer je Auftrag: Ein Auftrag, der nicht mehr der neueste ist, entfällt bzw. blendet aus. */
    private val cueSerial = AtomicLong()
    private val gongSerial = AtomicLong()

    @Volatile
    private var released = false

    @Volatile
    private var volumeScale = 1f

    /** true = voller, langer Ausklang (~11,5 s); false = kürzerer Ausklang (~6 s). In Einstellungen wählbar. */
    @Volatile
    private var gongLong = true

    fun setGongLong(long: Boolean) {
        gongLong = long
    }

    private val gongBusy = AtomicBoolean(false)

    /**
     * Vorhören in den Einstellungen: spielt den Gong im gewünschten Profil genau einmal;
     * weitere Taps während der laufenden Wiedergabe werden ignoriert (kein Aufstauen).
     */
    fun previewGong(long: Boolean) {
        if (released) return
        if (!gongBusy.compareAndSet(false, true)) return
        gongLong = long
        playGong(long) { gongBusy.set(false) }
    }

    /** Gesamt-Wiedergabedauer des Gongs (Ton + Stille) fürs aktuelle Profil – für Warte-/Fokus-Timing. */
    fun gongTotalMs(): Int = gongTotalMsFor(gongLong)

    fun setVolume(volume: ToneVolume) {
        volumeScale = cueVolumeScale(volume)
    }

    fun play(event: CueEvent) {
        if (released) return
        when (event) {
            CueEvent.INHALE -> playCue(528.0)
            CueEvent.EXHALE -> playCue(396.0)
            CueEvent.HOLD -> playCue(440.0)
            CueEvent.FINISH -> playGong(gongLong)
        }
    }

    /** Blendet einen noch klingenden Gong kurz aus (eine neue Sitzung beginnt); ein wartender entfällt. */
    fun silenceGong() {
        gongSerial.incrementAndGet()
    }

    /** Kurzer Wechselton (Synthese: [synthesizeCue]); der Auftrag kehrt nach dem Start des Tracks zurück. */
    private fun playCue(frequencyHz: Double) {
        val serial = cueSerial.incrementAndGet()
        submit(cueExecutor) {
            // Überholt, bevor er dran war: lieber auslassen als verspätet nachspielen.
            if (released || serial != cueSerial.get()) return@submit
            val track = startTrack(synthesizeCue(frequencyHz, CUE_MS, volumeScale)) ?: return@submit
            // Reserve über die Puffer-Dauer hinaus: stop() darf nie in noch klingendes Audio fallen
            // (HAL-/Bluetooth-Latenz). Das Pufferende ist ohnehin Stille.
            try {
                cueExecutor.schedule({ stopAndRelease(track) }, (CUE_MS + RELEASE_RESERVE_MS).toLong(), TimeUnit.MILLISECONDS)
            } catch (_: RejectedExecutionException) {
                stopAndRelease(track) // freigegeben, während der Ton anlief
            }
        }
    }

    /** Gong im Profil [long] (Synthese: [synthesizeGong]); [onDone] läuft in jedem Fall danach. */
    private fun playGong(long: Boolean, onDone: () -> Unit = {}) {
        val serial = gongSerial.incrementAndGet() // ein noch klingender Gong blendet jetzt aus
        val accepted = submit(gongExecutor) {
            try {
                if (!released && serial == gongSerial.get()) {
                    ringOut(synthesizeGong(long, volumeScale), gongTotalMsFor(long), serial)
                }
            } finally {
                onDone()
            }
        }
        if (!accepted) onDone()
    }

    /**
     * Spielt den Gong und wartet in kurzen Schritten bis zu seinem Ende. Ist [serial] überholt (neuer
     * Gong, [silenceGong]), blendet er kurz aus statt hart abzubrechen.
     */
    private fun ringOut(samples: ShortArray, durationMs: Int, serial: Long) {
        val track = startTrack(samples) ?: return
        try {
            val endAt = System.nanoTime() + (durationMs + RELEASE_RESERVE_MS) * NANOS_PER_MS
            while (System.nanoTime() < endAt) {
                if (released || serial != gongSerial.get()) {
                    fadeOut(track)
                    break
                }
                Thread.sleep(POLL_MS)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            stopAndRelease(track)
        }
    }

    private fun fadeOut(track: AudioTrack) {
        for (step in FADE_STEPS - 1 downTo 0) {
            track.setVolume(step.toFloat() / FADE_STEPS)
            Thread.sleep(POLL_MS)
        }
    }

    /** Baut einen MODE_STATIC-Track mit [samples] und startet ihn; null, wenn das Gerät keinen hergibt. */
    private fun startTrack(samples: ShortArray): AudioTrack? {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        // Fehlt ein Track (Audio-Ressourcen erschöpft), kostet das einen Ton, nicht die App.
        val track = runCatching {
            AudioTrack(attributes, format, samples.size * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE)
        }.getOrNull() ?: return null
        return try {
            track.write(samples, 0, samples.size)
            track.play()
            track
        } catch (_: IllegalStateException) {
            track.release()
            null
        }
    }

    private fun stopAndRelease(track: AudioTrack) {
        runCatching { track.stop() }
        track.release()
    }

    /** Reiht [task] ein; false, wenn der Player schon freigegeben ist (kein Absturz, N-4). */
    private fun submit(executor: Executor, task: () -> Unit): Boolean = try {
        executor.execute(task)
        true
    } catch (_: RejectedExecutionException) {
        false
    }

    /** Gibt die Hintergrund-Threads frei. Nach Aufruf werden keine Töne mehr gespielt. */
    fun release() {
        released = true
        cueExecutor.shutdown()
        gongExecutor.shutdown()
    }

    internal companion object {
        const val SAMPLE_RATE = 44100
        const val AMPLITUDE = 0.5 * Short.MAX_VALUE

        /** Länge der Wechseltöne. */
        const val CUE_MS = 180

        /** Reserve nach dem Pufferende, bevor ein Track gestoppt wird (HAL-/Bluetooth-Latenz). */
        const val RELEASE_RESERVE_MS = 300

        /** Takt, in dem ein klingender Gong prüft, ob er ausblenden soll; zugleich Schritt der Ausblende. */
        const val POLL_MS = 20L

        /** Ausblende eines überholten Gongs: 8 Schritte à [POLL_MS]. */
        const val FADE_STEPS = 8

        const val NANOS_PER_MS = 1_000_000L

        /** Ton-Fenster „voller Ausklang": Grundton tau 1,6 s → bei 11,5 s ≈ -62 dB (weit unter hörbar). */
        const val GONG_TONE_LONG_MS = 11500

        /** Ton-Fenster „kurz": Grundton tau 0,85 s → bei 6,0 s ≈ -61 dB (weit unter hörbar). */
        const val GONG_TONE_SHORT_MS = 6000

        /** Großzügige echte Stille NACH dem verklungenen Ton – garantiert kein Abschneiden. */
        const val GONG_TAIL_SILENCE_MS = 1500
    }
}

/*
 * Reine Synthese-Funktionen (ohne AudioTrack): Die Wiedergabe oben ruft sie auf, und
 * ToneEnvelopeTest prüft die erzeugten PCM-Daten auf Attack, Release und Spitzenpegel
 * (AUDIO-01) – ohne Gerät.
 */

/** Pegel-Faktor der Cue-Töne je Stufe; Grundpegel 0,5 FS, Anhebung mit Reserve gegen Clipping. */
internal fun cueVolumeScale(volume: ToneVolume): Float = when (volume) {
    ToneVolume.QUIET -> 0.9f
    ToneVolume.MEDIUM -> 1.4f
    ToneVolume.LOUD -> 1.85f
}

/** Gesamtdauer des Gongs (Ton + Stille) in ms für das Profil [long]. */
internal fun gongTotalMsFor(long: Boolean): Int = (if (long) ToneCuePlayer.GONG_TONE_LONG_MS else ToneCuePlayer.GONG_TONE_SHORT_MS) + ToneCuePlayer.GONG_TAIL_SILENCE_MS

/**
 * Abschluss-Gong: klangschalenartig statt Piep – weicher Anschlag, Grundton mit
 * feiner Verstimmung (typisches Schweben) plus inharmonische Obertöne, die schneller
 * abklingen als der Grundton. Amplituden normiert (Summe 1) → kein Clipping.
 */
internal fun synthesizeGong(long: Boolean, volumeScale: Float): ShortArray {
    // Grundton-Abklingzeit + Fenster je nach gewähltem Profil. Fenster ist so bemessen, dass der
    // Ton NATÜRLICH exponentiell bis ~-60 dB (praktisch Stille) ausschwingt – nicht abgeschnitten,
    // nicht künstlich gefadet (nur 150 ms Anti-Klick am Ende). t(-60dB) = tau·ln(1000).
    val fundTau = if (long) 1.6 else 0.85
    val toneMs = if (long) ToneCuePlayer.GONG_TONE_LONG_MS else ToneCuePlayer.GONG_TONE_SHORT_MS
    val toneCount = ToneCuePlayer.SAMPLE_RATE * toneMs / 1000
    // … plus großzügige echte Stille am Ende, damit die Audioausgabe (HAL/Bluetooth-Latenz)
    // das Ende garantiert nicht abschneidet.
    val silenceCount = ToneCuePlayer.SAMPLE_RATE * ToneCuePlayer.GONG_TAIL_SILENCE_MS / 1000
    val samples = ShortArray(toneCount + silenceCount) // ab toneCount bleiben die Werte 0 (Stille)
    // Teilton: Frequenz (Hz), Amplitude, Abkling-Zeitkonstante tau (s).
    val partials = listOf(
        Triple(330.0, 0.50, fundTau),
        Triple(331.6, 0.20, fundTau),
        Triple(894.0, 0.20, 0.45),
        Triple(1698.0, 0.10, 0.20),
    )
    val attackSamples = (ToneCuePlayer.SAMPLE_RATE * 0.008).toInt()
    // Weiche Ausblende (Raised-Cosine) statt linear: kommt mit Steigung 0 auf exakt 0 an →
    // kein hörbarer Knick/Übergang. Greift erst am ohnehin sehr leisen Ende (~-60 dB).
    val releaseSamples = (ToneCuePlayer.SAMPLE_RATE * 0.4).toInt()

    for (i in 0 until toneCount) {
        val t = i.toDouble() / ToneCuePlayer.SAMPLE_RATE
        var v = 0.0
        for ((freq, amp, tau) in partials) {
            v += amp * sin(2.0 * PI * freq * t) * exp(-t / tau)
        }
        val envelope = when {
            i < attackSamples -> i.toDouble() / attackSamples

            i > toneCount - releaseSamples -> {
                // progress 0→1 über die Ausblende; Raised-Cosine 1→0 (Steigung 0 an beiden Enden).
                val progress = (i - (toneCount - releaseSamples)).toDouble() / releaseSamples
                0.5 * (1.0 + cos(PI * progress))
            }

            else -> 1.0
        }
        samples[i] = (v * envelope * ToneCuePlayer.AMPLITUDE * volumeScale).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return samples
}

/** Kurzer Sinus-Wechselton mit linearer Ein-/Ausblende (12 / 60 ms) gegen Knackgeräusche. */
internal fun synthesizeCue(frequencyHz: Double, durationMs: Int, volumeScale: Float): ShortArray {
    val sampleCount = ToneCuePlayer.SAMPLE_RATE * durationMs / 1000
    val samples = ShortArray(sampleCount)
    val attackSamples = (ToneCuePlayer.SAMPLE_RATE * 0.012).toInt() // 12 ms Einblende
    val releaseSamples = (ToneCuePlayer.SAMPLE_RATE * 0.06).toInt() // 60 ms Ausblende

    for (i in 0 until sampleCount) {
        val angle = 2.0 * PI * i * frequencyHz / ToneCuePlayer.SAMPLE_RATE
        // Linearer Hüllkurven-Verlauf gegen Knackgeräusche.
        val envelope = when {
            i < attackSamples -> i.toDouble() / attackSamples

            i > sampleCount - releaseSamples ->
                (sampleCount - i).toDouble() / releaseSamples

            else -> 1.0
        }
        samples[i] = (sin(angle) * envelope * ToneCuePlayer.AMPLITUDE * volumeScale).toInt().toShort()
    }
    return samples
}
