package app.atemkraft.ui.meditation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/** Reine Umrechnung Zeitring/Skala ↔ Minuten. */
class DurationScaleTest {

    private val eps = 0.001f

    @Test
    fun `minutesAt an den Eckwinkeln`() {
        assertEquals(60, DialScale.minutesAt(0f))
        assertEquals(1, DialScale.minutesAt(6f))
        assertEquals(59, DialScale.minutesAt(354f))
        assertEquals(60, DialScale.minutesAt(359f))
        assertEquals(60, DialScale.minutesAt(360f))
        assertEquals(15, DialScale.minutesAt(90f))
        assertEquals(30, DialScale.minutesAt(180f))
        assertEquals(45, DialScale.minutesAt(270f))
    }

    @Test
    fun `minutesAt rundet auf die naechste Minute`() {
        assertEquals(1, DialScale.minutesAt(8.9f))
        assertEquals(2, DialScale.minutesAt(9.1f))
        assertEquals(12, DialScale.minutesAt(71f))
        assertEquals(12, DialScale.minutesAt(73.5f))
        assertEquals(60, DialScale.minutesAt(2.9f)) // knapp hinter 12 Uhr zählt noch als volle Runde
        assertEquals(1, DialScale.minutesAt(3.1f))
    }

    @Test
    fun `minutesAt normalisiert Winkel ausserhalb einer Runde`() {
        assertEquals(1, DialScale.minutesAt(366f))
        assertEquals(59, DialScale.minutesAt(-6f))
        assertEquals(60, DialScale.minutesAt(720f))
    }

    @Test
    fun `angleOf Minuten und zurueck`() {
        assertEquals(6f, DialScale.angleOf(1), eps)
        assertEquals(72f, DialScale.angleOf(12), eps)
        assertEquals(354f, DialScale.angleOf(59), eps)
        assertEquals(360f, DialScale.angleOf(60), eps) // voller Bogen
        assertEquals(6f, DialScale.angleOf(0), eps) // geklemmt
        assertEquals(360f, DialScale.angleOf(90), eps) // geklemmt
        for (m in 1..60) assertEquals(m, DialScale.minutesAt(DialScale.angleOf(m)))
    }

    @Test
    fun `angleOf Punkt in allen vier Quadranten`() {
        val cx = 100f
        val cy = 100f
        assertEquals(0f, DialScale.angleOf(100f, 0f, cx, cy), eps) // 12 Uhr
        assertEquals(90f, DialScale.angleOf(200f, 100f, cx, cy), eps) // 3 Uhr
        assertEquals(180f, DialScale.angleOf(100f, 200f, cx, cy), eps) // 6 Uhr
        assertEquals(270f, DialScale.angleOf(0f, 100f, cx, cy), eps) // 9 Uhr
        assertEquals(45f, DialScale.angleOf(150f, 50f, cx, cy), eps) // oben rechts
        assertEquals(135f, DialScale.angleOf(150f, 150f, cx, cy), eps) // unten rechts
        assertEquals(225f, DialScale.angleOf(50f, 150f, cx, cy), eps) // unten links
        assertEquals(315f, DialScale.angleOf(50f, 50f, cx, cy), eps) // oben links
    }

    @Test
    fun `angleOf Punkt liefert nie 360`() {
        // Knapp links von 12 Uhr: -0,0000006° + 360 ergibt in Float genau 360.
        assertEquals(0f, DialScale.angleOf(-1e-6f, -100f, 0f, 0f), eps)
        val a = DialScale.angleOf(99.9999f, 0f, 100f, 100f)
        assertTrue(a >= 0f && a < 360f)
    }

