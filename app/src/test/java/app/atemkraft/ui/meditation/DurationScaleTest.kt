package app.atemkraft.ui.meditation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `DialDrag haelt den Wert an 60, bis der Finger ueber 12 Uhr zurueckkommt`() {
        val drag = DialDrag(value = 58, raw = 58)
        assertEquals(59, drag.move(59))
        assertEquals(60, drag.move(60))
        // Weiter im Uhrzeigersinn über 12 Uhr und einmal halb herum: kein Sprung auf 30/31.
        for (raw in listOf(1, 2, 10, 29, 30, 31, 45, 56)) assertEquals("raw $raw", 60, drag.move(raw))
        // Noch einmal über 12 Uhr im Uhrzeigersinn (56 → 2): zählt nicht doppelt.
        assertEquals(60, drag.move(2))
        // Zurück gegen den Uhrzeigersinn über 12 Uhr: der Wert folgt wieder dem Finger.
        assertEquals(59, drag.move(59))
        assertEquals(50, drag.move(50))
    }

    @Test
    fun `DialDrag haelt den Wert an 1 gegen den Uhrzeigersinn`() {
        val drag = DialDrag(value = 2, raw = 2)
        assertEquals(1, drag.move(1))
        for (raw in listOf(60, 59, 31, 30, 29, 10, 5)) assertEquals("raw $raw", 1, drag.move(raw))
        // Einmal ganz herum wieder am Knopf: der Wert folgt dem Finger.
        assertEquals(4, drag.move(4))
        assertEquals(6, drag.move(6))
    }

    @Test
    fun `DialDrag folgt schnellen Spruengen und erkennt den Uebertritt`() {
        val drag = DialDrag(value = 50, raw = 50)
        assertEquals(60, drag.move(10)) // 50 → 10 in einem Schritt: über 12 Uhr
        assertEquals(60, drag.move(25))
        assertEquals(58, drag.move(58)) // 25 → 58: zurück über 12 Uhr
        assertEquals(40, drag.move(40))
    }

    @Test
    fun `DialDrag am Knopf gegriffen jenseits von 12 Uhr springt nicht`() {
        // Wert 60, gegriffen rechts von 12 Uhr (unter dem Finger liegt 1).
        val right = DialDrag(value = 60, raw = 1)
        assertEquals(60, right.move(2))
        assertEquals(59, right.move(59))
        // Wert 1, gegriffen links von 12 Uhr (unter dem Finger liegt 60).
        val left = DialDrag(value = 1, raw = 60)
        assertEquals(1, left.move(59))
        assertEquals(2, left.move(2))
        // Normal gegriffen: folgt sofort.
        assertEquals(13, DialDrag(value = 12, raw = 12).move(13))
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
