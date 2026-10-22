package app.atemkraft.ui.meditation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Maße des Zeitrings (px bei Dichte 1, also dp): Knopf greifen, Zahlen und ∞-Knopf ohne Überlappung. */
class TimeDialGeometryTest {

    private val touch = 48f

    private fun geometry(side: Float, numeral: IntSize): DialGeometry {
        val numerals = (5 until 60 step 5).associateWith { numeral }
        return dialGeometry(side, numerals, mark = IntSize(numeral.height, numeral.height), touch = touch)
    }

    /** Punkt auf der Spur bei [angleDeg] (0° = 12 Uhr) um die Mitte [c]. */
    private fun onTrack(geo: DialGeometry, c: Offset, angleDeg: Double): Offset {
        val rad = Math.toRadians(angleDeg)
        return c + Offset(sin(rad).toFloat(), -cos(rad).toFloat()) * geo.trackRadius
    }

    @Test
    fun `Knopf mit 48 dp Greifflaeche ist bei 60 auch rechts von 12 Uhr greifbar, ohne dass der Wert springt`() {
        for (side in listOf(200f, 360f)) {
            val geo = geometry(side, IntSize(14, 10))
            val c = Offset(side / 2f, side / 2f)
            // Rechte Knopfhälfte: unter dem Finger liegt schon 1 min.
            val right = onTrack(geo, c, 4.0)
            assertEquals(1, DialScale.minutesAt(DialScale.angleOf(right.x, right.y, c.x, c.y)))
            assertTrue("side $side: Knopf greifbar", geo.isOnKnob(right, c, 60))
            assertEquals(60, DialDrag(value = 60, at = DialScale.exactMinutesAt(DialScale.angleOf(right.x, right.y, c.x, c.y))).move(2f))
            // Linke Knopfhälfte bei 1 min: unter dem Finger liegt 60.
            val left = onTrack(geo, c, 2.0)
            assertTrue(geo.isOnKnob(left, c, 1))
            assertEquals(1, DialDrag(value = 1, at = DialScale.exactMinutesAt(DialScale.angleOf(left.x, left.y, c.x, c.y))).move(59f))
            // Weit weg vom Knopf: Tippen setzt den Wert.
            assertFalse(geo.isOnKnob(onTrack(geo, c, 90.0), c, 60))
            assertTrue("side $side: Greiffläche ≥ 48 dp", geo.knobGrab >= touch / 2f)
        }
    }

    /**
     * Die Zahlen zeichnet die Canvas – die Text-Gates der Screenshots sehen sie nicht. Kleiner Ring
     * bei großer Schrift zeigt nur die 10er; ∞ umfasst das Zeichen und hält Abstand.
     */
    @Test
    fun `Zahlen und ∞-Knopf ueberlappen nicht, kleiner Ring zeigt nur 10er`() {
        assertEquals(listOf(10, 20, 30, 40, 50), geometry(200f, IntSize(28, 20)).numerals)
        assertEquals((5 until 60 step 5).toList(), geometry(360f, IntSize(14, 10)).numerals)
        for (side in listOf(200f, 360f)) {
            for (numeral in listOf(IntSize(14, 10), IntSize(28, 20))) {
                val numerals = (5 until 60 step 5).associateWith { numeral }
                // ∞ ist breiter als hoch gemessen, die Zeile aber höher als das Zeichen.
                val mark = IntSize(numeral.height, numeral.height * 3 / 2)
                val geo = dialGeometry(side, numerals, mark, touch)
                val where = "side $side, Zahl $numeral"
                val reach = geo.markRadius + geo.tickWidth / 2f
                assertTrue("$where: Zeichen im Kreis", geo.markRadius > mark.width / 2f)
                assertTrue("$where: Kreis im Bild", geo.numeralRadius + reach <= side / 2f)
                assertTrue("$where: Kreis über den Strichen", geo.numeralRadius - reach >= geo.tickOuter)
                // Nächste Zahl neben ∞: 5 bzw. 10 (nur 10er), Mitte zu Mitte als Sehne.
                val step = if (5 in geo.numerals) 30.0 else 60.0
                val chord = 2f * geo.numeralRadius * sin(Math.toRadians(step / 2)).toFloat()
                assertTrue("$where: Luft zur Nachbarzahl", chord - reach - hypot(numeral.width / 2f, numeral.height / 2f) > 0f)
                assertTrue("$where: Tippfläche ≥ 48 dp und um den Kreis", geo.markTouch >= touch && geo.markTouch >= 2f * reach)
            }
        }
    }
}
