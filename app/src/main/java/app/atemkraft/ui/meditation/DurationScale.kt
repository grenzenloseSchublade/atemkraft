package app.atemkraft.ui.meditation

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Umrechnung Zeitring ↔ Minuten (reine Rechnung, ohne Compose). Eine Runde = 60 min wie ein
 * Küchenwecker: 0° = 12 Uhr, im Uhrzeigersinn, 6° je Minute.
 */
object DialScale {

    private const val DEG_PER_MINUTE = 360f / DURATION_MAX_MINUTES

    /** Winkel (0..360, gerundet auf die nächste Minute) → 1..60; 12 Uhr (0° bzw. 360°) ist 60. */
    fun minutesAt(angleDeg: Float): Int {
        val norm = ((angleDeg % 360f) + 360f) % 360f
        val m = (norm / DEG_PER_MINUTE).roundToInt()
        return if (m == 0) DURATION_MAX_MINUTES else clampMinutes(m)
    }

    /** Minuten → Winkel des Bogenendes; 60 ergibt 360° (voller Bogen). */
    fun angleOf(minutes: Int): Float = clampMinutes(minutes) * DEG_PER_MINUTE

    /** Bildschirmpunkt (y nach unten) relativ zur Mitte → Winkel 0..<360, 0° = 12 Uhr. */
    fun angleOf(x: Float, y: Float, cx: Float, cy: Float): Float {
        val deg = Math.toDegrees(atan2((x - cx).toDouble(), (cy - y).toDouble())).toFloat()
        val norm = if (deg < 0f) deg + 360f else deg
        return if (norm >= 360f) 0f else norm // winzige negative Winkel runden in Float auf 360
    }

    /**
     * Ohne Gedächtnis: Liegt [raw] mehr als eine halbe Runde von [previous] entfernt, gilt der
     * Finger als über 12 Uhr hinaus, und der Wert bleibt an der Grenze, der [previous] näher ist
     * (1 bzw. 60). Startregel von [DialDrag]; beim Ziehen selbst entscheidet [DialDrag].
     */
    fun follow(previous: Int, raw: Int): Int {
        val prev = clampMinutes(previous)
        val next = clampMinutes(raw)
        if (abs(next - prev) <= HALF_ROUND) return next
        return if (prev <= HALF_ROUND) DURATION_MIN_MINUTES else DURATION_MAX_MINUTES
    }

    /** 5er-Marke (längerer Strich, Zahl, deutlichere Haptik-Raste). */
    fun isMajor(minutes: Int): Boolean = minutes % MAJOR_STEP == 0
}

/**
 * Ziehen am Zeitring ohne Sprung über 12 Uhr. Startet mit dem Wert [value] und der Fingerposition
 * [raw] (Minuten unter dem Finger). Überquert der Finger 12 Uhr über die 60 hinaus (bzw. unter
 * die 1), hängt der Wert an der Grenze, bis der Finger über 12 Uhr zurückkommt oder einmal ganz
 * herum wieder beim Knopf ist – egal wie weit er dazwischen wandert. Weitere Runden zählen nicht.
 */
class DialDrag(value: Int, raw: Int) {

    private var lastRaw = clampMinutes(raw)

    // +1: über 60 hinaus, −1: unter 1 hinaus, 0: der Wert folgt dem Finger.
    private var side = when (DialScale.follow(value, lastRaw)) {
        lastRaw -> 0
        DURATION_MAX_MINUTES -> 1
        else -> -1
    }

    /** Neue Fingerposition [raw] → Wert. */
    fun move(raw: Int): Int {
        val next = clampMinutes(raw)
        val delta = next - lastRaw
        // Ein Sprung um mehr als eine halbe Runde heißt: über 12 Uhr (59 → 2 im Uhrzeigersinn).
        // Ist der Finger einmal ganz herum wieder am hängenden Knopf, folgt der Wert ihm.
        side = when {
            delta < -HALF_ROUND -> (side + 1).coerceAtMost(1)
            delta > HALF_ROUND -> (side - 1).coerceAtLeast(-1)
            side == 1 && next >= DURATION_MAX_MINUTES - RELEASE_WINDOW -> 0
            side == -1 && next <= DURATION_MIN_MINUTES + RELEASE_WINDOW -> 0
            else -> side
        }
        lastRaw = next
        return when (side) {
            1 -> DURATION_MAX_MINUTES
            -1 -> DURATION_MIN_MINUTES
            else -> next
        }
    }
}

/**
 * Raste-Stärke für einen Wechsel [previous] → [next]: deutlich, wenn dabei eine 5er-Marke erreicht
 * oder überfahren wird (schnelles Ziehen springt z. B. 13 → 16), sonst leicht.
 */
fun crossesMajor(previous: Int, next: Int): Boolean {
    val passed = if (next >= previous) previous + 1..next else next until previous
    return passed.any(DialScale::isMajor)
}

/**
 * Umrechnung wischbare Skala ↔ Minuten (Variante F): Versatz 0 steht auf 1 min, je [stepPx]
 * eine Minute weiter; nach der 60 folgt als letzter Strich ∞ (offene Sitzung).
 */
object RulerScale {

    // Strich-Index von ∞: direkt hinter der 60.
    private const val FREE_INDEX = DURATION_MAX_MINUTES - DURATION_MIN_MINUTES + 1

    /** Scroll-Versatz → 1..60 (gerundet auf den nächsten Strich; auf ∞ steht 60). */
    fun minutesAt(offsetPx: Float, stepPx: Float): Int {
        if (stepPx <= 0f) return DURATION_MIN_MINUTES
        return clampMinutes((offsetPx / stepPx).roundToInt() + DURATION_MIN_MINUTES)
    }

    /** Minuten → Scroll-Versatz, auf dem der Strich unter der Marke steht. */
    fun offsetOf(minutes: Int, stepPx: Float): Float = (clampMinutes(minutes) - DURATION_MIN_MINUTES) * stepPx

    /** Steht der Versatz (gerundet) auf dem ∞-Strich? */
    fun isFreeAt(offsetPx: Float, stepPx: Float): Boolean = stepPx > 0f && (offsetPx / stepPx).roundToInt() >= FREE_INDEX

    /** Versatz des ∞-Strichs – zugleich das Ende der Skala. */
    fun offsetOfFree(stepPx: Float): Float = FREE_INDEX * stepPx
}

private const val HALF_ROUND = DURATION_MAX_MINUTES / 2

// Minuten vor der hängenden Grenze, ab denen der Finger den Knopf wieder mitnimmt.
private const val RELEASE_WINDOW = 3
private const val MAJOR_STEP = 5

/** Klemmt auf [DURATION_MIN_MINUTES]..[DURATION_MAX_MINUTES]. */
internal fun clampMinutes(minutes: Int): Int = minutes.coerceIn(DURATION_MIN_MINUTES, DURATION_MAX_MINUTES)
