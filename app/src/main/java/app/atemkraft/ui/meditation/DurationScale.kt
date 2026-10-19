package app.atemkraft.ui.meditation

import app.atemkraft.domain.MEDITATION_MAX_MINUTES
import app.atemkraft.domain.MEDITATION_MIN_MINUTES
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Umrechnung Zeitring ↔ Minuten (reine Rechnung, ohne Compose). Eine Runde = 60 min wie ein
 * Küchenwecker: 0° = 12 Uhr, im Uhrzeigersinn, 6° je Minute.
 */
object DialScale {

    private const val DEG_PER_MINUTE = 360f / MEDITATION_MAX_MINUTES

    /** Winkel (0..360, gerundet auf die nächste Minute) → 1..60; 12 Uhr (0° bzw. 360°) ist 60. */
    fun minutesAt(angleDeg: Float): Int {
        val norm = ((angleDeg % 360f) + 360f) % 360f
        val m = (norm / DEG_PER_MINUTE).roundToInt()
        return if (m == 0) MEDITATION_MAX_MINUTES else clampMinutes(m)
    }

    /**
     * Winkel → ungerundete Minuten 0..<60 (12 Uhr = 0, 6° je Minute); Grundlage für das
     * Einrasten an 5er-Marken ([snapMinutes]) beim Ziehen.
     */
    fun exactMinutesAt(angleDeg: Float): Float = (((angleDeg % 360f) + 360f) % 360f) / DEG_PER_MINUTE

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
        return if (prev <= HALF_ROUND) MEDITATION_MIN_MINUTES else MEDITATION_MAX_MINUTES
    }

    /** 5er-Marke (längerer Strich, Zahl, deutlichere Haptik-Raste). */
    fun isMajor(minutes: Int): Boolean = minutes % MAJOR_STEP == 0
}

/**
 * Ziehen am Zeitring ohne Sprung über 12 Uhr. Startet mit dem Wert [value] und der Fingerposition
 * [at] (ungerundete Minuten unter dem Finger, [DialScale.exactMinutesAt]). Dazwischen folgt der
 * Wert dem Finger und rastet an den 5er-Marken magnetisch ein ([snapMinutes]).
 *
 * Über 12 Uhr hinaus:
 * - im Uhrzeigersinn über die 60 hinaus → offene Sitzung (∞, [move] liefert `null`), bis der
 *   Finger gegen den Uhrzeigersinn über 12 Uhr zurückkommt (dann wieder 60 bzw. der Wert unter
 *   dem Finger); weitere Runden ändern nichts;
 * - gegen den Uhrzeigersinn unter die 1 → der Wert hängt an der 1 (kein ∞), bis der Finger über
 *   12 Uhr zurückkommt oder einmal ganz herum wieder beim Knopf ist.
 *
 * Am Knopf bei 60 knapp rechts von 12 Uhr gegriffen ist noch nichts überquert: der Wert hängt an
 * der 60 (kein ∞), bis der Finger zurück links von 12 Uhr oder weiter einmal über 12 Uhr ist.
 */
class DialDrag(value: Int, at: Float) {

    private enum class Zone { FOLLOW, FREE, HELD_MAX, HELD_MIN }

    private var lastRaw = rawMinutes(unwrap(at))
    private var last = clampMinutes(value)
    private var zone = when (DialScale.follow(value, lastRaw)) {
        lastRaw -> Zone.FOLLOW
        MEDITATION_MAX_MINUTES -> Zone.HELD_MAX
        else -> Zone.HELD_MIN
    }

    /** Neue Fingerposition [at] (ungerundete Minuten) → Wert 1..60, `null` = offene Sitzung (∞). */
    fun move(at: Float): Int? {
        val exact = unwrap(at)
        val next = rawMinutes(exact)
        val delta = next - lastRaw
        // Ein Sprung um mehr als eine halbe Runde heißt: über 12 Uhr (59 → 2 im Uhrzeigersinn).
        zone = when {
            delta < -HALF_ROUND -> if (zone == Zone.HELD_MIN) Zone.FOLLOW else Zone.FREE

            delta > HALF_ROUND -> if (zone == Zone.FREE || zone == Zone.HELD_MAX) Zone.FOLLOW else Zone.HELD_MIN

            // Einmal ganz herum wieder am hängenden Knopf: der Wert folgt dem Finger.
            zone == Zone.HELD_MAX && next >= MEDITATION_MAX_MINUTES - RELEASE_WINDOW -> Zone.FOLLOW

            zone == Zone.HELD_MIN && next <= MEDITATION_MIN_MINUTES + RELEASE_WINDOW -> Zone.FOLLOW

            else -> zone
        }
        lastRaw = next
        last = when (zone) {
            Zone.FOLLOW -> snapMinutes(exact, last)
            Zone.FREE, Zone.HELD_MAX -> MEDITATION_MAX_MINUTES
            Zone.HELD_MIN -> MEDITATION_MIN_MINUTES
        }
        return if (zone == Zone.FREE) null else last
    }

