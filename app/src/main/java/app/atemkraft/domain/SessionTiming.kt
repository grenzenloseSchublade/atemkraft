package app.atemkraft.domain

import kotlinx.coroutines.delay

/** Zeitkonstanten, die Atem-Session und Meditation teilen (vorher je eine Kopie, DUP-9). */
object SessionTiming {
    /** Länge des Start-Countdowns in Sekunden. */
    const val COUNTDOWN_SECONDS = 3

    /** ~30 fps Tick – flüssig genug für Kreis, Ring und Countdown, schonend für den Akku. */
    const val FRAME_MS = 33L

    /** Ein Schritt des Start-Countdowns. */
    const val COUNTDOWN_STEP_MS = 1000L
}

/**
 * Uhr einer laufenden Sitzung, geteilt von Atem-Session und Meditation: Start-Countdown und die
 * aktive (pausenbereinigte) Zeit, die Bilanz und Logbuch zeigen. [now] ist eine monotone Uhr in
 * Millisekunden – in der App `SystemClock.elapsedRealtime`, im Test die virtuelle Zeit.
 *
 * Pausen zählen nicht, weil [resume] den Bezugspunkt beim Fortsetzen neu setzt; [accrue] schreibt
 * nur die Zeit seit dem letzten Tick fort.
 */
class SessionClock(private val now: () -> Long) {

    /** Aktive Zeit seit [reset] in Millisekunden. */
    var activeMs: Long = 0L
        private set

    private var lastTick = 0L

    /** Aktuelle Zeit der Uhr (für Phasen-Anker und Fristen). */
    fun now(): Long = now.invoke()

    /** Neue Sitzung: aktive Zeit auf 0, ab jetzt zählen. */
    fun reset() {
        activeMs = 0L
        lastTick = now()
    }

    /** Ab jetzt weiterzählen (Lauf beginnt oder geht nach einer Pause weiter). */
    fun resume() {
        lastTick = now()
    }

    /** Die seit dem letzten Tick verstrichene Zeit zur aktiven Zeit zählen. */
    fun accrue() {
        val t = now()
        activeMs += t - lastTick
        lastTick = t
    }

    /**
     * Start-Countdown: meldet [SessionTiming.COUNTDOWN_SECONDS] … 1 je Sekunde über [onSecond]
     * und kehrt nach der letzten Sekunde zurück.
     */
    suspend fun countdown(onSecond: (Int) -> Unit) {
        for (n in SessionTiming.COUNTDOWN_SECONDS downTo 1) {
            onSecond(n)
            delay(SessionTiming.COUNTDOWN_STEP_MS)
        }
    }
}
