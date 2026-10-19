package app.atemkraft.ui.meditation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** Maße des Zeitrings (px bei Dichte 1, also dp): Knopf greifen, Band und Mitte, 10er-Zahlen. */
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
    fun `Knopf bei 60 ist auch rechts von 12 Uhr greifbar, ohne dass der Wert springt`() {
        for (side in listOf(200f, 300f, 360f)) {
            val geo = geometry(side, IntSize(14, 10))
            val c = Offset(side / 2f, side / 2f)
            // Rechte Knopfhälfte: unter dem Finger liegt schon 1 min.
            val right = onTrack(geo, c, 4.0)
            assertEquals(1, DialScale.minutesAt(DialScale.angleOf(right.x, right.y, c.x, c.y)))
            assertTrue("side $side: Knopf greifbar", geo.isOnKnob(right, c, 60))
            assertEquals(60, DialDrag(value = 60, raw = 1).move(2))
            // Linke Knopfhälfte bei 1 min: unter dem Finger liegt 60.
            val left = onTrack(geo, c, 2.0)
            assertTrue(geo.isOnKnob(left, c, 1))
            assertEquals(1, DialDrag(value = 1, raw = 60).move(59))
            // Weit weg vom Knopf: Tippen setzt den Wert.
            assertFalse(geo.isOnKnob(onTrack(geo, c, 90.0), c, 60))
        }
    }

    @Test
    fun `Knopf hat mindestens 48 dp Greifflaeche`() {
        val geo = geometry(200f, IntSize(14, 10))
        assertTrue(geo.knobGrab >= touch / 2f)
    }

    @Test
    fun `Band beginnt erst ausserhalb der Mitte`() {
        for (side in listOf(200f, 240f, 300f, 360f)) {
            for (numeral in listOf(IntSize(14, 10), IntSize(28, 20))) {
                val geo = geometry(side, numeral)
                assertTrue("side $side, Zahl $numeral", geo.bandInner >= geo.contentRadius)
                assertTrue(geo.bandInner < geo.trackRadius)
            }
        }
    }

    @Test
    fun `kleiner Ring bei grosser Schrift zeigt nur die 10er`() {
        assertEquals(listOf(10, 20, 30, 40, 50), geometry(200f, IntSize(28, 20)).numerals)
        assertEquals((5 until 60 step 5).toList(), geometry(360f, IntSize(14, 10)).numerals)
    }

    @Test
    fun `Bogenkappe beginnt am 12-Uhr-Strich`() {
        val geo = geometry(300f, IntSize(14, 10))
        // Kappe = halbe Spurbreite als Winkel; kleiner als eine Minute (6°), damit 1 min sichtbar bleibt.
        assertTrue(geo.capDeg > 0f && geo.capDeg < 6f)
    }
}