    private companion object {
        // Ungerundete Minuten auf 0,5..<60,5: knapp rechts von 12 Uhr (unter 0,5) zählt als 60,x,
        // damit Rundung und Fang an der 60 nicht über 12 Uhr springen.
        fun unwrap(at: Float): Float {
            val norm = ((at % MEDITATION_MAX_MINUTES) + MEDITATION_MAX_MINUTES) % MEDITATION_MAX_MINUTES
            return if (norm < HALF_MINUTE) norm + MEDITATION_MAX_MINUTES else norm
        }

        fun rawMinutes(exact: Float): Int = clampMinutes(exact.roundToInt())
    }
}

/**
 * Magnetisches Einrasten an den 5er-Marken (Zeitring und Skala gleich): [exact] ist die
 * ungerundete Fingerposition in Minuten, [previous] der bisherige Wert.
 * - Steht der Wert auf einer 5er-Marke, hält er dort, bis der Finger [SNAP_RELEASE] Minuten
 *   entfernt ist (Hysterese – der Knopf „hängt“ an der Marke).
 * - Sonst fängt eine 5er-Marke ab [SNAP_CAPTURE] Minuten Abstand, früher als die Rundung.
 * - Sonst die nächste Minute. Zwischenwerte (4, 6, 9, 11 …) bleiben so erreichbar.
 */
fun snapMinutes(exact: Float, previous: Int): Int {
    if (DialScale.isMajor(previous) && abs(exact - previous) < SNAP_RELEASE) return clampMinutes(previous)
    val major = (exact / MAJOR_STEP).roundToInt() * MAJOR_STEP
    if (major >= MAJOR_STEP && abs(exact - major) <= SNAP_CAPTURE) return clampMinutes(major)
    return clampMinutes(exact.roundToInt())
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
 *
 * Vorrat, Stand v1.6.5: Variante F ist als Reserve zum Zeitring vorbereitet, aber nicht
 * ausgeliefert – nur `DurationScaleTest` nutzt dieses Objekt. Kein toter Code; entfernen nur nach
 * Entscheidung gegen Variante F.
 */
object RulerScale {

    // Strich-Index von ∞: direkt hinter der 60.
    private const val FREE_INDEX = MEDITATION_MAX_MINUTES - MEDITATION_MIN_MINUTES + 1

    /** Scroll-Versatz → 1..60 (gerundet auf den nächsten Strich; auf ∞ steht 60). */
    fun minutesAt(offsetPx: Float, stepPx: Float): Int {
        if (stepPx <= 0f) return MEDITATION_MIN_MINUTES
        return clampMinutes((offsetPx / stepPx).roundToInt() + MEDITATION_MIN_MINUTES)
    }

    /**
     * Scroll-Versatz → ungerundete Minuten (1 = erster Strich, ∞-Strich = 61), für das Einrasten
     * an 5er-Marken über [snapMinutes] wie am Zeitring.
     */
    fun exactMinutesAt(offsetPx: Float, stepPx: Float): Float {
        if (stepPx <= 0f) return MEDITATION_MIN_MINUTES.toFloat()
        return (offsetPx / stepPx + MEDITATION_MIN_MINUTES).coerceIn(MEDITATION_MIN_MINUTES.toFloat(), (FREE_INDEX + MEDITATION_MIN_MINUTES).toFloat())
    }

    /** Minuten → Scroll-Versatz, auf dem der Strich unter der Marke steht. */
    fun offsetOf(minutes: Int, stepPx: Float): Float = (clampMinutes(minutes) - MEDITATION_MIN_MINUTES) * stepPx

    /** Steht der Versatz (gerundet) auf dem ∞-Strich? */
    fun isFreeAt(offsetPx: Float, stepPx: Float): Boolean = stepPx > 0f && (offsetPx / stepPx).roundToInt() >= FREE_INDEX

    /** Versatz des ∞-Strichs – zugleich das Ende der Skala. */
    fun offsetOfFree(stepPx: Float): Float = FREE_INDEX * stepPx
}

private const val HALF_ROUND = MEDITATION_MAX_MINUTES / 2

// Minuten vor der hängenden Grenze, ab denen der Finger den Knopf wieder mitnimmt.
private const val RELEASE_WINDOW = 3
private const val MAJOR_STEP = 5
private const val HALF_MINUTE = 0.5f

// Fang an 5er-Marken (Minuten Abstand): fängt ab SNAP_CAPTURE, lässt erst ab SNAP_RELEASE los.
private const val SNAP_CAPTURE = 0.8f
private const val SNAP_RELEASE = 1.3f

/** Klemmt auf [MEDITATION_MIN_MINUTES]..[MEDITATION_MAX_MINUTES]. */
internal fun clampMinutes(minutes: Int): Int = minutes.coerceIn(MEDITATION_MIN_MINUTES, MEDITATION_MAX_MINUTES)