    @Test
    fun `DialDrag rastet ueber 60 hinaus in ∞ ein und kommt ueber 12 Uhr zurueck auf 60`() {
        val drag = DialDrag(value = 58, at = 58f)
        assertEquals(59, drag.move(59f))
        assertEquals(60, drag.move(60f))
        // Weiter im Uhrzeigersinn über 12 Uhr und einmal halb herum: ∞, kein Sprung auf 30/31.
        for (at in listOf(1f, 2f, 10f, 29f, 30f, 31f, 45f, 56f, 58f)) assertNull("at $at", drag.move(at))
        // Noch einmal über 12 Uhr im Uhrzeigersinn (58 → 2): bleibt ∞.
        assertNull(drag.move(2f))
        // Zurück gegen den Uhrzeigersinn über 12 Uhr: wieder 60, dann folgt der Wert dem Finger.
        assertEquals(60, drag.move(59.8f))
        assertEquals(50, drag.move(50f))
    }

    @Test
    fun `DialDrag ∞ beginnt erst hinter 12 Uhr`() {
        val drag = DialDrag(value = 60, at = 59.9f)
        assertEquals(60, drag.move(0.3f)) // noch vor der halben Minute hinter 12 Uhr
        assertNull(drag.move(0.6f))
        assertEquals(60, drag.move(0.2f)) // zurück vor 12 Uhr + ½ min: wieder 60
    }

    @Test
    fun `DialDrag haelt den Wert an 1 gegen den Uhrzeigersinn, ohne ∞`() {
        val drag = DialDrag(value = 2, at = 2f)
        assertEquals(1, drag.move(1f))
        for (at in listOf(60f, 59f, 31f, 30f, 29f, 10f, 5f)) assertEquals("at $at", 1, drag.move(at))
        // Einmal ganz herum wieder am Knopf: der Wert folgt dem Finger.
        assertEquals(4, drag.move(4f))
        assertEquals(6, drag.move(6.4f))
    }

    @Test
    fun `DialDrag folgt schnellen Spruengen und erkennt den Uebertritt`() {
        val drag = DialDrag(value = 50, at = 50f)
        assertNull(drag.move(10f)) // 50 → 10 in einem Schritt: über 12 Uhr → ∞
        assertNull(drag.move(25f))
        assertEquals(58, drag.move(58f)) // 25 → 58: zurück über 12 Uhr
        assertEquals(40, drag.move(40f))
    }

    @Test
    fun `DialDrag am Knopf gegriffen jenseits von 12 Uhr springt nicht`() {
        // Wert 60, gegriffen rechts von 12 Uhr (unter dem Finger liegt 1): hängt an 60, kein ∞.
        val right = DialDrag(value = 60, at = 1f)
        assertEquals(60, right.move(2f))
        // Zurück links von 12 Uhr: folgt wieder (die 60-Marke hält bis 1,3 min Abstand).
        assertEquals(60, right.move(59f))
        assertEquals(58, right.move(58f))
        // Wert 1, gegriffen links von 12 Uhr (unter dem Finger liegt 60).
        val left = DialDrag(value = 1, at = 60f)
        assertEquals(1, left.move(59f))
        assertEquals(2, left.move(2f))
        // Normal gegriffen: folgt sofort.
        assertEquals(13, DialDrag(value = 12, at = 12f).move(13f))
    }

    @Test
    fun `snapMinutes faengt 5er-Marken frueher als die Rundung`() {
        assertEquals(5, snapMinutes(4.3f, previous = 3)) // gerundet wäre es 4
        assertEquals(5, snapMinutes(5.7f, previous = 7)) // gerundet wäre es 6
        assertEquals(60, snapMinutes(59.3f, previous = 58))
        assertEquals(4, snapMinutes(4.1f, previous = 3)) // außerhalb des Fangs: Rundung
        assertEquals(6, snapMinutes(6.1f, previous = 7))
        assertEquals(12, snapMinutes(12.4f, previous = 11))
    }

