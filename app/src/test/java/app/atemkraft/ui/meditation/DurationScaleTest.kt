package app.atemkraft.ui.meditation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/** Reine Umrechnung Zeitring/Skala ↔ Minuten, Ziehen am Ring, Fang an den 5er-Marken und Rasten. */
class DurationScaleTest {

    private val eps = 0.001f

    @Test
    fun `Winkel und Minuten - Rundlauf 1 bis 60, Rundung, Normalisierung, 12 Uhr ist 60`() {
        for (m in 1..60) assertEquals(m, DialScale.minutesAt(DialScale.angleOf(m)))
        assertEquals(60, DialScale.minutesAt(0f))
        assertEquals(360f, DialScale.angleOf(60), eps) // voller Bogen
        assertEquals(6f, DialScale.angleOf(0), eps) // geklemmt
        // Knapp hinter 12 Uhr zählt noch als volle Runde.
        assertEquals(60, DialScale.minutesAt(2.9f))
        assertEquals(1, DialScale.minutesAt(3.1f))
        // Außerhalb einer Runde.
        assertEquals(1, DialScale.minutesAt(366f))
        assertEquals(59, DialScale.minutesAt(-6f))
        // Ungerundet für den Fang.
        assertEquals(12.5f, DialScale.exactMinutesAt(75f), eps)
        assertEquals(59f, DialScale.exactMinutesAt(-6f), eps)
    }

    @Test
    fun `Punkt zu Winkel - vier Himmelsrichtungen, nie 360`() {
        val cx = 100f
        val cy = 100f
        assertEquals(0f, DialScale.angleOf(100f, 0f, cx, cy), eps) // 12 Uhr
        assertEquals(90f, DialScale.angleOf(200f, 100f, cx, cy), eps) // 3 Uhr
        assertEquals(180f, DialScale.angleOf(100f, 200f, cx, cy), eps) // 6 Uhr
        assertEquals(270f, DialScale.angleOf(0f, 100f, cx, cy), eps) // 9 Uhr
        // Knapp links von 12 Uhr: -0,0000006° + 360 ergibt in Float genau 360.
        assertEquals(0f, DialScale.angleOf(-1e-6f, -100f, 0f, 0f), eps)
    }

    @Test
    fun `DialDrag im Uhrzeigersinn ueber 12 Uhr wird ∞ ab einer halben Minute, zurueck 60, auch bei schnellen Spruengen`() {
        val drag = DialDrag(value = 58, at = 58f)
        assertEquals(60, drag.move(60f))
        assertEquals(60, drag.move(0.3f)) // noch vor der halben Minute hinter 12 Uhr
        // Weiter im Uhrzeigersinn und einmal halb herum: ∞, kein Sprung auf 30/31.
        for (at in listOf(0.6f, 10f, 30f, 31f, 58f, 2f)) assertNull("at $at", drag.move(at))
        // Zurück gegen den Uhrzeigersinn über 12 Uhr: wieder 60, dann folgt der Wert dem Finger.
        assertEquals(60, drag.move(59.8f))
        assertEquals(50, drag.move(50f))

        // Schnelle Sprünge: 50 → 10 in einem Schritt ist der Übertritt, 25 → 58 der Weg zurück.
        val fast = DialDrag(value = 50, at = 50f)
        assertNull(fast.move(10f))
        assertEquals(58, fast.move(58f))
    }

    @Test
    fun `DialDrag haelt an 1 gegen den Uhrzeigersinn und springt am Knopf jenseits von 12 Uhr nicht`() {
        val drag = DialDrag(value = 2, at = 2f)
        assertEquals(1, drag.move(1f))
        for (at in listOf(60f, 30f, 5f)) assertEquals("at $at", 1, drag.move(at))
        // Einmal ganz herum wieder am Knopf: der Wert folgt dem Finger.
        assertEquals(4, drag.move(4f))

        // Wert 60, gegriffen rechts von 12 Uhr (unter dem Finger liegt 1): hängt an 60, kein ∞.
        val right = DialDrag(value = 60, at = 1f)
        assertEquals(60, right.move(2f))
        assertEquals(58, right.move(58f))
        // Wert 1, gegriffen links von 12 Uhr (unter dem Finger liegt 60).
        val left = DialDrag(value = 1, at = 60f)
        assertEquals(1, left.move(59f))
        assertEquals(2, left.move(2f))
    }

    @Test
    fun `snapMinutes faengt an 5er-Marken und haelt bis zum Loesen`() {
        assertEquals(5, snapMinutes(4.3f, previous = 3)) // gerundet wäre es 4
        assertEquals(5, snapMinutes(5.7f, previous = 7)) // gerundet wäre es 6
        assertEquals(4, snapMinutes(4.1f, previous = 3)) // außerhalb des Fangs: Rundung
        // Hysterese: an der Marke bleibt es länger.
        assertEquals(5, snapMinutes(6.2f, previous = 5))
        assertEquals(6, snapMinutes(6.4f, previous = 5))
        assertEquals(60, snapMinutes(58.8f, previous = 60))
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

    /** Ende der Kette Ziehen → Fang → Zustand: je Marke genau eine deutliche Raste. */
    @Test
    fun `Ziehen meldet je Marke genau eine deutliche Raste - 5er-Fang, 60, ∞ und zurueck`() {
        val five = DurationPickerState(initialMinutes = 3, initialFree = false)
        val fiveTicks = mutableListOf<Pair<Int, Boolean>>()
        val fiveDrag = DialDrag(five.minutes, at = 3f)
        for (at in listOf(3.6f, 4.3f, 4.8f, 5.5f, 6.2f, 6.4f)) {
            five.selectWithTick(fiveDrag.move(at)!!) { major -> fiveTicks += five.minutes to major }
        }
        // 4 leicht, Fang an 5 deutlich, Halten an 5 ohne Raste, Lösen auf 6 leicht.
        assertEquals(listOf(4 to false, 5 to true, 6 to false), fiveTicks)

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

    /** Skala als Lineal (Reserve, Variante F – nicht ausgeliefert): bleibt lauffähig. */
    @Test
    fun `RulerScale - Rundlauf, Klemmen, ∞-Strich hinter der 60`() {
        val step = 24f
        for (m in 1..60) assertEquals(m, RulerScale.minutesAt(RulerScale.offsetOf(m, step), step))
        assertEquals(1, RulerScale.minutesAt(-100f, step))
        assertEquals(60, RulerScale.minutesAt(10_000f, step))
        assertEquals(5.5f, RulerScale.exactMinutesAt(4.5f * step, step), eps)
        assertEquals(60 * step, RulerScale.offsetOfFree(step), eps)
        assertTrue(RulerScale.isFreeAt(59.6f * step, step)) // rundet auf ∞
        assertFalse(RulerScale.isFreeAt(59.4f * step, step)) // rundet auf 60
    }
}
