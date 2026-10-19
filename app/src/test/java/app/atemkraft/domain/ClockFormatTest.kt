package app.atemkraft.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** m:ss-Formatierung ([formatClock]) der Meditations-Zeiten (geteilt von Overlay, Mini-Leiste und Service). */
class ClockFormatTest {

    @Test
    fun `formatiert Minuten und Sekunden zweistellig`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:05", formatClock(5_400)) // abgerundet auf volle Sekunden
        assertEquals("1:00", formatClock(60_000))
        assertEquals("90:00", formatClock(90 * 60_000L))
    }

    @Test
    fun `negative Werte werden als Null-Zeit angezeigt`() {
        assertEquals("0:00", formatClock(-1_000))
    }
}