    @Test
    fun `snapMinutes haelt an der Marke bis zum Loesen (Hysterese)`() {
        assertEquals(5, snapMinutes(6.2f, previous = 5)) // gerundet 6, Fang allein ließe bei 5,8 los
        assertEquals(5, snapMinutes(3.8f, previous = 5))
        assertEquals(6, snapMinutes(6.4f, previous = 5))
        assertEquals(4, snapMinutes(3.6f, previous = 5))
        assertEquals(60, snapMinutes(58.8f, previous = 60))
        assertEquals(59, snapMinutes(58.6f, previous = 60))
    }

    @Test
    fun `snapMinutes laesst alle Zwischenwerte erreichbar`() {
        // Langsam aufwärts und abwärts in 0,1-min-Schritten: jede Minute kommt vor.
        for (path in listOf((10..600).map { it / 10f }, (600 downTo 10).map { it / 10f })) {
            var value = clampMinutes(path.first().roundToInt())
            val seen = mutableSetOf(value)
            for (exact in path) {
                value = snapMinutes(exact, value)
                seen += value
            }
            assertEquals((1..60).toSet(), seen)
        }
    }

    @Test
    fun `DialDrag rastet beim Ziehen an der 5 ein und meldet die deutliche Raste`() {
        val state = DurationPickerState(initialMinutes = 3, initialFree = false)
        val ticks = mutableListOf<Pair<Int, Boolean>>()
        val drag = DialDrag(state.minutes, at = 3f)
        for (at in listOf(3.6f, 4.3f, 4.8f, 5.5f, 6.2f, 6.4f)) {
            state.selectWithTick(drag.move(at)!!) { major -> ticks += state.minutes to major }
        }
        // 4 leicht, Fang an 5 deutlich, Halten an 5 ohne Raste, Lösen auf 6 leicht.
        assertEquals(listOf(4 to false, 5 to true, 6 to false), ticks)
    }

    @Test
    fun `∞ und zurueck melden je genau eine deutliche Raste`() {
        val state = DurationPickerState(initialMinutes = 57, initialFree = false)
        val ticks = mutableListOf<String>()
        val onTick = { major: Boolean -> ticks += (if (state.free) "∞" else "${state.minutes}") + (if (major) "!" else "") }
        val drag = DialDrag(state.minutes, at = 57f)
        for (at in listOf(58f, 59.3f, 59.9f, 0.6f, 1f, 5f, 4f, 0.9f, 59.6f, 59.2f, 58.6f)) {
            val next = drag.move(at)
            if (next == null) state.selectFreeWithTick(onTick) else state.selectWithTick(next, onTick)
        }
        // 58 leicht, Fang an 60 deutlich, ∞ deutlich (einmal), zurück auf 60 deutlich, Lösen auf 59.
        assertEquals(listOf("58", "60!", "∞!", "60!", "59"), ticks)
    }

    @Test
    fun `crossesMajor meldet erreichte und ueberfahrene 5er-Marken`() {
        assertTrue(crossesMajor(14, 15))
        assertTrue(crossesMajor(13, 16)) // 15 überfahren
        assertTrue(crossesMajor(16, 15)) // abwärts auf 15
        assertTrue(crossesMajor(17, 14)) // abwärts über 15
        assertFalse(crossesMajor(15, 16)) // von der Marke weg
        assertFalse(crossesMajor(15, 14))
        assertFalse(crossesMajor(11, 13))
        assertFalse(crossesMajor(12, 12))
        assertTrue(crossesMajor(59, 60))
    }

    @Test
    fun `follow springt nicht ueber 12 Uhr`() {
        // Im Uhrzeigersinn über 12 Uhr hinaus: bleibt bei 60.
        assertEquals(60, DialScale.follow(58, 2))
        assertEquals(60, DialScale.follow(60, 1))
        // Gegen den Uhrzeigersinn über 12 Uhr: bleibt bei 1.
        assertEquals(1, DialScale.follow(3, 57))
        assertEquals(1, DialScale.follow(1, 60))
        // Normales Drehen folgt dem Finger.
        assertEquals(13, DialScale.follow(12, 13))
        assertEquals(11, DialScale.follow(12, 11))
        assertEquals(1, DialScale.follow(31, 1)) // genau eine halbe Runde: noch erlaubt
        // Ohne Gedächtnis gilt 31 wieder als erreichbar; beim Ziehen hält DialDrag die 60.
        assertEquals(31, DialScale.follow(60, 31))
    }

    @Test
    fun `follow klemmt Eingaben`() {
        assertEquals(59, DialScale.follow(90, 59)) // alte 90 gilt als 60
        assertEquals(60, DialScale.follow(90, 75))
        assertEquals(1, DialScale.follow(0, 1))
        assertEquals(1, DialScale.follow(0, -4))
    }

    @Test
    fun `isMajor markiert 5er-Schritte`() {
        assertTrue(DialScale.isMajor(5))
        assertTrue(DialScale.isMajor(60))
        assertFalse(DialScale.isMajor(1))
        assertFalse(DialScale.isMajor(12))
    }

    @Test
    fun `RulerScale Rundlauf und Klemmen`() {
        val step = 24f
        for (m in 1..60) {
            assertEquals(m, RulerScale.minutesAt(RulerScale.offsetOf(m, step), step))
        }
        assertEquals(0f, RulerScale.offsetOf(1, step), eps)
        assertEquals(59 * step, RulerScale.offsetOf(60, step), eps)
        assertEquals(0f, RulerScale.offsetOf(0, step), eps)
        assertEquals(59 * step, RulerScale.offsetOf(90, step), eps)
        assertEquals(1, RulerScale.minutesAt(-100f, step))
        assertEquals(60, RulerScale.minutesAt(10_000f, step))
        assertEquals(2, RulerScale.minutesAt(step * 0.6f, step)) // rundet zum nächsten Strich
        assertEquals(1, RulerScale.minutesAt(step * 0.4f, step))
        assertEquals(1, RulerScale.minutesAt(50f, 0f)) // ungültige Schrittweite
    }

    @Test
    fun `exactMinutesAt liefert ungerundete Minuten`() {
        assertEquals(0f, DialScale.exactMinutesAt(0f), eps)
        assertEquals(12.5f, DialScale.exactMinutesAt(75f), eps)
        assertEquals(59.5f, DialScale.exactMinutesAt(357f), eps)
        assertEquals(1f, DialScale.exactMinutesAt(366f), eps)
        assertEquals(59f, DialScale.exactMinutesAt(-6f), eps)
        val step = 24f
        assertEquals(1f, RulerScale.exactMinutesAt(0f, step), eps)
        assertEquals(5.5f, RulerScale.exactMinutesAt(4.5f * step, step), eps)
        assertEquals(61f, RulerScale.exactMinutesAt(10_000f, step), eps) // ∞-Strich
        assertEquals(1f, RulerScale.exactMinutesAt(-100f, step), eps)
        assertEquals(5, snapMinutes(RulerScale.exactMinutesAt(3.3f * step, step), previous = 4)) // 4,3 → Fang
    }

    @Test
    fun `RulerScale fuehrt den ∞-Strich hinter der 60`() {
        val step = 24f
        assertEquals(60 * step, RulerScale.offsetOfFree(step), eps)
        assertTrue(RulerScale.isFreeAt(RulerScale.offsetOfFree(step), step))
        assertTrue(RulerScale.isFreeAt(59.6f * step, step)) // rundet auf ∞
        assertFalse(RulerScale.isFreeAt(59.4f * step, step)) // rundet auf 60
        assertFalse(RulerScale.isFreeAt(RulerScale.offsetOf(60, step), step))
        assertEquals(60, RulerScale.minutesAt(RulerScale.offsetOfFree(step), step))
        assertFalse(RulerScale.isFreeAt(10_000f, 0f))
    }
}
